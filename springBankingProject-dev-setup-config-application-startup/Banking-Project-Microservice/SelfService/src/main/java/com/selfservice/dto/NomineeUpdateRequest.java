package com.selfservice.dto;

import com.selfservice.model.Gender;
import com.selfservice.model.NomineeRelationship;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class NomineeUpdateRequest {
    private String name;

    @Min(0)
    private Integer age;

    private Gender gender;

    private NomineeRelationship relationship;

    private String email;
    private String phone;
    private String address;

    // Replace entire list when provided
    private List<String> documents;
}
