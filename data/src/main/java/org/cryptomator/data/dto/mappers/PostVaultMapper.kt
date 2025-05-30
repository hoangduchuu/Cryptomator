package org.cryptomator.data.dto.mappers

import org.cryptomator.data.dto.PostVaultDTO
import org.cryptomator.domain.models.device.PostVault

class PostVaultMapper {

	fun toDto(
		jwtTokenUnlocked: String,
		message: String,
		deviceId: String,
		jwtTokenLocked: String
	): PostVaultDTO {
		return PostVaultDTO(
			jwt_token_unlocked = jwtTokenUnlocked,
			message = message,
			deviceId = deviceId,
			jwt_token_locked = jwtTokenLocked
		)
	}

	fun toModel(dto: PostVaultDTO): PostVault {
		return PostVault(
			jwtTokenUnlocked = dto.jwt_token_unlocked,
			message = dto.message,
			deviceId = dto.deviceId,
			jwtTokenLocked = dto.jwt_token_locked
		)
	}

}