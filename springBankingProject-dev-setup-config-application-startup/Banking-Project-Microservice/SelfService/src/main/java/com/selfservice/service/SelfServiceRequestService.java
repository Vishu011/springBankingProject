package com.selfservice.service;

import com.selfservice.model.SelfServiceRequest;
import com.selfservice.model.SelfServiceRequestStatus;
import com.selfservice.model.SelfServiceRequestType;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SelfServiceRequestService {
    // Submit a new self-service request (multipart with mandatory documents)
    SelfServiceRequest submit(String userId, SelfServiceRequestType type, String payloadJson, MultipartFile[] documents);

    // Admin: list by status
    List<SelfServiceRequest> listByStatus(SelfServiceRequestStatus status);

    // User: list own requests
    List<SelfServiceRequest> listMine(String userId);

    // Admin: get one
    SelfServiceRequest getOne(String requestId);

    // Admin: review approve/reject; when approving, apply change to User service
    SelfServiceRequest review(String requestId, SelfServiceRequestStatus decision, String adminComment, String reviewerId);

    // Document access
    Resource loadDocumentAsResource(String relativePath);
    String detectContentType(Resource resource);
}
