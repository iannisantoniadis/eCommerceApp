package com.example.ecomerce.order;

import com.example.ecomerce.orderLine.OrderLine;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name="customer_order")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reference;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private PaymentMethodEnum paymentMethod;

    @Enumerated(EnumType.STRING)
    private OrderStatusEnum status = OrderStatusEnum.PENDING;

    private String customerId;

    @OneToMany(mappedBy = "order")
    private List<OrderLine> orderLines;

    @Version
    private Long version;

    //Auditing fields

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(insertable = false, nullable = true)
    private LocalDateTime lastModifiedDate;
}
