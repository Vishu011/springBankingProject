package com.bank.aiorchestrator.integrations.selfservice;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.bank.aiorchestrator.config.FeignOAuth2Config;
import com.bank.aiorchestrator.integrations.selfservice.dto.SelfServiceAdminDecisionRequest;
import com.bank.aiorchestrator.integrations.selfservice.dto.SelfServiceRequest;

/**
 * Feign client for SelfService admin endpoints.
 * Uses either /self-service/admin/requests or /admin/requests per gateway; we target the first path here.
 */
@FeignClient(
        name = "self-service-admin",
        url = "${orchestrator.integrations.gatewayBaseUrl}",
        configuration = FeignOAuth2Config.class
)
public interface SelfServiceAdminClient {

    @GetMapping(value = "/self-service/admin/requests", consumes = MediaType.ALL_VALUE)
    List<SelfServiceRequest> listByStatus(@RequestParam(name = "status", required = false, defaultValue = "SUBMITTED") String status);

    @GetMapping(value = "/self-service/admin/requests/{requestId}", consumes = MediaType.ALL_VALUE)
    SelfServiceRequest getOne(@PathVariable("requestId") String requestId);

    @PostMapping(value = "/self-service/admin/requests/{requestId}/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    SelfServiceRequest approve(@PathVariable("requestId") String requestId,
                               @RequestBody SelfServiceAdminDecisionRequest body);

    @PostMapping(value = "/self-service/admin/requests/{requestId}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    SelfServiceRequest reject(@PathVariable("requestId") String requestId,
                              @RequestBody SelfServiceAdminDecisionRequest body);

    /**
     * Download a stored document for a given request.
     * relativePath captures the entire path after '/documents/' using a regex.
     */
    @GetMapping(value = "/self-service/admin/requests/{requestId}/documents/{relativePath:.+}", consumes = MediaType.ALL_VALUE)
    ResponseEntity<byte[]> downloadDocument(@PathVariable("requestId") String requestId,
                                               @PathVariable("relativePath") String relativePath);
}
