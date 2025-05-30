package org.cryptomator.presentation.model

import org.cryptomator.domain.Vault
import org.cryptomator.util.VaultStatus
import org.cryptomator.util.VaultStatusParser
import org.cryptomator.util.crypto.CryptoMode
import java.io.Serializable

class VaultModel(private val vault: Vault) : Serializable {

	val vaultId: Long
		get() = vault.id
	val name: String
		get() = vault.name
	val path: String
		get() = vault.path
	val isLocked: Boolean
		get() = !vault.isUnlocked
	val position: Int
		get() = vault.position
	val format: Int
		get() = vault.format
	val shorteningThreshold: Int
		get() = vault.shorteningThreshold
	val status: String?
		get() = vault.status
	val size: Long
		get() = vault.size
	val policyId: String?
		get() = vault.policyId
	val driveQuota: Int?
		get() = vault.driveQuota
	val policyEtag: String?
		get() = vault.policyEtag

	val fullLocalPath: String?
		get() = vault.fullLocalPath


	fun getDisplayStatus(): String {
		val vaultStatus = VaultStatusParser.parse(status)
		return when (vaultStatus) {
			VaultStatus.DisablePending -> "Disabled"
			VaultStatus.Disabled -> "Disabled"
			VaultStatus.Reset -> "Reset"
			VaultStatus.ResetPending -> "Reset"
			VaultStatus.InUse -> "In Use"
			VaultStatus.ReadonlyPending -> "Readonly"
			VaultStatus.ReadOnly -> "Readonly"
			else -> "Unknown"
		}.uppercase()
	}

	fun getDisPlayPath():String{
		return if (!fullLocalPath.isNullOrEmpty()) {
			"$fullLocalPath"
		} else {
			"$path"
		}
	}

	fun toVault(): Vault {
		return vault
	}

	fun getVaultStatus(): VaultStatus {
		return if (status != null) {
			return VaultStatusParser.parse(status)
		} else {
			VaultStatus.Unknown
		}
	}

	fun shouldGetVaultRemote(): Boolean {
		return vault.policyEtag == "1" || vault.policyEtag == null || vault.policyEtag.isEmpty()
	}

	val cloudType: CloudTypeModel
		get() = CloudTypeModel.valueOf(vault.cloudType)
	val password: String?
		get() = vault.password
	val passwordCryptoMode: CryptoMode?
		get() = vault.passwordCryptoMode

	val createdBy: String?
		get() = vault.createdBy

	override fun equals(other: Any?): Boolean {
		return vault == (other as VaultModel).toVault()
	}

	override fun hashCode(): Int {
		return vault.hashCode()
	}
}
