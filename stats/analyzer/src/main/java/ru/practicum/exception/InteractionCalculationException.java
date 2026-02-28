package ru.practicum.exception;

public class InteractionCalculationException extends RuntimeException {

    public InteractionCalculationException(String message) {
        super(message);
    }

    public InteractionCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}