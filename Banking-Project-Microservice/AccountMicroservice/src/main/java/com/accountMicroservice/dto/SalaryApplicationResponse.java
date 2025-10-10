package com.accountMicroservice.dto;

import com.accountMicroservice.model.SalaryApplicationStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Salary/Corporate account applications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryApplicationResponse {
    private String applicationId;
    private String userId;
    private String corporateEmail;
    private List<String> documents;
    private SalaryApplicationStatus status;
    private String adminComment;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String reviewerId;
}
