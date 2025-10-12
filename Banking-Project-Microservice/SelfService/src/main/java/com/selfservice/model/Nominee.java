package com.selfservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "self_nominee", indexes = {
        @Index(name = "idx_self_nominee_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nominee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "nominee_id", updatable = false, nullable = false)
    private String nomineeId;

    @NotBlank
    @Column(name = "user_id", nullable = false)
    private String userId;

    @NotBlank
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @NotNull
    @Min(0)
    @Column(name = "age", nullable = false)
    private Integer age;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 16)
    private Gender gender;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "relationship", nullable = false, length = 32)
    private NomineeRelationship relationship;

    @Column(name = "email", length = 160)
    private String email; // optional

    @Column(name = "phone", length = 32)
    private String phone; // optional

    @Column(name = "address", length = 512)
    private String address; // optional

    // Optional: store supporting docs for nominee if needed later
    @ElementCollection
    private List<String> documents;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static Nominee nowTimestamps(Nominee n) {
        LocalDateTime now = LocalDateTime.now();
        n.setCreatedAt(now);
        n.setUpdatedAt(now);
        return n;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
