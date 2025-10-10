package com.otp.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "otp_codes", indexes = {
    @Index(name = "idx_otp_user_purpose_ctx", columnList = "userId,purpose,contextId"),
    @Index(name = "idx_otp_expires_at", columnList = "expiresAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpCode {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 128)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OtpPurpose purpose;

    @Column(length = 128)
    private String contextId;

    @Column(nullable = false, length = 128)
    private String codeHash;

    @Column(nullable = false, length = 64)
    private String salt;

    // Comma-separated channels (initially EMAIL). Could be normalized later.
    @Column(nullable = false, length = 64)
    private String channels;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime consumedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(length = 64)
    private String ipAddress;

    @Lob
    private String metadata;

    public static OtpCode newInstance(String userId, OtpPurpose purpose, String contextId,
                                      String codeHash, String salt, String channels,
                                      LocalDateTime expiresAt, int maxAttempts, String ipAddress, String metadata) {
        return OtpCode.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .purpose(purpose)
                .contextId(contextId)
                .codeHash(codeHash)
                .salt(salt)
                .channels(channels)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .consumedAt(null)
                .attempts(0)
                .maxAttempts(maxAttempts)
                .ipAddress(ipAddress)
                .metadata(metadata)
                .build();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isConsumed() {
        return consumedAt != null;
    }
}
