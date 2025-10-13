package com.selfservice.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfservice.dao.SelfServiceRequestRepository;
import com.selfservice.dto.NotificationRequest;
import com.selfservice.model.SelfServiceRequest;
import com.selfservice.model.SelfServiceRequestStatus;
import com.selfservice.model.SelfServiceRequestType;
import com.selfservice.proxy.NotificationServiceClient;
import com.selfservice.proxy.UserServiceClient;
import com.selfservice.proxy.dto.UserUpdateRequest;
import com.selfservice.service.SelfServiceRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SelfServiceRequestServiceImpl implements SelfServiceRequestService {

    private final SelfServiceRequestRepository repo;
    private final NotificationServiceClient notificationClient;
    private final UserServiceClient userServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path storageRoot = Paths.get("uploads", "self-service");

    private Path ensureDir(Path p) {
        try {
            Files.createDirectories(p);
            return p;
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare storage directory: " + p.toAbsolutePath(), e);
        }
    }

    private String sanitizeFilename(String name) {
        if (name == null || name.isBlank()) return "file";
        String safe = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safe.equals(".") || safe.equals("..")) safe = "file";
        if (safe.length() > 200) safe = safe.substring(safe.length() - 200);
        return safe;
    }

    @Override
    @Transactional
    public SelfServiceRequest submit(String userId, SelfServiceRequestType type, String payloadJson, MultipartFile[] documents) {
        if (documents == null || documents.length == 0) {
            throw new IllegalArgumentException("Documents are mandatory for self-service requests.");
        }
        // Create request first to get requestId for storage path
        SelfServiceRequest req = SelfServiceRequest.submitNew(userId, type, payloadJson, new ArrayList<>());
        req = repo.save(req);

        // Store docs under uploads/self-service/{userId}/{requestId}/
        List<String> rels = saveDocuments(userId, req.getRequestId(), documents);
        req.setDocuments(rels);
        req = repo.save(req);

        // Notify user (best-effort)
        notifySafe(userId, "EMAIL", "Your self-service " + type + " request has been submitted and is pending admin review.", null);

        return req;
    }

    private List<String> saveDocuments(String userId, String requestId, MultipartFile[] documents) {
        List<String> rels = new ArrayList<>();
        Path appRoot = ensureDir(storageRoot.resolve(Paths.get(userId, requestId)).toAbsolutePath().normalize());
        for (MultipartFile file : documents) {
            if (file == null || file.isEmpty()) continue;
            String safe = sanitizeFilename(file.getOriginalFilename());
            String prefixed = System.currentTimeMillis() + "_" + safe;
            Path target = appRoot.resolve(prefixed).normalize();
            if (!target.startsWith(appRoot)) throw new RuntimeException("Invalid document target path.");
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                throw new RuntimeException("Failed to store uploaded document: " + safe, e);
            }
            String rel = userId.replace("\\", "/") + "/" + requestId + "/" + prefixed;
            rels.add(rel);
        }
        if (rels.isEmpty()) throw new IllegalArgumentException("Documents are mandatory for self-service requests.");
        return rels;
    }

    @Override
    public List<SelfServiceRequest> listByStatus(SelfServiceRequestStatus status) {
        return repo.findByStatusOrderBySubmittedAtAsc(status);
    }

    @Override
    public List<SelfServiceRequest> listMine(String userId) {
        return repo.findByUserIdOrderBySubmittedAtDesc(userId);
    }

    @Override
    public SelfServiceRequest getOne(String requestId) {
        return repo.findById(requestId).orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));
    }

    @Override
    @Transactional
    public SelfServiceRequest review(String requestId, SelfServiceRequestStatus decision, String adminComment, String reviewerId) {
        if (decision == null) throw new IllegalArgumentException("Decision is required.");
        if (decision != SelfServiceRequestStatus.APPROVED && decision != SelfServiceRequestStatus.REJECTED) {
            throw new IllegalArgumentException("Decision must be APPROVED or REJECTED.");
        }
        SelfServiceRequest req = getOne(requestId);
        if (req.getStatus() != SelfServiceRequestStatus.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED requests can be reviewed.");
        }
        if (decision == SelfServiceRequestStatus.APPROVED) {
            // Apply change to user-service
            applyChangeToUser(req);
            req.approve(reviewerId, adminComment);
            repo.save(req);
            notifySafe(req.getUserId(), "EMAIL", "Your self-service " + req.getType() + " request has been APPROVED.", null);
        } else {
            req.reject(reviewerId, adminComment);
            repo.save(req);
            notifySafe(req.getUserId(), "EMAIL", "Your self-service " + req.getType() + " request has been REJECTED."
                    + (adminComment != null ? (" Reason: " + adminComment) : ""), null);
        }
        return req;
    }

    private void applyChangeToUser(SelfServiceRequest req) {
        try {
            Map<String, Object> payload = new ObjectMapper().readValue(
                    req.getPayloadJson() == null ? "{}" : req.getPayloadJson(),
                    new TypeReference<Map<String, Object>>() {});
            UserUpdateRequest update = new UserUpdateRequest();
            switch (req.getType()) {
                case NAME_CHANGE -> {
                    // Support nested { name: { firstName, middleName, lastName } } and flat keys
                    Object nameObj = payload.get("name");
                    if (nameObj instanceof Map<?, ?> nameMap) {
                        Object fn = nameMap.get("firstName");
                        Object mn = nameMap.get("middleName");
                        Object ln = nameMap.get("lastName");
                        if (fn != null && !String.valueOf(fn).isBlank()) update.setFirstName(String.valueOf(fn).trim());
                        if (mn != null && !String.valueOf(mn).isBlank()) update.setMiddleName(String.valueOf(mn).trim());
                        if (ln != null && !String.valueOf(ln).isBlank()) update.setLastName(String.valueOf(ln).trim());
                    } else {
                        Object fn = payload.get("firstName");
                        Object mn = payload.get("middleName");
                        Object ln = payload.get("lastName");
                        if (fn != null && !String.valueOf(fn).isBlank()) update.setFirstName(String.valueOf(fn).trim());
                        if (mn != null && !String.valueOf(mn).isBlank()) update.setMiddleName(String.valueOf(mn).trim());
                        if (ln != null && !String.valueOf(ln).isBlank()) update.setLastName(String.valueOf(ln).trim());
                    }
                }
                case DOB_CHANGE -> {
                    // Accept either 'dateOfBirth' or 'dob'
                    Object dobVal = payload.containsKey("dateOfBirth") ? payload.get("dateOfBirth") : payload.get("dob");
                    if (dobVal != null && !String.valueOf(dobVal).isBlank()) {
                        update.setDateOfBirth(java.time.LocalDate.parse(String.valueOf(dobVal)));
                    }
                }
                case ADDRESS_CHANGE -> {
                    // Accept structured address object or a preformatted string
                    Object addrObj = payload.get("address");
                    if (addrObj instanceof Map<?, ?> addr) {
                        String line1 = str(addr.get("line1"));
                        String line2 = str(addr.get("line2"));
                        String city = str(addr.get("city"));
                        String state = str(addr.get("state"));
                        String postalCode = str(addr.get("postalCode"));
                        String country = str(addr.get("country"));
                        String formatted = joinNonBlank(", ",
                                line1,
                                line2,
                                city,
                                state,
                                postalCode,
                                country
                        );
                        if (!formatted.isBlank()) update.setAddress(formatted);
                    } else if (addrObj != null && !String.valueOf(addrObj).isBlank()) {
                        update.setAddress(String.valueOf(addrObj).trim());
                    } else if (payload.containsKey("fullAddress")) {
                        Object fa = payload.get("fullAddress");
                        if (fa != null && !String.valueOf(fa).isBlank()) update.setAddress(String.valueOf(fa).trim());
                    }
                }
                default -> throw new IllegalStateException("Unsupported type: " + req.getType());
            }
            userServiceClient.updateUser(req.getUserId(), update);
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply approved change to user profile: " + e.getMessage(), e);
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }

    private static String joinNonBlank(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (sb.length() > 0) sb.append(sep);
                sb.append(p);
            }
        }
        return sb.toString();
    }

    private void notifySafe(String userId, String type, String content, String toEmail) {
        try {
            notificationClient.sendEmailNotification(new NotificationRequest(userId, type, content, toEmail));
        } catch (Exception ignore) {
        }
    }

    @Override
    public Resource loadDocumentAsResource(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Document path must be provided.");
        }
        Path rel = Paths.get(relativePath).normalize();
        if (rel.isAbsolute() || relativePath.contains("..")) {
            throw new IllegalArgumentException("Invalid document path.");
        }
        Path base = storageRoot.toAbsolutePath().normalize();
        Path absolute = base.resolve(rel).normalize();
        if (!absolute.startsWith(base)) {
            throw new IllegalArgumentException("Access to requested document is not allowed.");
        }
        try {
            if (!Files.exists(absolute) || !Files.isReadable(absolute)) {
                throw new IllegalArgumentException("Requested document not found.");
            }
            return new UrlResource(absolute.toUri());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load requested document.", e);
        }
    }

    @Override
    public String detectContentType(Resource resource) {
        if (resource == null) return "application/octet-stream";
        try {
            try {
                java.io.File f = resource.getFile();
                String probed = Files.probeContentType(f.toPath());
                if (probed != null && !probed.isBlank()) return probed;
            } catch (Exception ignore) {}
            String byName = URLConnection.guessContentTypeFromName(resource.getFilename());
            if (byName != null && !byName.isBlank()) return byName;
        } catch (Exception ignore) {}
        return "application/octet-stream";
    }
}
