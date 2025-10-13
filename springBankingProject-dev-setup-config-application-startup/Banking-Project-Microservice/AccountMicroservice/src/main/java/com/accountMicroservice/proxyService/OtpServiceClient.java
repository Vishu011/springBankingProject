package com.accountMicroservice.proxyService;

import com.accountMicroservice.config.FeignClientConfiguration;
import com.accountMicroservice.dto.OtpVerifyRequest;
import com.accountMicroservice.dto.OtpVerifyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "otp-service", path = "/otp", configuration = FeignClientConfiguration.class)
public interface OtpServiceClient {

    @PostMapping("/verify")
    OtpVerifyResponse verify(@RequestBody OtpVerifyRequest request);

    // Public OTP verification for corporate email flows (no auth required on OTP service)
    @PostMapping("/public/verify")
    OtpVerifyResponse verifyPublic(@RequestBody OtpVerifyRequest request);
}
