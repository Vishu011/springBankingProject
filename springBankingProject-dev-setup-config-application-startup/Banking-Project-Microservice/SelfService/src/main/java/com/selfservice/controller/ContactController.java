package com.selfservice.controller;

import com.selfservice.dto.EmailChangeInitiateRequest;
import com.selfservice.proxy.OtpServiceClient;
import com.selfservice.proxy.UserServiceClient;
import com.selfservice.proxy.dto.OtpGenerateRequest;
import com.selfservice.proxy.dto.OtpGenerateResponse;
import com.selfservice.proxy.dto.OtpVerifyRequest;
import com.selfservice.proxy.dto.OtpVerifyResponse;
import com.selfservice.proxy.dto.UserUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.selfservice.dto.NotificationRequest;
import com.selfservice.proxy.NotificationServiceClient;

import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping({"/self-service/contact", "/contact"})
@RequiredArgsConstructor
public class ContactController {

    private final OtpServiceClient otpClient;
    private final UserServiceClient userServiceClient;
    private final NotificationServiceClient notificationClient;

    // 1) EMAIL CHANGE FLOW
    // Step 1: Initiate - send OTP to new email (public OTP generate); contextId ties to the userId
    @PostMapping("/email/initiate")
    @PreAuthorize("isAuthenticated() and #req.userId == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<OtpGenerateResponse> initiateEmailChange(@Valid @RequestBody EmailChangeInitiateRequest req) {
        OtpGenerateRequest gen = new OtpGenerateRequest();
        // use new email as userId to force toEmail behavior in otp-service
        gen.setUserId(req.getNewEmail());
        gen.setPurpose("CONTACT_VERIFICATION");
        gen.setChannels(java.util.List.of("EMAIL"));
        gen.setContextId("EMAIL_CHANGE:" + req.getUserId());
        OtpGenerateResponse resp = otpClient.generatePublic(gen);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    public static class EmailChangeVerifyRequest {
        @NotBlank
        public String userId;    // current user id
        @NotBlank
        public String newEmail;  // same email used during initiate
        @NotBlank
        public String code;      // OTP received
    }

    // Step 2: Verify - verify OTP public; on success, update user email
    @PostMapping("/email/verify")
    @PreAuthorize("isAuthenticated() and #req.userId == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<?> verifyEmailChange(@Valid @RequestBody EmailChangeVerifyRequest req) {
        OtpVerifyRequest v = new OtpVerifyRequest();
        v.setUserId(req.newEmail);
        v.setPurpose("CONTACT_VERIFICATION");
        v.setContextId("EMAIL_CHANGE:" + req.userId);
        v.setCode(req.code);
        OtpVerifyResponse verification = otpClient.verifyPublic(v);
        if (verification == null || !verification.isVerified()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(verification);
        }

        // Update user-service email
        UserUpdateRequest update = new UserUpdateRequest();
        update.setEmail(req.newEmail);
        userServiceClient.updateUser(req.userId, update);

        // Notify user to new email
        try {
            notificationClient.sendEmailNotification(new NotificationRequest(req.userId, "EMAIL",
                    "Your email address has been updated successfully.", req.newEmail));
        } catch (Exception ignore) {}

        return ResponseEntity.ok(verification);
    }

    // 2) PHONE CHANGE FLOW
    // Step 1: Initiate - send OTP to current recorded email (authenticated generate)
    public static class PhoneChangeInitiateRequest {
        @NotBlank
        public String userId;
        @NotBlank
        public String newPhone;
    }

    @PostMapping("/phone/initiate")
    @PreAuthorize("isAuthenticated() and #req.userId == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<OtpGenerateResponse> initiatePhoneChange(@Valid @RequestBody PhoneChangeInitiateRequest req) {
        OtpGenerateRequest gen = new OtpGenerateRequest();
        gen.setUserId(req.userId); // not an email, otp-service will resolve email via NotificationService
        gen.setPurpose("ACCOUNT_OPERATION");
        gen.setChannels(java.util.List.of("EMAIL"));
        gen.setContextId("PHONE_CHANGE:" + req.userId);
        OtpGenerateResponse resp = otpClient.generate(gen);

        // Notify content to user's email that a phone change was initiated (best-effort)
        try {
            notificationClient.sendEmailNotification(new NotificationRequest(req.userId, "EMAIL",
                    "A phone number change has been initiated for your profile. If this wasn't you, contact support.", null));
        } catch (Exception ignore) {}

        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    public static class PhoneChangeVerifyRequest {
        @NotBlank
        public String userId;
        @NotBlank
        public String newPhone;
        @NotBlank
        public String code;
    }

    // Step 2: Verify - verify OTP (authenticated); on success, update phone
    @PostMapping("/phone/verify")
    @PreAuthorize("isAuthenticated() and #req.userId == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<?> verifyPhoneChange(@Valid @RequestBody PhoneChangeVerifyRequest req) {
        OtpVerifyRequest v = new OtpVerifyRequest();
        v.setUserId(req.userId);
        v.setPurpose("ACCOUNT_OPERATION");
        v.setContextId("PHONE_CHANGE:" + req.userId);
        v.setCode(req.code);
        OtpVerifyResponse verification = otpClient.verify(v);
        if (verification == null || !verification.isVerified()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(verification);
        }

        // Update user-service phone number
        UserUpdateRequest update = new UserUpdateRequest();
        update.setPhoneNumber(req.newPhone);
        userServiceClient.updateUser(req.userId, update);

        // Notify user to email
        try {
            notificationClient.sendEmailNotification(new NotificationRequest(req.userId, "EMAIL",
                    "Your phone number has been updated successfully.", null));
        } catch (Exception ignore) {}

        return ResponseEntity.ok(verification);
    }
}
