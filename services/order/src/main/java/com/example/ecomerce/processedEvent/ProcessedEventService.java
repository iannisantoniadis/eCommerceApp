package com.example.ecomerce.processedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository repository;

    public boolean alreadyProcessed(Long orderId, String eventType) {
        return repository.existsByOrderIdAndEventType(orderId, eventType);
    }

    public void markProcessed(Long orderId, String eventType) {
        var event = ProcessedEvent.builder()
                .eventType(eventType)
                .orderId(orderId)
                .createdAt(Instant.now())
                .build();
        repository.save(event);
    }
}
