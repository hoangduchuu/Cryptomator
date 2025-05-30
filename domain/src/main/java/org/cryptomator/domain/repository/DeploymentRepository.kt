package org.cryptomator.domain.repository

import org.cryptomator.domain.exception.BackendException
import org.cryptomator.domain.models.deployment.DeploymentStatusInfo
import org.cryptomator.domain.models.deployment.DeploymentWithStatus
import org.cryptomator.util.DeploymentStatus

interface DeploymentRepository {

	@Throws(BackendException::class)
	fun getDeploymentInfo(deviceSerial: String): List<DeploymentWithStatus>

	fun updateVaultStatus(status: DeploymentStatus, deviceSerial: String, vaultId: String)

	// New: Get lightweight status info for all deployments
	@Throws(BackendException::class)
	fun getDeploymentStatusInfo(deviceSerial: String): List<DeploymentStatusInfo>

	// New: Check if a vault exists locally
	fun vaultExistsLocally(vaultId: String): Boolean

	// New: Get full DeploymentWithStatus for a specific vault
	@Throws(BackendException::class)
	fun getDeploymentWithStatus(deviceSerial: String, vaultId: String): DeploymentWithStatus
}