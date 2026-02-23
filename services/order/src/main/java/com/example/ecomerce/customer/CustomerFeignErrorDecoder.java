package com.example.ecomerce.customer;

import com.example.ecomerce.exception.BusinessException;
import feign.Response;
import feign.codec.ErrorDecoder;
import jakarta.ws.rs.BadRequestException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CustomerFeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = "Error reading customer body!";
        try {
            if (response.body() != null) {
                message = feign.Util.toString(response.body().asReader(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new BusinessException(message);
        }
        return switch (response.status()) {
            case 404 -> new BusinessException(message);
            case 400 -> new BadRequestException(message);
            default -> defaultDecoder.decode(methodKey, response);
        };
    }
}
