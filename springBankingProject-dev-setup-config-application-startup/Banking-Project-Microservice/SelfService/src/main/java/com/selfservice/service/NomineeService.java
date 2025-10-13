package com.selfservice.service;

import com.selfservice.dto.NomineeCreateRequest;
import com.selfservice.dto.NomineeUpdateRequest;
import com.selfservice.dto.NomineeResponse;

import java.util.List;

public interface NomineeService {
    NomineeResponse create(NomineeCreateRequest request);
    List<NomineeResponse> list(String userId);
    NomineeResponse update(String nomineeId, NomineeUpdateRequest request);
    void delete(String nomineeId, String userId);
}
