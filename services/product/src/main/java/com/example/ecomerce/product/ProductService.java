package com.example.ecomerce.product;

import com.example.ecomerce.exception.ProductPurchaseException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repository;

    private final ProductMapper mapper;

    public Long createProduct(ProductRequest request) {
        return repository.save(mapper.toProduct(request)).getId();

    }

    public List<ProductPurchaseResponse> purchaseProducts(List<ProductPurchaseRequest> requestList) {
        requestList = requestList.stream().sorted(Comparator.comparingLong(ProductPurchaseRequest::productId)).toList();
        var productIds = requestList.stream().map(ProductPurchaseRequest::productId).toList();
        var storedProducts = repository.findAllByIdInOrderById(productIds);
        //Check whether we have the products ordered by the customer, send error if not, along with ids
        List<Long> missingIds = productIds.stream()
                .filter(id -> !storedProducts.stream().map(Product::getId).toList().contains(id))
                .toList();
        if (!missingIds.isEmpty()){
            throw new ProductPurchaseException("One or more products does not exist" + missingIds.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        }
        //Check whether the quantity of stuff ordered by the customer is available for purchase,
        //if so, subtract it from existing stock, otherwise send ids and quantities available as error.
        //Corner case: the customer orders the entire quantity in stock for a product -> delete product when quantity reaches 0
        Map<Long, Double> insufficientProducts = new HashMap<>();
        List<ProductPurchaseResponse> returnList = new ArrayList<>();
        for (int i = 0; i < requestList.size(); i++) {
            if (storedProducts.get(i).getAvailableQuantity() < requestList.get(i).quantity()) {
                insufficientProducts.put(requestList.get(i).productId(), requestList.get(i).quantity());
            }
        }
        if (!insufficientProducts.isEmpty()){
            throw new ProductPurchaseException("One or more product is in insufficient quantity: " +
                    insufficientProducts.entrySet().stream().map(es -> es.getKey() + ": " + es.getValue()).toList());
        }
        for (int i = 0; i < requestList.size(); i++)  {
            var difference = storedProducts.get(i).getAvailableQuantity() - requestList.get(i).quantity();
            returnList.add(mapper.toProductPurchaseResponse(storedProducts.get(i), requestList.get(i)));
            storedProducts.get(i).setAvailableQuantity(difference);

        }
        repository.saveAll(storedProducts);
        return returnList;
    }

    public ProductResponse findById(Long productId) {
        return repository.findById(productId)
                .map(mapper::toProductResponse)
                .orElseThrow(() -> new EntityNotFoundException("Product not found for id: " + productId));
    }

    public List<ProductResponse> findAll() {
        return repository.findAll().stream().map(mapper::toProductResponse).toList();
    }

    public void restoreProducts(List<ProductPurchaseRequest> requestList) {
        requestList = requestList.stream().sorted(Comparator.comparingLong(ProductPurchaseRequest::productId)).toList();

        var productsToRestore = repository.findAllByIdInOrderById(requestList.stream().map(ProductPurchaseRequest::productId).toList());
        if (requestList.size() != productsToRestore.size()){
            throw new ProductPurchaseException("The following product ids are invalid: "
                    + requestList.stream()
                    .filter(req -> !productsToRestore.stream().map(Product::getId).toList().contains(req.productId())).toList());
        }
        for (int i=0; i< productsToRestore.size(); i++){
            Product product = productsToRestore.get(i);
            product.setAvailableQuantity(product.getAvailableQuantity() + requestList.get(i).quantity());
        }
        repository.saveAll(productsToRestore);
    }
}
