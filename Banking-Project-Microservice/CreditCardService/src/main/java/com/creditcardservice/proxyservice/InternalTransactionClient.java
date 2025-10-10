package com.creditcardservice.proxyservice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import com.creditcardservice.dto.InternalDebitRequest;

@FeignClient(name = "transaction-service", path = "/transactions/internal")
public interface InternalTransactionClient {

    @PostMapping("/debit")
    void debit(InternalDebitRequest request);
}
