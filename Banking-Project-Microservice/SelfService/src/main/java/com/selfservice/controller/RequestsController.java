package com.selfservice.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfservice.model.SelfServiceRequest;
import com.selfservice.model.SelfServiceRequestType;
import com.selfservice.service.SelfServiceRequestService;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * User-facing Self Service Requests controller (submit + list mine).
 * Delegates to the JPA-driven SelfServiceRequestService so that admin review API
 * (list/get/approve/reject) can see the same requests and statuses.
 *
 * Available under both /self-service/requests and /requests to tolerate gateway rewrites.
 */
@RestController
@RequestMapping({"/self-service/requests", "/requests"})
@RequiredArgsConstructor
public class RequestsController {

    private final SelfServiceRequestService requestService;

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    // DTOs used for API responses
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelfServiceRequestDto {
        private String requestId;
        private String userId;
        private String type; // NAME_CHANGE | DOB_CHANGE | ADDRESS_CHANGE
        private String status; // SUBMITTED | APPROVED | REJECTED
        private String adminComment;
        private LocalDateTime submittedAt;
        private LocalDateTime reviewedAt;
        private String payloadJson;
        private List<String> documents; // relative paths (uploads/self-service/{userId}/{requestId}/*)
    }

    @Data
    public static class CreateRequestForm {
        @NotBlank
        private String userId;
        @NotBlank
        private String type; // NAME_CHANGE | DOB_CHANGE | ADDRESS_CHANGE
        private String payloadJson; // UI auto-generates from structured inputs
    }

    /**
     * Submit a new self-service request (documents are MANDATORY).
     * Saves documents to disk and persists request metadata in DB with status SUBMITTED.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated() and #userId == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<?> submitRequest(
            @RequestParam("userId") String userId,
            @RequestParam("type") String type,
            @RequestParam(value = "payloadJson", required = false) String payloadJson,
            @RequestPart("documents") MultipartFile[] documents
    ) {
        if (documents == null || documents.length == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Documents are required.");
        }
        try {
            SelfServiceRequestType t = SelfServiceRequestType.valueOf(Objects.requireNonNull(type).toUpperCase());
            SelfServiceRequest saved = requestService.submit(userId, t, payloadJson, documents);
            return new ResponseEntity<>(toDto(saved), HttpStatus.CREATED);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to submit request: " + ex.getMessage());
        }
    }

    /**
     * List my requests (latest first).
     */
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated() and #userId == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<List<SelfServiceRequestDto>> listMine(@RequestParam("userId") String userId) {
        List<SelfServiceRequest> list = requestService.listMine(userId);
        List<SelfServiceRequestDto> out = list.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    private SelfServiceRequestDto toDto(SelfServiceRequest r) {
        if (r == null) return null;
        return new SelfServiceRequestDto(
                r.getRequestId(),
                r.getUserId(),
                r.getType() != null ? r.getType().name() : null,
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getAdminComment(),
                r.getSubmittedAt(),
                r.getReviewedAt(),
                r.getPayloadJson(),
                r.getDocuments()
        );
    }
}
