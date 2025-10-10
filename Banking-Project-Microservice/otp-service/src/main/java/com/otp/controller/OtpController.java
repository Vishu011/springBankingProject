package com.otp.controller;

import com.otp.api.dto.GenerateOtpRequest;
import com.otp.api.dto.GenerateOtpResponse;
import com.otp.api.dto.VerifyOtpRequest;
import com.otp.api.dto.VerifyOtpResponse;
import com.otp.service.OtpService;
import com.otp.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OTP REST endpoints for generation and verification.
 * Base path: /otp
 */
@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    @PostMapping("/generate")
    public ResponseEntity<GenerateOtpResponse> generate(@Valid @RequestBody GenerateOtpRequest request,
                                                        HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        GenerateOtpResponse resp = otpService.generate(request, ip);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    @PostMapping("/verify")
    public ResponseEntity<VerifyOtpResponse> verify(@Valid @RequestBody VerifyOtpRequest request,
                                                    HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        VerifyOtpResponse resp = otpService.verify(request, ip);
        return ResponseEntity.ok(resp);
    }

    // Public (unauthenticated) endpoints for flows like registration contact verification
    @PostMapping("/public/generate")
    public ResponseEntity<GenerateOtpResponse> generatePublic(@Valid @RequestBody GenerateOtpRequest request,
                                                              HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        GenerateOtpResponse resp = otpService.generate(request, ip);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    @PostMapping("/public/verify")
    public ResponseEntity<VerifyOtpResponse> verifyPublic(@Valid @RequestBody VerifyOtpRequest request,
                                                          HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        VerifyOtpResponse resp = otpService.verify(request, ip);
        return ResponseEntity.ok(resp);
    }
}
