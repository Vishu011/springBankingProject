package com.bank.loan.client;

import com.bank.loan.dto.OtpVerifyRequest;
import com.bank.loan.dto.OtpVerifyResponse;
import com.bank.loan.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "otp-service", path = "/otp", configuration = FeignClientConfiguration.class)
public interface OtpServiceClient {

    @PostMapping("/verify")
    OtpVerifyResponse verify(@RequestBody OtpVerifyRequest request);
}
