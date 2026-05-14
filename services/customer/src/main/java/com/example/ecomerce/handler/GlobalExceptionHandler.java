package com.example.ecomerce.handler;

import com.example.ecomerce.exception.CustomerBusinessException;
import jakarta.ws.rs.BadRequestException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
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

    @ExceptionHandler(CustomerBusinessException.class)
    public ResponseEntity<String> handle(CustomerBusinessException ex){
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

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<String> handle(DuplicateKeyException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT).body("This email is already in use!");
        //no direct way to extract the key or the field in cause without regex, limitation of the mongoDB driver, this will suffice
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<String> handle(OptimisticLockingFailureException ex){
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Resource was modified by another request. Please retry.\n" + System.currentTimeMillis());
    }
}
