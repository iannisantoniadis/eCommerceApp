package com.example.ecomerce.product;

import com.example.ecomerce.category.Category;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private Double availableQuantity;

    private BigDecimal price;

    @Version
    private Long version;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
