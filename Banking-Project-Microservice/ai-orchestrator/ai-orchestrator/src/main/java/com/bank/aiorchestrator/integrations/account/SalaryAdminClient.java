package com.bank.aiorchestrator.integrations.account;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bank.aiorchestrator.config.FeignOAuth2Config;
import com.bank.aiorchestrator.integrations.account.dto.SalaryApplicationResponse;

import feign.Response;

/**
 * Feign client for AccountMicroservice Salary/Corporate applications admin endpoints.
 * Base URL via orchestrator.integrations.accountServiceBaseUrl.
 */
@FeignClient(
        name = "account-service-salary-admin",
        url = "${orchestrator.integrations.gatewayBaseUrl}",
        configuration = FeignOAuth2Config.class
)
public interface SalaryAdminClient {

    @GetMapping(value = "/accounts/salary/applications", consumes = MediaType.ALL_VALUE)
    List<SalaryApplicationResponse> listByStatus(@RequestParam("status") String status);

    @GetMapping(value = "/accounts/salary/applications/{id}", consumes = MediaType.ALL_VALUE)
    SalaryApplicationResponse getOne(@PathVariable("id") String id);

    @PutMapping(value = "/accounts/salary/applications/{id}/review", consumes = MediaType.ALL_VALUE)
    SalaryApplicationResponse review(@PathVariable("id") String id,
                                     @RequestParam("decision") String decision,
                                     @RequestParam(value = "adminComment", required = false) String adminComment,
                                     @RequestParam(value = "reviewerId", required = false) String reviewerId);

    @GetMapping(value = "/accounts/salary/applications/{id}/documents", consumes = MediaType.ALL_VALUE)
    Response downloadDocument(@PathVariable("id") String id, @RequestParam("path") String path);
}
