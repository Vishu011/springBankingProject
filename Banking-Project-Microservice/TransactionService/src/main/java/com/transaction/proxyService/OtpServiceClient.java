package com.transaction.proxyService;

import com.transaction.config.FeignClientConfiguration;
import com.transaction.dto.OtpVerifyRequest;
import com.transaction.dto.OtpVerifyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "otp-service", path = "/otp", configuration = FeignClientConfiguration.class)
public interface OtpServiceClient {

    @PostMapping("/verify")
    OtpVerifyResponse verify(@RequestBody OtpVerifyRequest request);
}
