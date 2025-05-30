package org.cryptomator.domain.models.deployment;

import org.cryptomator.util.DeploymentStatus;

public class DeploymentStatusInfo {
    private final String vaultId;
    private final DeploymentStatus status;

    public DeploymentStatusInfo(String vaultId, DeploymentStatus status) {
        this.vaultId = vaultId;
        this.status = status;
    }

    public String getVaultId() {
        return vaultId;
    }

    public DeploymentStatus getStatus() {
        return status;
    }
} 