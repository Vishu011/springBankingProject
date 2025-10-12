package com.accountMicroservice.controller;

import com.accountMicroservice.dto.CreateSalaryApplicationRequest;
import com.accountMicroservice.dto.SalaryApplicationResponse;
import com.accountMicroservice.model.SalaryApplicationStatus;
import com.accountMicroservice.service.SalaryAccountApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ContentDisposition;

/**
 * Endpoints for Salary/Corporate account applications.
 *
 * Routes:
 *  - POST   /accounts/salary/applications                  (user)    submit application (OTP already verified via /otp/public/verify inside service)
 *  - GET    /accounts/salary/applications?status=SUBMITTED (admin)   list by status
 *  - GET    /accounts/salary/applications/mine?userId=...  (user)    list my applications
 *  - GET    /accounts/salary/applications/{id}             (admin)   get application by id
 *  - PUT    /accounts/salary/applications/{id}/review      (admin)   approve/reject with optional adminComment, reviewerId
 */
@RestController
@RequestMapping("/accounts/salary/applications")
@RequiredArgsConstructor
public class SalaryAccountApplicationController {

    private final SalaryAccountApplicationService service;

    @PostMapping
    public ResponseEntity<SalaryApplicationResponse> submit(@Valid @RequestBody CreateSalaryApplicationRequest request) {
        SalaryApplicationResponse resp = service.submitApplication(request);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SalaryApplicationResponse>> listByStatus(
            @RequestParam("status") SalaryApplicationStatus status) {
        return ResponseEntity.ok(service.getApplicationsByStatus(status));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<SalaryApplicationResponse>> myApplications(
            @RequestParam("userId") String userId) {
        return ResponseEntity.ok(service.getMyApplications(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SalaryApplicationResponse> getOne(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.getApplication(id));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SalaryApplicationResponse> review(
            @PathVariable("id") String id,
            @RequestParam("decision") SalaryApplicationStatus decision,
            @RequestParam(value = "adminComment", required = false) String adminComment,
            @RequestParam(value = "reviewerId", required = false) String reviewerId) {
        return ResponseEntity.ok(service.reviewApplication(id, decision, adminComment, reviewerId));
    }

    // New: multipart/form-data submission with document upload
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SalaryApplicationResponse> submitMultipart(
            @RequestPart("userId") String userId,
            @RequestPart("corporateEmail") String corporateEmail,
            @RequestPart("otpCode") String otpCode,
            @RequestPart(value = "documents", required = true) MultipartFile[] documents) {
        SalaryApplicationResponse resp = service.submitApplicationMultipart(userId, corporateEmail, otpCode, documents);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // New: secure document streaming for admin
    @GetMapping("/{id}/documents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> streamDocument(
            @PathVariable("id") String id,
            @RequestParam("path") String relativePath) {
        // Verify requested document belongs to this application
        SalaryApplicationResponse app = service.getApplication(id);
        List<String> docs = app.getDocuments();
        if (docs == null || docs.stream().noneMatch(p -> p != null && p.equals(relativePath))) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = service.loadDocumentAsResource(relativePath);
        String contentType = service.detectContentType(resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(resource.getFilename() != null ? resource.getFilename() : "document")
                .build();
        headers.setContentDisposition(disposition);

        try {
            headers.setContentLength(resource.contentLength());
        } catch (Exception ignore) {
        }

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
}
