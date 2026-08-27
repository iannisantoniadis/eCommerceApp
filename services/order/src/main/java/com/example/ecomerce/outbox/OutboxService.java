package com.example.ecomerce.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

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
            throw new IllegalStateException("Could not serialize the payload"); // If payload serialization fails, the transactional should roll it back
        }
    }

    public List<OutboxEvent> findUnpublishedPaymentConfirmation() {
        return repository.findTop50ByPublishedFalseAndEventTypeOrderByCreatedAtAsc(OutboxEventTypes.PAYMENT_REQUEST);
    }

    public List<OutboxEvent> findUnpublishedOrdersForNotification() {
        return repository.findTop50ByPublishedFalseAndEventTypeOrderByCreatedAtAsc(OutboxEventTypes.ORDER_CONFIRMATION);
    }

    public OutboxEvent save(OutboxEvent event){
        return repository.save(event);
    }

}
