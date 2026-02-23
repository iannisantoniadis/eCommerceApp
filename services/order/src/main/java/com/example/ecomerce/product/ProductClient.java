package com.example.ecomerce.product;

import com.example.ecomerce.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;


import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductClient {
    /*** This is the way to implement REST Template if we don't want to use OpenFeign***/

    @Value("${application.config.product-url}")
    public String productUrl;

//    private final RestTemplate restTemplate;

    private final RestClient restClient;

    public List<PurchaseResponse> purchaseProducts(List<PurchaseRequest> requestBody){
//        HttpHeaders httpHeaders = new HttpHeaders();
//        //Here you can also pass a token
//        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
//
//        HttpEntity<List<PurchaseRequest>> requestEntity = new HttpEntity<>(requestBody, httpHeaders);
//        ParameterizedTypeReference<List<PurchaseResponse>> responseType = new ParameterizedTypeReference<>() {};
//        ResponseEntity<List<PurchaseResponse>> responseEntity = restTemplate.exchange(
//                productUrl + "/purchase",
//                HttpMethod.POST,
//                requestEntity,
//                responseType);
//        if (responseEntity.getStatusCode().isError()){
//            throw new BusinessException("An error occurred while processing the product purchase: " + responseEntity.getStatusCode());
//        }
//        return responseEntity.getBody();
        List<PurchaseResponse> responseList = restClient.post()
                .uri(productUrl + "/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new BusinessException("An error occurred while processing the product purchase: " + res.getStatusCode());
        })
                .body(new ParameterizedTypeReference<List<PurchaseResponse>>() {});

        return responseList;
    }
}
