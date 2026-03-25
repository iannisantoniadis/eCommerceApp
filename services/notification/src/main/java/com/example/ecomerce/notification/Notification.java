package com.example.ecomerce.notification;

import com.example.ecomerce.kafka.order.OrderConfirmation;
import com.example.ecomerce.kafka.payment.PaymentEvent;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private NotificationTypeEnum notificationType;

    private LocalDateTime notificationDate;

    private OrderConfirmation orderConfirmation;

    private PaymentEvent paymentEvent;


}
