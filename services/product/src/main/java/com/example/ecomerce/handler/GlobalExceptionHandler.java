package com.example.ecomerce.handler;

import com.example.ecomerce.exception.ProductPurchaseException;
import com.example.ecomerce.exception.ProductRestoreException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductPurchaseException.class)
    public ResponseEntity<String> handle(ProductPurchaseException ex){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handle(EntityNotFoundException ex){
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomErrorResponse> handle(MethodArgumentNotValidException ex){

        var errors = new HashMap<String, String>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            var fieldName = ((FieldError) error).getField();
            var errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new CustomErrorResponse(errors));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<String> handle(OptimisticLockingFailureException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Resource was modified by another request. Please retry.\n" + System.currentTimeMillis());
    }

    @ExceptionHandler(ProductRestoreException.class)
    public ResponseEntity<String> handle(ProductRestoreException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage() + "\n" + System.currentTimeMillis());
    }
}
