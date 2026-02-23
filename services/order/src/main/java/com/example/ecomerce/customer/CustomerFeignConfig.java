package com.example.ecomerce.customer;

import org.springframework.context.annotation.Bean;

public class CustomerFeignConfig {

    @Bean
    public CustomerFeignErrorDecoder customerFeignErrorDecoder(){
        return new CustomerFeignErrorDecoder();
    }
}
