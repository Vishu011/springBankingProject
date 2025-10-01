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
@Table(name = "ADDRESSES")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ADDRESS_ID")
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "CUSTOMER_ID", nullable = false)
  private Customer customer;

  @Column(name = "ADDRESS_LINE1", length = 255, nullable = false)
  private String addressLine1;

  @Column(name = "CITY", length = 100, nullable = false)
  private String city;

  // ACTIVE / INACTIVE to keep address history
  @Column(name = "STATUS", length = 15, nullable = false)
  private String status;

  @CreationTimestamp
  @Column(name = "CREATED_AT", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "UPDATED_AT")
  private Instant updatedAt;
}
