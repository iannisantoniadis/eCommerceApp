package com.example.ecomerce.product;

import com.example.ecomerce.category.Category;
import org.springframework.stereotype.Service;

@Service
public class ProductMapper {

    public Product toProduct(ProductRequest request){
        return Product.builder()
                .price(request.price())
                .name(request.name())
                .description(request.description())
                .availableQuantity(request.availableQuantity())
                .category(
                        Category.builder()
                        .id(request.categoryId())
                        .build())
                .build();
    }

    public ProductResponse toProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getAvailableQuantity(),
                product.getPrice(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getDescription()
        );
    }

    public ProductPurchaseResponse toProductPurchaseResponse(Product prod, ProductPurchaseRequest request){
        return new ProductPurchaseResponse(
                request.productId(),
                prod.getName(),
                prod.getDescription(),
                prod.getPrice(),
                request.quantity()
        );
    }
}
