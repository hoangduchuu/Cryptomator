package org.cryptomator.data.dto.mappers

import org.cryptomator.data.dto.VaultInfoDTO
import org.cryptomator.domain.models.vault.VaultInfo

class VaultInfoMapper {
    fun toModel(dto: VaultInfoDTO): VaultInfo {
        return VaultInfo(
            lastVaultUpdated = dto.lastVaultUpdated,
            vaultMK = dto.vaultMK ?: "",
            vaultData = dto.vaultData ?: "",
            vaultPKI = dto.vaultPKI ?: "",
        )
    }
} 