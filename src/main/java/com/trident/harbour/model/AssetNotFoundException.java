// AssetNotFoundException.java
package com.trident.harbour.model;

public class AssetNotFoundException extends HarbourGraphException {
    public AssetNotFoundException(int assetId) {
        super("Unknown asset: " + assetId);
    }
}