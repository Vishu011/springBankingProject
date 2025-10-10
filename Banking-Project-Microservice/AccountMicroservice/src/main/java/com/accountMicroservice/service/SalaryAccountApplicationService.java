package com.accountMicroservice.service;

import com.accountMicroservice.dto.CreateSalaryApplicationRequest;
import com.accountMicroservice.dto.SalaryApplicationResponse;
import com.accountMicroservice.model.SalaryApplicationStatus;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface SalaryAccountApplicationService {

    SalaryApplicationResponse submitApplication(CreateSalaryApplicationRequest request);

    List<SalaryApplicationResponse> getApplicationsByStatus(SalaryApplicationStatus status);

    List<SalaryApplicationResponse> getMyApplications(String userId);

    SalaryApplicationResponse getApplication(String applicationId);

    SalaryApplicationResponse reviewApplication(String applicationId, SalaryApplicationStatus decision, String adminComment, String reviewerId);

    // New: multipart submission and document streaming
    SalaryApplicationResponse submitApplicationMultipart(String userId,
                                                         String corporateEmail,
                                                         String otpCode,
                                                         MultipartFile[] documents);

    Resource loadDocumentAsResource(String relativePath);

    String detectContentType(Resource resource);
}
