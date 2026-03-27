package com.example.ordertracker.exception;

import com.example.ordertracker.model.OrderStatus;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
