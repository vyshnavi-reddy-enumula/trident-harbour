package com.trident.harbour.exception;

/** Thrown when an operation is not allowed in the asset's current state (e.g. completing a repair never started). */
public class IllegalAssetStateException extends RuntimeException {
    public IllegalAssetStateException(String message) {
        super(message);
    }
}
