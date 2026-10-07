package com.trident.harbour.exception;

/** Thrown when a health score outside 0..100 (or NaN) is supplied. */
public class InvalidHealthException extends RuntimeException {
    public InvalidHealthException(double value) {
        super("Health score must be between 0 and 100 but was " + value);
    }
}
