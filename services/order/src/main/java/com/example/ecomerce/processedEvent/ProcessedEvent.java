package com.example.ecomerce.processedEvent;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_event")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ProcessedEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String eventType; //PaymentSuccess or PaymentFailure

    @Column(unique = true)
    private Long orderId;

    private Instant createdAt = Instant.now();
}
