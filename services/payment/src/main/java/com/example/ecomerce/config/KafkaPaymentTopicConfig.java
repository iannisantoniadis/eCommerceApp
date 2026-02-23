package com.example.ecomerce.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Service;

@Service
public class KafkaPaymentTopicConfig {

    @Value("${spring.kafka.template.default-topic}")
    private String topic;

    @Bean
    public NewTopic paymentTopic(){
        return TopicBuilder
                .name(topic)
                .build();
    }
}
