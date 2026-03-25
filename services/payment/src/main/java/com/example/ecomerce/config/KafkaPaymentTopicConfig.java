package com.example.ecomerce.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Service;

@Service
public class KafkaPaymentTopicConfig {

    @Value("${spring.kafka.template.payment-topic}")
    private String paymentTopic;

    @Value("${spring.kafka.template.payment-success-topic}")
    private String paymentSuccessTopic;

    @Value("${spring.kafka.template.payment-failure-topic}")
    private String paymentFailureTopic;

    @Bean
    public NewTopic paymentTopic(){
        return TopicBuilder
                .name(paymentTopic)
                .build();
    }

    @Bean
    public NewTopic paymentSuccessTopic(){
        return TopicBuilder
                .name(paymentSuccessTopic)
                .build();
    }

    @Bean
    public NewTopic paymentFailureTopic(){
        return TopicBuilder
                .name(paymentFailureTopic)
                .build();
    }
}
