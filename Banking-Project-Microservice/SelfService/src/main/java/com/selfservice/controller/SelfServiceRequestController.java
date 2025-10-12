package com.selfservice.controller;

import com.selfservice.model.SelfServiceRequest;
import com.selfservice.model.SelfServiceRequestStatus;
import com.selfservice.model.SelfServiceRequestType;
import com.selfservice.service.SelfServiceRequestService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/self-service/requests")
@RequiredArgsConstructor
public class SelfServiceRequestController {

    private final SelfServiceRequestService service;

    // Submit new request (multipart; documents mandatory)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated() and #userId == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<SelfServiceRequest> submit(@RequestPart("userId") @NotBlank String userId,
                                                     @RequestPart("type") @NotBlank String type,
                                                     @RequestPart("payloadJson") String payloadJson,
                                                     @RequestPart(value = "documents", required = true) MultipartFile[] documents) {
        SelfServiceRequestType t = SelfServiceRequestType.valueOf(type.trim().toUpperCase());
        SelfServiceRequest req = service.submit(userId, t, payloadJson, documents);
        return ResponseEntity.status(201).body(req);
    }

    // Admin: list by status
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SelfServiceRequest>> listByStatus(@RequestParam(name = "status", defaultValue = "SUBMITTED") String status) {
        SelfServiceRequestStatus st = SelfServiceRequestStatus.valueOf(status.trim().toUpperCase());
        return ResponseEntity.ok(service.listByStatus(st));
    }

    // User: list mine
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated() and #userId == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<List<SelfServiceRequest>> listMine(@RequestParam("userId") String userId) {
        return ResponseEntity.ok(service.listMine(userId));
    }

    // Admin: get one
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SelfServiceRequest> getOne(@PathVariable("id") String id) {
        return ResponseEntity.ok(service.getOne(id));
    }

    // Admin: review approve/reject
    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SelfServiceRequest> review(@PathVariable("id") String id,
                                                     @RequestParam("decision") String decision,
                                                     @RequestParam(value = "adminComment", required = false) String adminComment,
                                                     @RequestParam(value = "reviewerId", required = false) String reviewerId) {
        SelfServiceRequestStatus st = SelfServiceRequestStatus.valueOf(decision.trim().toUpperCase());
        return ResponseEntity.ok(service.review(id, st, adminComment, reviewerId));
    }

    // Admin: stream stored documents (validate that path belongs to the request if needed by service layer)
    @GetMapping(value = "/{id}/documents", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> streamDocument(@PathVariable("id") String id,
                                                   @RequestParam("path") String relativePath) {
        // In this version, we trust service-level path checks. Optionally, we could load request and verify membership.
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
        } catch (Exception ignore) { }

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }
}
