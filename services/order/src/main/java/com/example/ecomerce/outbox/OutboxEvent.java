package com.example.ecomerce.outbox;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String eventType; //PaymentSuccess or PaymentFailure

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    // Binary JSON is PostgreSQL specific, it would fail in MSSQL ,for example, in which case it should be changed to NVARCHAR(MAX)
    // - worth addressing but not an immediate issue, though noteworthy since such datatype would fail at startup or alter on other dialects
    private String payload; //The event itself

    private boolean published = false;

    private Instant createdAt = Instant.now();

}
