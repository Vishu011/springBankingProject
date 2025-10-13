package com.selfservice.controller;

import com.selfservice.model.SelfServiceRequest;
import com.selfservice.model.SelfServiceRequestStatus;
import com.selfservice.service.SelfServiceRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Admin endpoints to review Self Service profile change requests.
 * Routes are available under both /self-service/admin/requests and /admin/requests
 * so that API Gateway rewrite/prefix differences are tolerated.
 */
@RestController
@RequestMapping({"/self-service/admin/requests", "/admin/requests"})
@RequiredArgsConstructor
public class AdminRequestsController {

    private final SelfServiceRequestService service;

    @Data
    public static class AdminDecisionRequest {
        private String adminComment;
        @NotBlank
        private String reviewerId; // could be populated from JWT sub on UI side
    }

    /**
     * List requests by status (defaults to SUBMITTED/pending).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SelfServiceRequest>> listByStatus(@RequestParam(name = "status", required = false) String status) {
        SelfServiceRequestStatus st = SelfServiceRequestStatus.SUBMITTED;
        if (status != null && !status.isBlank()) {
            st = SelfServiceRequestStatus.valueOf(status.toUpperCase());
        }
        return ResponseEntity.ok(service.listByStatus(st));
    }

    /**
     * Get a single request by its id.
     */
    @GetMapping("/{requestId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SelfServiceRequest> getOne(@PathVariable("requestId") String requestId) {
        return ResponseEntity.ok(service.getOne(requestId));
    }

    /**
     * Approve a request. Applies the profile change into User service, stores decision, and notifies user.
     */
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SelfServiceRequest> approve(@PathVariable("requestId") String requestId,
                                                      @RequestBody AdminDecisionRequest body) {
        String reviewer = (body != null && body.getReviewerId() != null) ? body.getReviewerId() : "admin";
        String comment = (body != null) ? body.getAdminComment() : null;
        SelfServiceRequest updated = service.review(requestId, SelfServiceRequestStatus.APPROVED, comment, reviewer);
        return ResponseEntity.ok(updated);
    }

    /**
     * Reject a request with an optional admin comment/reason. Notifies user.
     */
    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SelfServiceRequest> reject(@PathVariable("requestId") String requestId,
                                                     @RequestBody AdminDecisionRequest body) {
        String reviewer = (body != null && body.getReviewerId() != null) ? body.getReviewerId() : "admin";
        String comment = (body != null) ? body.getAdminComment() : null;
        SelfServiceRequest updated = service.review(requestId, SelfServiceRequestStatus.REJECTED, comment, reviewer);
        return ResponseEntity.ok(updated);
    }

    /**
     * Download a stored document for a given request. The relative path is everything after '/documents/' in the URL.
     */
    @GetMapping("/{requestId}/documents/**")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable("requestId") String requestId,
                                                     HttpServletRequest request) {
        // Extract the relative path after '/documents/'
        String uri = request.getRequestURI();
        // Support both mappings (with or without '/self-service' prefix)
        int idx = uri.indexOf("/documents/");
        if (idx < 0) {
            return ResponseEntity.badRequest().build();
        }
        String relative = uri.substring(idx + "/documents/".length());
        // Load the resource using the storage service
        Resource res = service.loadDocumentAsResource(relative);
        String contentType = service.detectContentType(res);
        String encodedFilename = URLEncoder.encode(res.getFilename(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .body(res);
    }
}
