package org.cryptomator.domain.models.deployment

import org.cryptomator.domain.Vault
import java.io.Serializable

data class VaultRemote(
	val id: String?,
	val deviceId: String?,
	val fullDeviceId: String?,
	val serial: String?,
	val serialBarcode: String?,
	val volumeName: String?,
	val volumeDescription: String?,
	val owner: Owner?,
	val version: String?,
	val machineID: String?,
	val computers: List<Computer>?,
	val status: String?,
	val lastMpwdUpdated: Long?,
	val lastUserEventDate: Long?,
	val computer: Computer?,
	val volumePath: String? = null,
	val cloudPath: String? = null,
	val policyId: String? = null,
	val policyEtag: String? = null,
	val policy: Policy? = null
) : Serializable {

	data class Owner(
		val id: String?,
		val name: String?,
		val email: String?
	) : Serializable

	data class Computer(
		val hostname: String?,
		val serial: String?,
		val platform: String?,
		val distro: String?,
		val release: String?,
		val build: String?,
		val kernel: String?,
		val codename: String?,
		val arch: String?,
		val volumePath: String?,
		val cloudPath: String?,
		val clientVersion: String?,
		val logofile: String?,
		val fqdn: String?,
		val latestUseDate: Long?,
		val vaultStatus: String?,
	) : Serializable

	data class Policy(
		val ncryptor: org.cryptomator.domain.models.policy.NCryptorPolicy?
	) : Serializable

	fun isSupportImportVault(): Boolean {
		return cloudPath?.startsWith("dropbox://") == true ||
				cloudPath?.startsWith("google_drive://") == true ||
				cloudPath?.startsWith("file://") == true ||
				cloudPath?.startsWith("local://") == true
	}

	// builder with ID
	fun withId(id: String?): VaultRemote {
		return this.copy(id = id)
	}

	fun toVault(): Vault {
		return Vault.aVault()
			.withName(volumeName ?: "")
			.withPath(cloudPath ?: "")
			.withUnlocked(false)
			.withPosition(0)
			.withFormat(0)
			.withShorteningThreshold(0)
			.withDeviceID(deviceId)
			.withCreatedBy(owner?.id)
			.withVaultDescription(volumeDescription)
			.withEtag("1")
			.withStatus(status)
			.withFullLocalPath(volumePath)
			.withSize(0L)
			.withPolicyId(policyId)
			.withPolicyEtag(policyEtag)
			.build();
	}
}