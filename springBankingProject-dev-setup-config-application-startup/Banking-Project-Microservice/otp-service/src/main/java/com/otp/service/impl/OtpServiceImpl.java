package com.otp.service.impl;

import com.otp.api.dto.GenerateOtpRequest;
import com.otp.api.dto.GenerateOtpResponse;
import com.otp.api.dto.VerifyOtpRequest;
import com.otp.api.dto.VerifyOtpResponse;
import com.otp.client.NotificationClient;
import com.otp.client.dto.NotificationRequest;
import com.otp.config.OtpProperties;
import com.otp.domain.OtpCode;
import com.otp.domain.OtpPurpose;
import com.otp.repository.OtpCodeRepository;
import com.otp.service.OtpService;
import com.otp.util.CryptoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {
    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    private final OtpCodeRepository otpCodeRepository;
    private final OtpProperties otpProperties;
    private final NotificationClient notificationClient;
    private final MeterRegistry meterRegistry;

    @Override
    @Transactional
    public GenerateOtpResponse generate(GenerateOtpRequest request, String ipAddress) {
        String userId = request.getUserId();
        OtpPurpose purpose = parsePurpose(request.getPurpose());
        String contextId = nullIfBlank(request.getContextId());
        meterRegistry.counter("otp_generate_requests_total", "purpose", purpose.name()).increment();

        // Rate limiting: at most N per window per user+purpose
        int windowSeconds = otpProperties.getRateLimit().getWindowSeconds();
        int maxRequests = otpProperties.getRateLimit().getMaxRequestsPerWindow();
        LocalDateTime windowStart = LocalDateTime.now().minusSeconds(windowSeconds);
        long recent = otpCodeRepository.countByUserIdAndPurposeAndCreatedAtAfter(userId, purpose, windowStart);
        if (recent >= maxRequests) {
            meterRegistry.counter("otp_generate_rate_limited_total", "purpose", purpose.name()).increment();
            log.warn("OTP generate rate-limited user={} purpose={} window={}s", safeUser(userId), purpose, windowSeconds);
            throw new IllegalStateException("Too many OTP requests. Please try again later.");
        }

        // TTL
        Integer ttlOverride = request.getTtlSeconds();
        int ttlSeconds = ttlOverride != null ? ttlOverride : otpProperties.ttlForPurpose(toPurposeKey(purpose));
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ttlSeconds);

        // Generate secure OTP
        int codeLength = otpProperties.getCodeLength();
        String code = CryptoUtils.randomNumericCode(codeLength);
        String salt = CryptoUtils.randomSaltHex(16);
        String hash = CryptoUtils.sha256Hex(salt + ":" + code);

        // Channels default
        List<String> channels = request.getChannels();
        String channelCsv = (channels == null || channels.isEmpty()) ? "EMAIL" : String.join(",", channels);

        // Persist
        OtpCode otp = OtpCode.newInstance(
                userId,
                purpose,
                contextId,
                hash,
                salt,
                channelCsv,
                expiresAt,
                otpProperties.getMaxAttempts(),
                ipAddress,
                null
        );
        otp = otpCodeRepository.save(otp);

        // Send email via NotificationService (best-effort, do not expose OTP in logs)
        if (channelCsv.contains("EMAIL")) {
            String content = String.format(
                    "Your OTP is %s. It is valid for %d minute(s) for %s. If you did not request this, please contact support.",
                    code, Math.max(1, ttlSeconds / 60), purpose.name().replace('_', ' ').toLowerCase()
            );
            try {
                NotificationRequest nr = new NotificationRequest();
                nr.setUserId(userId);
                nr.setType("EMAIL");
                nr.setContent(content);
                // For public CONTACT_VERIFICATION flow the frontend sends userId = email.
                // Only use direct toEmail when userId looks like an email to avoid breaking secured flows.
                if (userId != null && userId.contains("@")) {
                    nr.setToEmail(userId);
                }
                notificationClient.sendEmail(nr);
            } catch (Exception e) {
                // Log minimal info (avoid printing code). Continue regardless.
                meterRegistry.counter("otp_notification_failures_total", "purpose", purpose.name()).increment();
                log.warn("OTP email dispatch failed user={} purpose={} err={}", safeUser(userId), purpose, e.getMessage());
            }
        }

        meterRegistry.counter("otp_generate_success_total", "purpose", purpose.name()).increment();
        return new GenerateOtpResponse(otp.getId(), expiresAt);
    }

    @Override
    @Transactional
    public VerifyOtpResponse verify(VerifyOtpRequest request, String ipAddress) {
        String userId = request.getUserId();
        OtpPurpose purpose = parsePurpose(request.getPurpose());
        String contextId = nullIfBlank(request.getContextId());
        String code = request.getCode();
        meterRegistry.counter("otp_verify_requests_total", "purpose", purpose.name()).increment();

        var optOtp = (contextId != null)
                ? otpCodeRepository.findTopByUserIdAndPurposeAndContextIdAndConsumedAtIsNullOrderByCreatedAtDesc(userId, purpose, contextId)
                : otpCodeRepository.findTopByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(userId, purpose);

        if (optOtp.isEmpty()) {
            meterRegistry.counter("otp_verify_failed_total", "purpose", purpose.name(), "reason", "not_found").increment();
            log.info("OTP verify failure: not_found user={} purpose={}", safeUser(userId), purpose);
            return new VerifyOtpResponse(false, null, null, null, "No active OTP found for verification");
        }

        OtpCode otp = optOtp.get();

        if (otp.isConsumed()) {
            meterRegistry.counter("otp_verify_failed_total", "purpose", purpose.name(), "reason", "used").increment();
            log.info("OTP verify failure: used user={} purpose={} otpId={}", safeUser(userId), purpose, otp.getId());
            return new VerifyOtpResponse(false, otp.getId(), null, null, "OTP already used");
        }

        if (otp.isExpired()) {
            meterRegistry.counter("otp_verify_failed_total", "purpose", purpose.name(), "reason", "expired").increment();
            log.info("OTP verify failure: expired user={} purpose={} otpId={}", safeUser(userId), purpose, otp.getId());
            return new VerifyOtpResponse(false, otp.getId(), null, otp.getMaxAttempts() - otp.getAttempts(), "OTP expired");
        }

        String computed = CryptoUtils.sha256Hex(otp.getSalt() + ":" + code);
        if (computed.equalsIgnoreCase(otp.getCodeHash())) {
            otp.setConsumedAt(LocalDateTime.now());
            otpCodeRepository.save(otp);
            meterRegistry.counter("otp_verify_success_total", "purpose", purpose.name()).increment();
            log.info("OTP verify success user={} purpose={} otpId={}", safeUser(userId), purpose, otp.getId());
            return new VerifyOtpResponse(true, otp.getId(), LocalDateTime.now(), null, "OTP verified successfully");
        } else {
            int attempts = otp.getAttempts() + 1;
            otp.setAttempts(attempts);
            if (attempts >= otp.getMaxAttempts()) {
                otp.setConsumedAt(LocalDateTime.now()); // lock/consume after max attempts
            }
            otpCodeRepository.save(otp);
            int remaining = Math.max(0, otp.getMaxAttempts() - attempts);
            String reason = remaining == 0 ? "max_attempts" : "invalid";
            meterRegistry.counter("otp_verify_failed_total", "purpose", purpose.name(), "reason", reason).increment();
            log.info("OTP verify failure: {} user={} purpose={} otpId={} remaining={}", reason, safeUser(userId), purpose, otp.getId(), remaining);
            return new VerifyOtpResponse(false, otp.getId(), null, remaining, remaining == 0 ? "Maximum verification attempts exceeded" : "Invalid OTP code");
        }
    }

    private OtpPurpose parsePurpose(String s) {
        if (s == null) throw new IllegalArgumentException("purpose is required");
        String norm = s.trim().replace('-', '_').toUpperCase();
        return OtpPurpose.valueOf(norm);
    }

    private String toPurposeKey(OtpPurpose p) {
        return p.name().toLowerCase().replace('_', '-');
    }

    private String nullIfBlank(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private String safeUser(String userId) {
        if (userId == null) return "null";
        if (userId.length() <= 2) return "***";
        return userId.substring(0, 1) + "***" + userId.substring(userId.length() - 1);
    }
}
