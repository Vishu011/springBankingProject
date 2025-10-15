package com.bank.aiorchestrator.integrations.card;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.bank.aiorchestrator.config.FeignOAuth2Config;
import com.bank.aiorchestrator.integrations.card.dto.CardApplicationResponse;
import com.bank.aiorchestrator.integrations.card.dto.ReviewCardApplicationRequest;

/**
 * Feign client for CreditCardService admin endpoints.
 * Base URL via orchestrator.integrations.creditCardServiceBaseUrl.
 */
@FeignClient(
        name = "card-service-admin",
        url = "${orchestrator.integrations.gatewayBaseUrl}",
        configuration = FeignOAuth2Config.class
)
public interface CardAdminClient {

    @GetMapping(value = "/cards/applications", consumes = MediaType.ALL_VALUE)
    List<CardApplicationResponse> listApplicationsByStatus(@RequestParam(name = "status", defaultValue = "SUBMITTED") String status);

    @GetMapping(value = "/cards/applications/{id}", consumes = MediaType.ALL_VALUE)
    CardApplicationResponse getApplication(@PathVariable("id") String id);

    @PutMapping(value = "/cards/applications/{id}/review", consumes = MediaType.APPLICATION_JSON_VALUE)
    CardApplicationResponse reviewApplication(@PathVariable("id") String id,
                                              @RequestBody ReviewCardApplicationRequest request);
}
