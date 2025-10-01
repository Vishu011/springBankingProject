package com.omnibank.customerprofile.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "UPDATE_REQUESTS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "REQUEST_ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "CUSTOMER_ID", nullable = false)
  private Customer customer;

  @Column(name = "REQUEST_TYPE", length = 30, nullable = false)
  private String requestType; // e.g., ADDRESS_UPDATE

  @Column(name = "STATUS", length = 20, nullable = false)
  private String status; // PENDING_VERIFICATION, APPROVED, REJECTED

  @Lob
  @Column(name = "SUBMITTED_DATA")
  private String submittedData; // JSON blob for the requested change

  @Column(name = "APPROVED_BY", length = 100)
  private String approvedBy;

  @Column(name = "APPROVED_TS")
  private Instant approvedTs;

  @CreationTimestamp
  @Column(name = "CREATED_AT", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "UPDATED_AT")
  private Instant updatedAt;
}
