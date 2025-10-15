package com.bank.aiorchestrator.integrations.loan;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.bank.aiorchestrator.config.FeignOAuth2Config;
import com.bank.aiorchestrator.integrations.loan.dto.LoanRejectionRequest;
import com.bank.aiorchestrator.integrations.loan.dto.LoanResponseDto;

/**
 * Feign client for Loan Service admin endpoints.
 * Base URL via orchestrator.integrations.loanServiceBaseUrl.
 */
@FeignClient(
        name = "loan-service-admin",
        url = "${orchestrator.integrations.gatewayBaseUrl}",
        configuration = FeignOAuth2Config.class
)
public interface LoanAdminClient {

    @GetMapping(value = "/loans", consumes = MediaType.ALL_VALUE)
    List<LoanResponseDto> getAllLoans();

    @PutMapping(value = "/loans/{loanId}/approve", consumes = MediaType.ALL_VALUE)
    LoanResponseDto approve(@PathVariable("loanId") String loanId);

    @PutMapping(value = "/loans/{loanId}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    LoanResponseDto reject(@PathVariable("loanId") String loanId, @RequestBody LoanRejectionRequest request);
}
