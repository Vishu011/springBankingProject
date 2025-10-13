package com.selfservice.controller;

import com.selfservice.dto.NomineeCreateRequest;
import com.selfservice.dto.NomineeResponse;
import com.selfservice.dto.NomineeUpdateRequest;
import com.selfservice.service.NomineeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/self-service/nominees", "/nominees"})
@RequiredArgsConstructor
public class NomineeController {

    private final NomineeService nomineeService;

    // Create nominee (authenticated user)
    @PostMapping
    @PreAuthorize("isAuthenticated() and #request.userId == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<NomineeResponse> create(@Valid @RequestBody NomineeCreateRequest request) {
        NomineeResponse resp = nomineeService.create(request);
        return new ResponseEntity<>(resp, HttpStatus.CREATED);
    }

    // List nominees for a user
    @GetMapping
    @PreAuthorize("isAuthenticated() and #userId == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<List<NomineeResponse>> list(@RequestParam("userId") String userId) {
        return ResponseEntity.ok(nomineeService.list(userId));
    }

    // Update nominee
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // Service will enforce ownership as needed
    public ResponseEntity<NomineeResponse> update(@PathVariable("id") String id,
                                                  @Valid @RequestBody NomineeUpdateRequest request) {
        NomineeResponse resp = nomineeService.update(id, request);
        return ResponseEntity.ok(resp);
    }

    // Delete nominee
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and #userId == authentication.name or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable("id") String id, @RequestParam("userId") String userId) {
        nomineeService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }
}
