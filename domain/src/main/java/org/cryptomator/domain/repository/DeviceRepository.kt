package org.cryptomator.domain.repository

import android.util.Pair
import org.cryptomator.domain.CloudFolder
import org.cryptomator.domain.DeviceArgs
import org.cryptomator.domain.Vault
import org.cryptomator.domain.exception.BackendException
import org.cryptomator.domain.models.deployment.VaultRemote
import org.cryptomator.domain.models.device.PostVault
import org.cryptomator.util.VaultStatus

data class PollResponse(
	val needsUpdate: Boolean,
	val eTag: String? = "1",
	val body: Any?
){

}

interface DeviceRepository {

	@Throws(BackendException::class)
	fun postVault(
		vaultFolder: CloudFolder,
		deviceArgs: DeviceArgs,
		vaultDescription: String
	): Pair<String, PostVault>?

	@Throws(BackendException::class)
	fun renameVault(deviceId: String, vaultId: String, newName: String): Boolean

	@Throws(BackendException::class)
	fun getDeviceStatus(deviceId: String): VaultStatus

	@Throws(BackendException::class)
	fun postNewDeviceWithoutCode(deviceArgs: DeviceArgs, volumeName: String, vaultDescription: String, volumePath: String,cloudPath:String): PostVault

	@Throws(BackendException::class)
	fun getPollDeviceStatus(userId: String, deviceId: String, eTag: String): PollResponse

	@Throws(BackendException::class)
	fun getPollDevicePolicyId(userId: String, policyId: String, eTag: String): PollResponse

	@Throws(BackendException::class)
	fun updateVaultIfNeeded(vault: Vault): Vault

	@Throws(BackendException::class)
	fun updateVaultPolicyIdIfNeeded(vault: Vault): Vault

	@Throws(BackendException::class)
	fun updateVaultStatus(vault: Vault, newStatus: VaultStatus): Vault

	@Throws(BackendException::class)
	fun getDeploymentStatus(computerId: String): Any

	@Throws(BackendException::class)
	fun logDeviceEvent(deviceId: String, type:String, deviceArgs: DeviceArgs):Any

	@Throws(BackendException::class)
	fun getVaultRemote(deviceId: String): VaultRemote
}
