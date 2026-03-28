package com.example.inventory.repository.inventory.exception;

public class InvalidReserveAmountException extends RuntimeException {
    public InvalidReserveAmountException(String message) {
        super(message);
    }
}
