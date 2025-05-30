package org.cryptomator.domain.models.vault

data class VaultInfo(
    val lastVaultUpdated: Long,
    val vaultMK: String,
    val vaultData: String,
    val vaultPKI: String
) 