package com.example.ecomerce.processedEvent;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "processed_event", uniqueConstraints = {
        @UniqueConstraint(name = "uk_processed_event_order_type", columnNames = {"orderId", "eventType"})
})
public class ProcessedEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String eventType; //PaymentSuccess or PaymentFailure

    @Column(unique = true)
    private Long orderId;

    private Instant createdAt = Instant.now();
}
