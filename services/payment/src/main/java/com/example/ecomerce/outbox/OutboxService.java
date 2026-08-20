package com.example.ecomerce.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository repository;

    private final ObjectMapper objectMapper;

    public void saveOutboxEvent (String eventType, Object payload) {
        try {
            OutboxEvent event = new OutboxEvent();
            event.setEventType(eventType);
            event.setPayload(objectMapper.writeValueAsString(payload));
            repository.save(event);
        } catch (JacksonException e) {
            throw new IllegalStateException("Could not serialize the payload"); // If payload serialization fails, the transactional
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOutboxEventInNewTransaction(String eventType, Object payload) {
        saveOutboxEvent(eventType,
                payload);
    }
}
