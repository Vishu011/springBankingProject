package com.userMicroservice.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.userMicroservice.dao.KycApplicationRepository;
import com.userMicroservice.dto.KycStatusUpdateRequest;
import com.userMicroservice.exceptions.UserNotFoundException;
import com.userMicroservice.model.KycApplication;
import com.userMicroservice.model.KycReviewStatus;
import com.userMicroservice.model.KycStatus;

/**
 * Implementation of KycService that persists KYC applications
 * and handles document storage + admin review workflow.
 */
@Service
public class KycServiceImpl implements KycService {

    private final KycApplicationRepository kycRepo;
    private final UserService userService;

    // Simple on-disk storage under project dir. In production, move to S3/Blob storage.
    private final Path storageRoot = Paths.get("uploads", "kyc");

    private static final Pattern AADHAR_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");

    @Autowired
    public KycServiceImpl(KycApplicationRepository kycRepo, UserService userService) {
        this.kycRepo = kycRepo;
        this.userService = userService;
        ensureStorageRoot();
    }

    private void ensureStorageRoot() {
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize KYC storage root: " + storageRoot.toAbsolutePath(), e);
        }
    }

    private void validateIds(String aadharNumber, String panNumber) {
        if (aadharNumber == null || !AADHAR_PATTERN.matcher(aadharNumber).matches()) {
            throw new IllegalArgumentException("Invalid Aadhar number. Must be 12 digits.");
        }
        if (panNumber == null || !PAN_PATTERN.matcher(panNumber).matches()) {
            throw new IllegalArgumentException("Invalid PAN number. Expected format: 5 letters, 4 digits, 1 letter (e.g., ABCDE1234F).");
        }
    }

    private void validateAddress(String line1, String city, String state, String postal) {
        if (isBlank(line1) || isBlank(city) || isBlank(state) || isBlank(postal)) {
            throw new IllegalArgumentException("Address incomplete. line1, city, state, and postalCode are required.");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @Override
    @Transactional
    public KycApplication submitApplication(
        String userId,
        String aadharNumber,
        String panNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        MultipartFile[] documents
    ) {
        if (isBlank(userId)) {
            throw new IllegalArgumentException("UserId is required.");
        }
        // Validate user existence
        userService.getUserProfileById(userId).orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        validateIds(aadharNumber, panNumber);
        validateAddress(addressLine1, city, state, postalCode);

        KycApplication app = new KycApplication();
        app.setUserId(userId);
        app.setAadharNumber(aadharNumber);
        app.setPanNumber(panNumber);
        app.setAddressLine1(addressLine1);
        app.setAddressLine2(addressLine2);
        app.setCity(city);
        app.setState(state);
        app.setPostalCode(postalCode);
        app.setReviewStatus(KycReviewStatus.SUBMITTED);
        app.setSubmittedAt(LocalDateTime.now());

        // Persist first to get applicationId for folder naming
        app = kycRepo.save(app);

        // Store documents (if any)
        if (documents != null && documents.length > 0) {
            final String applicationId = app.getApplicationId();
            Path appDir = storageRoot.resolve(userId).resolve(applicationId);
            try {
                Files.createDirectories(appDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed creating application storage dir: " + appDir.toAbsolutePath(), e);
            }

            for (MultipartFile file : documents) {
                if (file == null || file.isEmpty()) continue;
                String safeName = sanitizeFilename(file.getOriginalFilename());
                String timestamped = System.currentTimeMillis() + "_" + safeName;
                Path target = appDir.resolve(timestamped);
                try {
                    // Use NIO copy which is more robust across temp-backed multipart implementations
                    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
                    // Store relative path
                    Path rel = storageRoot.relativize(target);
                    app.getDocumentPaths().add(rel.toString().replace("\\", "/"));
                } catch (IOException e) {
                    throw new RuntimeException("Failed saving KYC document: " + safeName, e);
                }
            }
        }

        // Save updated doc paths
        app = kycRepo.save(app);

        return app;
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycApplication> listApplicationsByStatus(KycReviewStatus status) {
        return kycRepo.findByReviewStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public KycApplication getApplicationById(String applicationId) {
        return kycRepo.findById(applicationId).orElseThrow(() -> new IllegalArgumentException("KYC Application not found: " + applicationId));
    }

    @Override
    @Transactional
    public KycApplication reviewApplication(String applicationId, KycReviewStatus decision, String adminComment) {
        KycApplication app = getApplicationById(applicationId);
        app.setReviewStatus(decision);
        app.setAdminComment(adminComment);
        app.setReviewedAt(LocalDateTime.now());
        app = kycRepo.save(app);

        // Update user's KYC status accordingly and emit notification via existing flow
        if (decision == KycReviewStatus.APPROVED) {
            userService.updateKycStatus(app.getUserId(), new KycStatusUpdateRequest(KycStatus.VERIFIED, null));
        } else if (decision == KycReviewStatus.REJECTED) {
            userService.updateKycStatus(app.getUserId(), new KycStatusUpdateRequest(KycStatus.REJECTED, adminComment));
        }
        return app;
    }

    /**
     * Resolve a stored document path to a Spring Resource for download/preview.
     */
    public Resource loadDocumentAsResource(String relativePath) {
        try {
            Path file = storageRoot.resolve(relativePath).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("Document not found or unreadable: " + relativePath);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid document path: " + relativePath, e);
        }
    }

    private String sanitizeFilename(String original) {
        if (original == null) return "document";
        String safe = original.trim();
        // Replace any character not alphanumeric, dot, underscore, or dash with underscore
        safe = safe.replaceAll("[^A-Za-z0-9._-]", "_");
        // Prevent hidden/empty names
        if (safe.startsWith(".")) {
            safe = "file" + safe;
        }
        // Limit length and preserve extension
        if (safe.length() > 200) {
            String ext = "";
            int dot = safe.lastIndexOf('.');
            if (dot > 0) {
                ext = safe.substring(dot);
                safe = safe.substring(0, dot);
            }
            safe = safe.substring(0, Math.min(180, safe.length())) + ext;
        }
        if (safe.isBlank()) {
            safe = "document";
        }
        return safe;
    }

    /**
     * Helper to get MIME type, optional if you need to set Content-Type explicitly.
     */
    public String detectContentType(Resource resource) {
        try {
            return Files.probeContentType(Paths.get(resource.getURI()));
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
}
