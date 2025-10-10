package com.creditcardservice.proxyservice;

import com.creditcardservice.dto.OtpVerifyRequest;
import com.creditcardservice.dto.OtpVerifyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "otp-service", path = "/otp")
public interface OtpServiceClient {

    @PostMapping("/verify")
    OtpVerifyResponse verify(@RequestBody OtpVerifyRequest request);
}
