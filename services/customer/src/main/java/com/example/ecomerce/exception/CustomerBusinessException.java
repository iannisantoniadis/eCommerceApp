package com.example.ecomerce.exception;

public class CustomerBusinessException extends RuntimeException {
    public CustomerBusinessException(String message) {
        super(message); // This passes the string to RuntimeException
    }
}
