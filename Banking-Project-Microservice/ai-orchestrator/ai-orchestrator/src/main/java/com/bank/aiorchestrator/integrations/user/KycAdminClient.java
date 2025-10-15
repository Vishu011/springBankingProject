package com.bank.aiorchestrator.integrations.user;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bank.aiorchestrator.config.FeignOAuth2Config;
import com.bank.aiorchestrator.integrations.user.dto.KycApplicationDto;
import com.bank.aiorchestrator.integrations.user.dto.KycReviewStatus;

import feign.Response;

/**
 * Feign client to call UserMicroservice KYC admin endpoints directly.
 * Base URL is configured via orchestrator.integrations.userServiceBaseUrl.
 */
@FeignClient(
        name = "user-service-kyc",
        url = "${orchestrator.integrations.gatewayBaseUrl}",
        configuration = FeignOAuth2Config.class
)
public interface KycAdminClient {

    @GetMapping(value = "/auth/kyc/applications", consumes = MediaType.ALL_VALUE)
    List<KycApplicationDto> listByStatus(@RequestParam("status") KycReviewStatus status);

    @GetMapping(value = "/auth/kyc/applications/{applicationId}", consumes = MediaType.ALL_VALUE)
    KycApplicationDto getById(@PathVariable("applicationId") String applicationId);

    @PutMapping(value = "/auth/kyc/applications/{applicationId}/review", consumes = MediaType.ALL_VALUE)
    KycApplicationDto review(@PathVariable("applicationId") String applicationId,
                             @RequestParam("decision") KycReviewStatus decision,
                             @RequestParam(value = "adminComment", required = false) String adminComment);

    @GetMapping(value = "/auth/kyc/applications/{applicationId}/documents", consumes = MediaType.ALL_VALUE)
    Response downloadDocument(@PathVariable("applicationId") String applicationId,
                              @RequestParam("path") String path);
}
