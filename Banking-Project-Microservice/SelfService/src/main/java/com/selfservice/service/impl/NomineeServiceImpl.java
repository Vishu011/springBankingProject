package com.selfservice.service.impl;

import com.selfservice.dao.NomineeRepository;
import com.selfservice.dto.NomineeCreateRequest;
import com.selfservice.dto.NomineeResponse;
import com.selfservice.dto.NomineeUpdateRequest;
import com.selfservice.model.Nominee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.selfservice.proxy.NotificationServiceClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NomineeServiceImpl implements com.selfservice.service.NomineeService {

    private final NomineeRepository nomineeRepository;
    private final NotificationServiceClient notificationClient;

    @Override
    @Transactional
    public NomineeResponse create(NomineeCreateRequest request) {
        // Basic validations (JPA/Bean Validation will also enforce)
        if (request.getDocuments() == null) {
            // Allow empty list by default; documents are optional for nominees
        }

        Nominee nominee = Nominee.builder()
                .userId(request.getUserId())
                .name(request.getName())
                .age(request.getAge())
                .gender(request.getGender())
                .relationship(request.getRelationship())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .documents(request.getDocuments())
                .build();

        nominee = Nominee.nowTimestamps(nominee);
        nominee = nomineeRepository.save(nominee);
        try {
            notificationClient.sendEmailNotification(new com.selfservice.dto.NotificationRequest(
                    nominee.getUserId(),
                    "EMAIL",
                    "Nominee " + nominee.getName() + " has been added to your profile.",
                    null
            ));
        } catch (Exception ignore) {}
        return toResponse(nominee);
    }

    @Override
    public List<NomineeResponse> list(String userId) {
        return nomineeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NomineeResponse update(String nomineeId, NomineeUpdateRequest request) {
        Nominee nominee = nomineeRepository.findById(nomineeId)
                .orElseThrow(() -> new IllegalArgumentException("Nominee not found: " + nomineeId));

        if (request.getName() != null && !request.getName().isBlank()) nominee.setName(request.getName());
        if (request.getAge() != null) nominee.setAge(request.getAge());
        if (request.getGender() != null) nominee.setGender(request.getGender());
        if (request.getRelationship() != null) nominee.setRelationship(request.getRelationship());
        if (request.getEmail() != null) nominee.setEmail(request.getEmail());
        if (request.getPhone() != null) nominee.setPhone(request.getPhone());
        if (request.getAddress() != null) nominee.setAddress(request.getAddress());
        if (request.getDocuments() != null) nominee.setDocuments(request.getDocuments());

        nominee.touch();
        nominee = nomineeRepository.save(nominee);
        try {
            notificationClient.sendEmailNotification(new com.selfservice.dto.NotificationRequest(
                    nominee.getUserId(),
                    "EMAIL",
                    "Nominee " + nominee.getName() + " has been updated in your profile.",
                    null
            ));
        } catch (Exception ignore) {}
        return toResponse(nominee);
    }

    @Override
    @Transactional
    public void delete(String nomineeId, String userId) {
        Nominee nominee = nomineeRepository.findById(nomineeId)
                .orElseThrow(() -> new IllegalArgumentException("Nominee not found: " + nomineeId));
        if (!nominee.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Nominee does not belong to user");
        }
        nomineeRepository.deleteById(nomineeId);
        try {
            notificationClient.sendEmailNotification(new com.selfservice.dto.NotificationRequest(
                    nominee.getUserId(),
                    "EMAIL",
                    "Nominee " + nominee.getName() + " has been removed from your profile.",
                    null
            ));
        } catch (Exception ignore) {}
    }

    private NomineeResponse toResponse(Nominee n) {
        return NomineeResponse.builder()
                .nomineeId(n.getNomineeId())
                .userId(n.getUserId())
                .name(n.getName())
                .age(n.getAge())
                .gender(n.getGender())
                .relationship(n.getRelationship())
                .email(n.getEmail())
                .phone(n.getPhone())
                .address(n.getAddress())
                .documents(n.getDocuments())
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .build();
    }
}
