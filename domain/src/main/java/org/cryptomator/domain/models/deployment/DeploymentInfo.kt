package org.cryptomator.domain.models.deployment

import org.cryptomator.domain.models.vault.VaultInfo
import java.io.Serializable

data class DeploymentInfo(
    val vaultRemote: VaultRemote?,
    val vaultInfo: VaultInfo?
) : Serializable {
    companion object {
        fun minimal(vaultId: String): DeploymentInfo {
            // Create a minimal VaultRemote with just the id
            val remote = VaultRemote(
                id = null,
                deviceId = vaultId,
                fullDeviceId = vaultId,
                serial = null,
                serialBarcode = null,
                volumeName = null,
                volumeDescription = null,
                owner = null,
                version = null,
                machineID = null,
                computers = null,
                status = null,
                lastMpwdUpdated = null,
                lastUserEventDate = null,
                computer = null,
                volumePath = null,
                cloudPath = null
            )
            return DeploymentInfo(remote, null)
        }
    }
}