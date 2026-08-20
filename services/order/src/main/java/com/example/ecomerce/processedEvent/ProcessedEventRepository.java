package com.example.ecomerce.processedEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByOrderIdAndEventType(Long orderId, String eventType);

    Optional<ProcessedEvent> findByOrderIdAndEventType(Long orderId, String eventType);
}
