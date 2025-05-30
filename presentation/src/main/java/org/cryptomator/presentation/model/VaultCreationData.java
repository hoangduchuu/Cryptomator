package org.cryptomator.presentation.model;

import java.io.Serializable;

public class VaultCreationData implements Serializable {
    private final String name;
    private final String description;

    public VaultCreationData(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
} 