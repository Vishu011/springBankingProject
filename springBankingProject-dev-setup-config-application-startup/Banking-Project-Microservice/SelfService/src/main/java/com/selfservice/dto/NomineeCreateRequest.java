package com.selfservice.dto;

import com.selfservice.model.Gender;
import com.selfservice.model.NomineeRelationship;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class NomineeCreateRequest {
    @NotBlank
    private String userId;

    @NotBlank
    private String name;

    @NotNull
    @Min(0)
    private Integer age;

    @NotNull
    private Gender gender;

    @NotNull
    private NomineeRelationship relationship;

    private String email;
    private String phone;
    private String address;

    // optional list of previously uploaded document relative paths
    private List<String> documents;
}
