package org.cryptomator.data.dto

import com.google.gson.annotations.SerializedName
import org.cryptomator.domain.CloudType
import org.cryptomator.domain.DeviceArgs
import org.cryptomator.domain.Vault
import java.io.Serializable

data class VaultRemoteDTO(
	val id: String? = null,

	@SerializedName("device_id")
	val deviceId: String? = null,

	@SerializedName("deviceId")
	val fullDeviceId: String? = null,

	val serial: String? = null,

	@SerializedName("serialBarcode")
	val serialBarcode: String? = null,

	@SerializedName("volumeName")
	val volumeName: String? = null,

	@SerializedName("volumeDescription")
	val volumeDescription: String? = null,

	val owner: OwnerDTO? = null,

	val version: String? = null,

	@SerializedName("machineID")
	val machineID: String? = null,

	val computers: List<ComputerDTO>? = null,

	val computer: ComputerDTO? = null,

	val status: String? = null,

	@SerializedName("lastMpwdUpdated")
	val lastMpwdUpdated: Long? = null,

	@SerializedName("lastUserEventDate")
	val lastUserEventDate: Long? = null,

	@SerializedName("volumePath")
	val volumePath: String? = null,

	@SerializedName("cloudPath")
	val cloudPath: String? = null,

	@SerializedName("policy_id")
	val policyId: String? = null,

	@SerializedName("policy_etag")
	val policyEtag: String? = null,

	val policy: PolicyDTO? = null

) : Serializable {

	fun toVault(existingVault: Vault, mEtag: String): Vault {
		val v = Vault.aCopyOf(existingVault)
			.withName(volumeName ?: existingVault.name)
			.withVaultDescription(volumeDescription ?: existingVault.vaultDescription)
			.withEtag(mEtag)
			.withPolicyId(policyId ?: "")
			.withStatus(status ?: existingVault.status)
			.withUnlocked(existingVault.isUnlocked)
			.withDriveQuota(policy?.ncryptor?.driveQuota ?: existingVault.driveQuota)
			.build();

		return v;
	}

	fun toVaultWithPolicyEtag(existingVault: Vault, mEtag: String): Vault {
		val v = Vault.aCopyOf(existingVault)
			.withName(volumeName ?: existingVault.name)
			.withVaultDescription(volumeDescription ?: existingVault.vaultDescription)
			.withPolicyEtag(mEtag)
			.withPolicyId(policyId ?: "")
			.withStatus(status ?: existingVault.status)
			.withUnlocked(existingVault.isUnlocked)
			.withDriveQuota(policy?.ncryptor?.driveQuota ?: existingVault.driveQuota)
			.build();

		return v;
	}

	fun fromRemoteDto(): Vault {
		// Determine cloud type: try to use a field that matches your CloudType enum, fallback to LOCAL
		val cloudTypeString = computers?.firstOrNull()?.platform
			?: "LOCAL"
		val type = try {
			CloudType.valueOf(cloudTypeString.uppercase())
		} catch (e: Exception) {
			CloudType.LOCAL
		}

		val devideArgs = DeviceArgs.builder().build()

		// Build the Vault object with all relevant fields (only using available builder methods)
		return Vault.aVault()
			.withId(1L) // Use a valid id, or map from DTO if available
			.withName(volumeName ?: "Unnamed")
			.withPath(computers?.firstOrNull()?.volumePath ?: "/")
			.withCloudType(type)
			.withPosition(0) // Use a valid position, or map from DTO if available
			.withUnlocked(false)
			.withDeviceID(deviceId ?: "")
			.withCreatedBy(owner?.id ?: "")
			.withVaultDescription(volumeDescription ?: "")
			.withEtag("")
			.withStatus(status ?: "")
			.build()
	}

	data class OwnerDTO(
		val id: String? = null,
		val name: String? = null,
		val email: String? = null
	) : Serializable

	data class ComputerDTO(
		val hostname: String? = null,
		val serial: String? = null,
		val platform: String? = null,
		val distro: String? = null,
		val release: String? = null,
		val build: String? = null,
		val kernel: String? = null,
		val codename: String? = null,
		val arch: String? = null,
		val volumePath: String? = null,
		val cloudPath: String? = null,
		val clientVersion: String? = null,
		val logofile: String? = null,
		val fqdn: String? = null,
		val latestUseDate: Long? = null,
		val vaultStatus: String? = null
	) : Serializable

	data class PolicyDTO(
		val ncryptor: NCryptorPolicyDTO? = null
	) : Serializable

	data class NCryptorPolicyDTO(
		@SerializedName("allowOfflineUnlock")
		val allowOfflineUnlock: Boolean? = null,
		@SerializedName("isDefault")
		val isDefault: Boolean? = null,
		@SerializedName("driveQuota")
		val driveQuota: Int? = null,
		@SerializedName("restricted")
		val restricted: Boolean? = null,
		@SerializedName("autounlockcli")
		val autounlockcli: Boolean? = null,
		@SerializedName("enabled")
		val enabled: Boolean? = null,
		@SerializedName("autounlockgui")
		val autounlockgui: Boolean? = null,
		@SerializedName("driveLimit")
		val driveLimit: Int? = null
	) : Serializable

	data class OverridesDTO(
		val fblocker: FblockerDTO? = null,
		val flogger: FloggerDTO? = null
	) : Serializable

	data class FblockerDTO(
		val enabled: Boolean? = null
	) : Serializable

	data class FloggerDTO(
		val enabled: Boolean? = null
	) : Serializable
}