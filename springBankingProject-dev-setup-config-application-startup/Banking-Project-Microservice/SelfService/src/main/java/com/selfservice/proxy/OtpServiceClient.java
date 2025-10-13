package com.selfservice.proxy;

import com.selfservice.config.FeignAuthConfig;
import com.selfservice.proxy.dto.OtpVerifyRequest;
import com.selfservice.proxy.dto.OtpVerifyResponse;
import com.selfservice.proxy.dto.OtpGenerateRequest;
import com.selfservice.proxy.dto.OtpGenerateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "otp-service", path = "/otp", configuration = FeignAuthConfig.class)
public interface OtpServiceClient {

    // Authenticated generate (uses Authorization header forwarded by FeignAuthConfig)
    @PostMapping("/generate")
    OtpGenerateResponse generate(@RequestBody OtpGenerateRequest request);

    // Public generate for new email verification
    @PostMapping("/public/generate")
    OtpGenerateResponse generatePublic(@RequestBody OtpGenerateRequest request);

    // Authenticated verify
    @PostMapping("/verify")
    OtpVerifyResponse verify(@RequestBody OtpVerifyRequest request);

    // Public verify for new email verification
    @PostMapping("/public/verify")
    OtpVerifyResponse verifyPublic(@RequestBody OtpVerifyRequest request);
}
