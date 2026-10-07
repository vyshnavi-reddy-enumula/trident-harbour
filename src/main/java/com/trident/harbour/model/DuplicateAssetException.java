// DuplicateAssetException.java
package com.trident.harbour.model;

public class DuplicateAssetException extends HarbourGraphException {
    public DuplicateAssetException(int assetId) {
        super("Asset already exists: " + assetId);
    }
}