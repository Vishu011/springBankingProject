package com.accountMicroservice.proxyService;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.accountMicroservice.config.FeignClientConfiguration;
import com.accountMicroservice.dto.FineRequest;

/**
 * Feign client for Transaction Service internal operations.
 */
@FeignClient(name = "transaction-service", path = "/transactions", configuration = FeignClientConfiguration.class)
public interface TransactionServiceClient {

    /**
     * Records a fine transaction (type FINE) for an account.
     * This is an internal call initiated by Account Service when a fine is applied or recovered.
     */
    @PostMapping("/fine")
    void recordFine(@RequestBody FineRequest request);
}
