package com.trident.harbour.exception;

/** Thrown when an asset is built with invalid data (bad criticality, blank name, ...). */
public class InvalidAssetException extends RuntimeException {
    public InvalidAssetException(String message) {
        super(message);
    }
}
