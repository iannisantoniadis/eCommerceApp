package com.example.ecomerce.product;

import com.example.ecomerce.exception.ProductPurchaseException;
import com.example.ecomerce.exception.ProductRestoreException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    public List<ProductPurchaseResponse> purchaseProducts(List<ProductPurchaseRequest> requestList) {
        // Remove potential duplicates (e.g. : [{id:1, quantity:1}, {id:1, quantity;2}] -> [{id:1, quantity:3}])
        Map<Long, Double> requestedQuantityById = new LinkedHashMap<>();
        for (ProductPurchaseRequest req : requestList) {
            requestedQuantityById.merge(req.productId(), req.quantity(), Double::sum);
        }
        var productIds = new ArrayList<>(requestedQuantityById.keySet());

        // Getting stored products from DB
        var storedProducts = repository.findAllByIdInOrderById(productIds);

        // Checking for missing products
        var storedIds = storedProducts.stream().map(Product::getId).toList();
        List<Long> missingIds = productIds.stream()
                .filter(id -> !storedIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new ProductPurchaseException("One or more products does not exist: " +
                            missingIds.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        }

        // Building map for lookup
        Map<Long, Product> productById = storedProducts.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        // Checking quantity
        Map<Long, Double> insufficientProducts = new HashMap<>();
        for (var entry : requestedQuantityById.entrySet()) {
            var product = productById.get(entry.getKey());
            if (product.getAvailableQuantity() < entry.getValue()) {
                insufficientProducts.put(entry.getKey(), entry.getValue());
            }
        }

        if (!insufficientProducts.isEmpty()) {
            throw new ProductPurchaseException(
                    "One or more products are in insufficient quantity: " +
                            insufficientProducts.entrySet().stream()
                                    .map(es -> es.getKey() + ": " + es.getValue())
                                    .collect(Collectors.joining(", "))
            );
        }

        // Deduct quantities and build response
        List<ProductPurchaseResponse> returnList = new ArrayList<>();
        for (var entry : requestedQuantityById.entrySet()) {
            var product = productById.get(entry.getKey());
            var requestedQty = entry.getValue();
            var difference = product.getAvailableQuantity() - requestedQty;
            product.setAvailableQuantity(difference);

            // Synthesizing a new request with the identical products grouped by id and quantity summed up
            var aggregatedRequest = new ProductPurchaseRequest(entry.getKey(), requestedQty);
            returnList.add(mapper.toProductPurchaseResponse(product, aggregatedRequest));
        }

        repository.saveAll(new ArrayList<>(productById.values()));
        return returnList;
    }

    @Cacheable(value = "products", key = "#productId")
    public ProductResponse findById(Long productId) {
        return repository.findById(productId)
                .map(mapper::toProductResponse)
                .orElseThrow(() -> new EntityNotFoundException("Product not found for id: " + productId));
    }

    public Page<ProductResponse> findAll(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("id"))).map(mapper::toProductResponse);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    public void restoreProducts(List<ProductPurchaseRequest> requestList) {
        // Remove potential duplicates (e.g. : [{id:1, quantity:1}, {id:1, quantity;2}] -> [{id:1, quantity:3}])
        Map<Long, Double> returnedProductsMap = new LinkedHashMap<>();
        for (ProductPurchaseRequest req : requestList) {
            returnedProductsMap.merge(req.productId(), req.quantity(), Double::sum);
        }

        var productIds = new ArrayList<>(returnedProductsMap.keySet());

        // Getting stored products from DB
        var productsToRestore = repository.findAllByIdInOrderById(productIds).stream().collect(Collectors.toMap(Product::getId, product -> product));

        if (returnedProductsMap.size() != productsToRestore.size()){
            throw new ProductRestoreException("The following product ids are invalid: " +
                    returnedProductsMap.entrySet().stream().filter(item ->
                            !productsToRestore.values().stream().map(Product::getId).toList()
                                    .contains(item.getKey())).map(item -> String.valueOf(item.getKey())).collect(Collectors.joining(", ")));
        }

        for (Long id : productsToRestore.keySet()){
            var productToRestore = productsToRestore.get(id);
            productToRestore.setAvailableQuantity(productToRestore.getAvailableQuantity() + returnedProductsMap.get(id));
        }
        repository.saveAll(productsToRestore.values());
        }
}
