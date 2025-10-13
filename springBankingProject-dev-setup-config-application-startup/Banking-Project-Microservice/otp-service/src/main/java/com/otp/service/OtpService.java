package com.otp.service;

import com.otp.api.dto.GenerateOtpRequest;
import com.otp.api.dto.GenerateOtpResponse;
import com.otp.api.dto.VerifyOtpRequest;
import com.otp.api.dto.VerifyOtpResponse;

public interface OtpService {
    GenerateOtpResponse generate(GenerateOtpRequest request, String ipAddress);
    VerifyOtpResponse verify(VerifyOtpRequest request, String ipAddress);
}
