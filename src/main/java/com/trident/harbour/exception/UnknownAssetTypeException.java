package com.trident.harbour.exception;

/** Thrown by AssetFactory when the database 'type' value matches no known AssetType. */
public class UnknownAssetTypeException extends RuntimeException {
    public UnknownAssetTypeException(String type) {
        super("Unknown asset type: '" + type + "'");
    }
}
