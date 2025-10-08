package com.userMicroservice.service;

import java.util.List;

import com.userMicroservice.model.KycApplication;
import com.userMicroservice.model.KycReviewStatus;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

public interface KycService {

    KycApplication submitApplication(
        String userId,
        String aadharNumber,
        String panNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        MultipartFile[] documents
    );

    List<KycApplication> listApplicationsByStatus(KycReviewStatus status);

    KycApplication getApplicationById(String applicationId);

    KycApplication reviewApplication(String applicationId, KycReviewStatus decision, String adminComment);

    // Document access helpers
    Resource loadDocumentAsResource(String relativePath);

    String detectContentType(Resource resource);
}
