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
@Table(name = "CUSTOMERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "CUSTOMER_ID")
  private Long id;

  @Column(name = "CIF_ID", unique = true, length = 32, nullable = false)
  private String cifId;

  @Enumerated(EnumType.STRING)
  @Column(name = "STATUS", length = 15, nullable = false)
  private CustomerStatus status;

  @Column(name = "FIRST_NAME", length = 100, nullable = false)
  private String firstName;

  @Column(name = "LAST_NAME", length = 100, nullable = false)
  private String lastName;

  @Column(name = "EMAIL", length = 150, nullable = false)
  private String email;

  @Column(name = "MOBILE", length = 20, nullable = false)
  private String mobile;

  @CreationTimestamp
  @Column(name = "CREATED_AT", updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(name = "UPDATED_AT")
  private Instant updatedAt;
}
