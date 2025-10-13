package com.selfservice.dto;

import com.selfservice.model.Gender;
import com.selfservice.model.NomineeRelationship;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NomineeResponse {
    private String nomineeId;
    private String userId;
    private String name;
    private Integer age;
    private Gender gender;
    private NomineeRelationship relationship;
    private String email;
    private String phone;
    private String address;
    private List<String> documents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
