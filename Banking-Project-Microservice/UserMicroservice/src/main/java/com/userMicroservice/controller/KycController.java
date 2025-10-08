package com.userMicroservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

import com.userMicroservice.model.KycApplication;
import com.userMicroservice.model.KycReviewStatus;
import com.userMicroservice.service.KycService;

/**
 * KYC Controller:
 * - Authenticated users submit their KYC application (IDs, address, documents)
 * - Admin reviews applications (approve/reject with optional comment)
 */
@RestController
@RequestMapping("/auth/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    /**
     * Submit a new KYC application for the authenticated user.
     * Consumes multipart/form-data with optional documents (images/pdf).
     */
    @PostMapping(value = "/applications", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<KycApplication> submitKyc(
            Authentication authentication,
            @RequestParam("aadharNumber") String aadharNumber,
            @RequestParam("panNumber") String panNumber,
            @RequestParam("addressLine1") String addressLine1,
            @RequestParam(value = "addressLine2", required = false) String addressLine2,
            @RequestParam("city") String city,
            @RequestParam("state") String state,
            @RequestParam("postalCode") String postalCode,
            @RequestParam(value = "documents", required = false) MultipartFile[] documents) {

        String userId = authentication.getName();
        KycApplication app = kycService.submitApplication(
                userId,
                aadharNumber,
                panNumber,
                addressLine1,
                addressLine2,
                city,
                state,
                postalCode,
                documents
        );
        return new ResponseEntity<>(app, HttpStatus.CREATED);
    }

    /**
     * List KYC applications by review status (ADMIN only).
     */
    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<KycApplication>> listByStatus(
            @RequestParam(name = "status", defaultValue = "SUBMITTED") KycReviewStatus status) {
        return ResponseEntity.ok(kycService.listApplicationsByStatus(status));
    }

    /**
     * Get a specific application by id (ADMIN only).
     * (Can be extended to allow owner to view their own application.)
     */
    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KycApplication> getById(@PathVariable String applicationId) {
        return ResponseEntity.ok(kycService.getApplicationById(applicationId));
    }

    /**
     * Review an application (approve/reject) with optional admin comment (ADMIN only).
     * When approved/rejected, the user's KYC status is updated and notification emitted.
     */
    @PutMapping("/applications/{applicationId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KycApplication> review(
            @PathVariable String applicationId,
            @RequestParam("decision") KycReviewStatus decision,
            @RequestParam(value = "adminComment", required = false) String adminComment) {
        KycApplication updated = kycService.reviewApplication(applicationId, decision, adminComment);
        return ResponseEntity.ok(updated);
    }

    /**
     * Download a stored KYC document by relative path (ADMIN only).
     * Example: GET /auth/kyc/applications/{applicationId}/documents?path=userId/appId/filename.pdf
     */
    @GetMapping(value = "/applications/{applicationId}/documents", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable String applicationId,
            @RequestParam("path") String path) {
        Resource resource = kycService.loadDocumentAsResource(path);
        String contentType = kycService.detectContentType(resource);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
