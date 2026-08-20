package com.example.ecomerce.outbox;

public interface OutboxEventTypes {
    public final String PAYMENT_SUCCESS = "Payment Success";
    public final String PAYMENT_FAILURE = "Payment Failure";
}
