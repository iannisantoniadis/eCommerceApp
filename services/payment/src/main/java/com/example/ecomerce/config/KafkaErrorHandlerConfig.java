package com.example.ecomerce.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler errorHandler() {
        return new DefaultErrorHandler((record, exception) -> {
            log.error("Kafka consumer error on topic {}, partition {}, offset {}: {}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    exception.getMessage());
        }, new FixedBackOff(1000L, 3L)); // retry 3 times, 1s apart
    }
}