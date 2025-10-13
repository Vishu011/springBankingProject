package com.notification.controller;

import com.notification.dto.NotificationRequest;
import com.notification.config.FeignClientConfiguration;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.util.StringUtils;
import com.notification.service.NotificationService;
import com.notification.dao.NotificationRepository;
import com.notification.model.Notification;
import com.notification.dto.NotificationResponse;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final JavaMailSender mailSender;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    // Will be provided in a separate class; marked required=false to avoid startup failure if not present yet
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private UserClient userClient;

    @PostMapping("/send-email")
    public ResponseEntity<?> sendEmail(@RequestBody NotificationRequest request) {
        if (request == null || !"EMAIL".equalsIgnoreCase(request.getType())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Only EMAIL notifications are supported.");
        }
        try {
            // Delegate to business service which resolves recipient via User service (with service token) or toEmail
            notificationService.sendNotification(request);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (MailException ex) {
            // Do not fail the flow hard; return 202 so callers can fallback/notify
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Email queued; mail server temporarily unavailable.");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send email: " + ex.getMessage());
        }
    }

    private String determineRecipient(NotificationRequest req) {
        if (StringUtils.hasText(req.getToEmail())) {
            return req.getToEmail();
        }
        try {
            if (userClient != null && StringUtils.hasText(req.getUserId())) {
                UserResponse u = userClient.getUserById(req.getUserId());
                if (u != null && StringUtils.hasText(u.getEmail())) {
                    return u.getEmail();
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    private void sendEmailInternal(String toEmail, String subject, String body) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

        // Ensure 'From' is configured to spring.mail.username when available
        try {
            if (mailSender instanceof JavaMailSenderImpl impl) {
                String from = impl.getUsername();
                if (StringUtils.hasText(from)) {
                    helper.setFrom(from);
                }
            }
        } catch (Exception ignore) {}

        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, false);
        mailSender.send(message);
    }

    private String safeContent(String c) {
        return StringUtils.hasText(c) ? c : "You have a new notification.";
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsByUser(
            @PathVariable("userId") String userId,
            @AuthenticationPrincipal Jwt jwt) {

        // Fetch by provided identifier (may be application userId OR an email)
        List<Notification> primary = notificationRepository.findByUserId(userId);

        // Also fetch by resolved email from User service if provided id looks like app user id
        List<Notification> byEmailFromUserSvc = Collections.emptyList();
        try {
            if (userId != null && !userId.contains("@") && userClient != null) {
                UserResponse u = userClient.getUserById(userId);
                if (u != null && StringUtils.hasText(u.getEmail())) {
                    byEmailFromUserSvc = notificationRepository.findByUserId(u.getEmail().trim());
                }
            }
        } catch (Exception ignore) {
            // best effort only
        }

        // Also fetch by email from JWT if available (covers Keycloak 'sub' mismatch)
        List<Notification> byEmailFromJwt = Collections.emptyList();
        try {
            if (jwt != null) {
                String emailClaim = jwt.getClaim("email");
                if (!StringUtils.hasText(emailClaim)) {
                    // some realms use preferred_username as email
                    emailClaim = jwt.getClaim("preferred_username");
                }
                if (StringUtils.hasText(emailClaim)) {
                    byEmailFromJwt = notificationRepository.findByUserId(emailClaim.trim());
                }
            }
        } catch (Exception ignore) {
            // best effort only
        }

        // Merge distinct by notificationId
        List<NotificationResponse> items = java.util.stream.Stream.of(primary, byEmailFromUserSvc, byEmailFromJwt)
            .flatMap(List::stream)
            .collect(java.util.stream.Collectors.toMap(
                Notification::getNotificationId,
                n -> n,
                (a, b) -> a
            ))
            .values()
            .stream()
            .sorted((a, b) -> b.getSentAt().compareTo(a.getSentAt()))
            .map(n -> new NotificationResponse(
                n.getNotificationId(),
                n.getUserId(),
                n.getType(),
                n.getContent(),
                n.getStatus(),
                n.getSentAt(),
                null
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(items);
    }

    // Minimal DTO for listing notifications in the UI (stub)
    public static class NotificationItem {
        private String notificationId;
        private String userId;
        private String type;    // e.g., TRANSACTION, SECURITY, ACCOUNT, SYSTEM
        private String content;
        private String status;  // e.g., SENT, PENDING, FAILED
        private LocalDateTime sentAt;

        public String getNotificationId() { return notificationId; }
        public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getSentAt() { return sentAt; }
        public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    }

    // Feign client to resolve user email from User service (will be generated in a separate file)
    @org.springframework.cloud.openfeign.FeignClient(name = "user-service", path = "/auth", configuration = FeignClientConfiguration.class)
    public interface UserClient {
        @org.springframework.web.bind.annotation.GetMapping("/user/{userId}")
        UserResponse getUserById(@org.springframework.web.bind.annotation.PathVariable("userId") String userId);
    }

    // Minimal DTO for email resolution
    public static class UserResponse {
        private String userId;
        private String email;
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
