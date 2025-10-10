package com.accountMicroservice.service.impl;

import com.accountMicroservice.dao.AccountRepository;
import com.accountMicroservice.dao.SalaryAccountApplicationRepository;
import com.accountMicroservice.dto.CreateSalaryApplicationRequest;
import com.accountMicroservice.dto.NotificationRequest;
import com.accountMicroservice.dto.OtpVerifyRequest;
import com.accountMicroservice.dto.OtpVerifyResponse;
import com.accountMicroservice.dto.SalaryApplicationResponse;
import com.accountMicroservice.exception.AccountProcessingException;
import com.accountMicroservice.model.Account;
import com.accountMicroservice.model.AccountStatus;
import com.accountMicroservice.model.AccountType;
import com.accountMicroservice.model.SalaryAccountApplication;
import com.accountMicroservice.model.SalaryApplicationStatus;
import com.accountMicroservice.proxyService.NotificationServiceClient;
import com.accountMicroservice.proxyService.OtpServiceClient;
import com.accountMicroservice.service.SalaryAccountApplicationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
public class SalaryAccountApplicationServiceImpl implements SalaryAccountApplicationService {

    private final SalaryAccountApplicationRepository applicationRepository;
    private final AccountRepository accountRepository;
    private final OtpServiceClient otpServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Path storageRoot = Paths.get("uploads", "salary");

    @Override
    @Transactional
    public SalaryApplicationResponse submitApplication(CreateSalaryApplicationRequest request) {
        // Verify OTP against corporate email using public verify endpoint with CONTACT_VERIFICATION
        OtpVerifyRequest otpReq = new OtpVerifyRequest(
                request.getCorporateEmail(), // userId as email triggers toEmail override in OTP service/notifications
                "CONTACT_VERIFICATION",
                null,
                request.getOtpCode()
        );
        OtpVerifyResponse otpRes = otpServiceClient.verifyPublic(otpReq);
        if (otpRes == null || !otpRes.isVerified()) {
            throw new AccountProcessingException("Corporate email OTP verification failed"
                    + (otpRes != null && otpRes.getMessage() != null ? " - " + otpRes.getMessage() : ""));
        }

        SalaryAccountApplication app = new SalaryAccountApplication();
        app.setUserId(request.getUserId());
        app.setCorporateEmail(request.getCorporateEmail());
        app.setDocumentsJson(toJsonSafe(request.getDocuments()));
        app.setStatus(SalaryApplicationStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());

        try {
            app = applicationRepository.save(app);
        } catch (Exception e) {
            throw new AccountProcessingException("Failed to submit salary account application", e);
        }

        // Notify user
        notifySafe(request.getUserId(),
                "EMAIL",
                "Your Salary/Corporate account application has been submitted and is pending review.",
                null);

        return mapToResponse(app);
    }

    @Override
    public List<SalaryApplicationResponse> getApplicationsByStatus(SalaryApplicationStatus status) {
        return applicationRepository.findByStatusOrderBySubmittedAtDesc(status)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<SalaryApplicationResponse> getMyApplications(String userId) {
        return applicationRepository.findByUserIdOrderBySubmittedAtDesc(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public SalaryApplicationResponse getApplication(String applicationId) {
        SalaryAccountApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AccountProcessingException("Application not found: " + applicationId));
        return mapToResponse(app);
    }

    @Override
    @Transactional
    public SalaryApplicationResponse reviewApplication(String applicationId,
                                                       SalaryApplicationStatus decision,
                                                       String adminComment,
                                                       String reviewerId) {
        SalaryAccountApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AccountProcessingException("Application not found: " + applicationId));

        if (app.getStatus() != SalaryApplicationStatus.SUBMITTED) {
            throw new AccountProcessingException("Only SUBMITTED applications can be reviewed.");
        }

        // If approval requested, enforce cap and create account
        if (decision == SalaryApplicationStatus.APPROVED) {
            long existing = accountRepository.countByUserIdAndAccountTypeAndStatusNot(
                    app.getUserId(), AccountType.SALARY_CORPORATE, AccountStatus.CLOSED);
            if (existing >= 1) {
                // Convert to rejection with reason
                decision = SalaryApplicationStatus.REJECTED;
                adminComment = (adminComment != null ? adminComment + " " : "")
                        + "(Auto-rejected: user already has a non-closed SALARY/CORPORATE account)";
            } else {
                // Create the SALARY_CORPORATE account with initial balance 0.0
                Account account = new Account();
                account.setUserId(app.getUserId());
                account.setAccountNumber(generateUniqueAccountNumber());
                // Ensure uniqueness
                while (accountRepository.findByAccountNumber(account.getAccountNumber()).isPresent()) {
                    account.setAccountNumber(generateUniqueAccountNumber());
                }
                account.setAccountType(AccountType.SALARY_CORPORATE);
                account.setBalance(0.0);
                account.setStatus(AccountStatus.ACTIVE);
                account.setCreatedAt(LocalDateTime.now());
                try {
                    accountRepository.save(account);
                } catch (DataIntegrityViolationException e) {
                    throw new AccountProcessingException("Failed to create SALARY/CORPORATE account (number conflict).", e);
                } catch (Exception e) {
                    throw new AccountProcessingException("Failed to create SALARY/CORPORATE account.", e);
                }

                // Notify approval
                notifySafe(app.getUserId(),
                        "EMAIL",
                        "Your Salary/Corporate account application has been APPROVED. Account Number: " + account.getAccountNumber(),
                        null);
            }
        }

        // For rejection, just notify with reason
        if (decision == SalaryApplicationStatus.REJECTED) {
            notifySafe(app.getUserId(),
                    "EMAIL",
                    "Your Salary/Corporate account application has been REJECTED."
                            + (adminComment != null ? (" Reason: " + adminComment) : ""),
                    null);
        }

        app.setStatus(decision);
        app.setAdminComment(adminComment);
        app.setReviewerId(reviewerId);
        app.setReviewedAt(LocalDateTime.now());

        app = applicationRepository.save(app);
        return mapToResponse(app);
    }

    private SalaryApplicationResponse mapToResponse(SalaryAccountApplication app) {
        return new SalaryApplicationResponse(
                app.getApplicationId(),
                app.getUserId(),
                app.getCorporateEmail(),
                fromJsonSafe(app.getDocumentsJson()),
                app.getStatus(),
                app.getAdminComment(),
                app.getSubmittedAt(),
                app.getReviewedAt(),
                app.getReviewerId()
        );
    }

    private void notifySafe(String userId, String type, String content, String toEmail) {
        try {
            notificationServiceClient.sendEmailNotification(new NotificationRequest(userId, type, content, toEmail));
        } catch (Exception ignore) {
            // best-effort notifications
        }
    }

    private String toJsonSafe(List<String> docs) {
        try {
            if (docs == null) return null;
            return objectMapper.writeValueAsString(docs);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> fromJsonSafe(String json) {
        try {
            if (json == null || json.isEmpty()) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Generate a unique 10-digit numeric account number.
     */
    private String generateUniqueAccountNumber() {
        long positiveNumber = Math.abs(UUID.randomUUID().getMostSignificantBits() % 10_000_000_000L);
        return String.format("%010d", positiveNumber);
    }

    @Override
    @Transactional
    public SalaryApplicationResponse submitApplicationMultipart(String userId,
                                                                String corporateEmail,
                                                                String otpCode,
                                                                MultipartFile[] documents) {
        // Verify OTP for corporate email using public verify endpoint
        OtpVerifyRequest otpReq = new OtpVerifyRequest(
                corporateEmail,
                "CONTACT_VERIFICATION",
                null,
                otpCode
        );
        OtpVerifyResponse otpRes = otpServiceClient.verifyPublic(otpReq);
        if (otpRes == null || !otpRes.isVerified()) {
            throw new AccountProcessingException("Corporate email OTP verification failed"
                    + (otpRes != null && otpRes.getMessage() != null ? " - " + otpRes.getMessage() : ""));
        }

        // Create application first to obtain applicationId for storage pathing
        SalaryAccountApplication app = new SalaryAccountApplication();
        app.setUserId(userId);
        app.setCorporateEmail(corporateEmail);
        app.setStatus(SalaryApplicationStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());

        try {
            app = applicationRepository.save(app);
        } catch (Exception e) {
            throw new AccountProcessingException("Failed to submit salary account application", e);
        }

        // Save documents to disk and persist their relative paths (relative to storageRoot)
        List<String> relPaths = saveDocuments(userId, app.getApplicationId(), documents);
        app.setDocumentsJson(toJsonSafe(relPaths));

        try {
            app = applicationRepository.save(app);
        } catch (Exception e) {
            throw new AccountProcessingException("Failed to persist document paths for salary account application", e);
        }

        // Notify user (best-effort)
        notifySafe(userId,
                "EMAIL",
                "Your Salary/Corporate account application has been submitted and is pending review.",
                null);

        return mapToResponse(app);
    }

    @Override
    public Resource loadDocumentAsResource(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new AccountProcessingException("Document path must be provided.");
        }
        Path rel = Paths.get(relativePath).normalize();
        if (rel.isAbsolute() || relativePath.contains("..")) {
            throw new AccountProcessingException("Invalid document path.");
        }

        Path base = storageRoot.toAbsolutePath().normalize();
        Path absolute = base.resolve(rel).normalize();

        if (!absolute.startsWith(base)) {
            throw new AccountProcessingException("Access to requested document is not allowed.");
        }

        try {
            if (!Files.exists(absolute) || !Files.isReadable(absolute)) {
                throw new AccountProcessingException("Requested document not found.");
            }
            return new UrlResource(absolute.toUri());
        } catch (Exception e) {
            throw new AccountProcessingException("Failed to load requested document.", e);
        }
    }

    @Override
    public String detectContentType(Resource resource) {
        if (resource == null) return "application/octet-stream";
        try {
            // Prefer file system probing when available
            try {
                java.io.File f = resource.getFile();
                String probed = Files.probeContentType(f.toPath());
                if (probed != null && !probed.isBlank()) return probed;
            } catch (Exception ignore) {
                // Not a file-based resource or probing failed
            }

            // Fallback to name-based detection
            String byName = URLConnection.guessContentTypeFromName(resource.getFilename());
            if (byName != null && !byName.isBlank()) return byName;
        } catch (Exception ignore) {
        }
        return "application/octet-stream";
    }

    private List<String> saveDocuments(String userId, String applicationId, MultipartFile[] documents) {
        List<String> rels = new ArrayList<>();
        if (documents == null || documents.length == 0) return rels;

        Path appRoot = storageRoot.resolve(Paths.get(userId, applicationId)).toAbsolutePath().normalize();
        try {
            Files.createDirectories(appRoot);
        } catch (Exception e) {
            throw new AccountProcessingException("Failed to prepare storage for documents.", e);
        }

        for (MultipartFile file : documents) {
            if (file == null || file.isEmpty()) continue;
            String original = file.getOriginalFilename();
            String safe = sanitizeFilename(original);
            String prefixed = System.currentTimeMillis() + "_" + safe;

            Path target = appRoot.resolve(prefixed).normalize();

            // Final defense: ensure still under appRoot
            if (!target.startsWith(appRoot)) {
                throw new AccountProcessingException("Invalid target path derived for upload.");
            }

            try (java.io.InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                throw new AccountProcessingException("Failed to store uploaded document: " + safe, e);
            }

            // Store relative path without the storageRoot prefix: userId/applicationId/filename
            String rel = userId.replace("\\", "/") + "/" + applicationId + "/" + prefixed;
            rels.add(rel);
        }

        return rels;
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "file";
        // Keep alnum, dot, dash, underscore; replace others with underscore
        String safe = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        // Avoid names like "." or ".."
        if (safe.equals(".") || safe.equals("..")) safe = "file";
        // Trim excessive length
        if (safe.length() > 200) safe = safe.substring(safe.length() - 200);
        return safe;
    }
}
