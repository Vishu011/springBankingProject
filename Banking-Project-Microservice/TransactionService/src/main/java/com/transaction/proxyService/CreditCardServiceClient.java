package com.transaction.proxyService;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.transaction.config.FeignClientConfiguration;
import com.transaction.dto.DebitCardValidationRequest;
import com.transaction.dto.DebitCardValidationResponse;

@FeignClient(name = "credit-card-service", path = "/cards", configuration = FeignClientConfiguration.class)
public interface CreditCardServiceClient {

    @PostMapping("/debit/validate-transaction")
    DebitCardValidationResponse validateDebitTransaction(@RequestBody DebitCardValidationRequest request);
}
