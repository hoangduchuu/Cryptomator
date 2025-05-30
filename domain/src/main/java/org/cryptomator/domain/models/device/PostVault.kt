package org.cryptomator.domain.models.device

import java.io.Serializable

class PostVault(
	val jwtTokenUnlocked: String,
	val message: String,
	val deviceId: String,
	val jwtTokenLocked: String
) : Serializable {

	override fun toString(): String {
		return "PostVault(jwtTokenUnlocked='$jwtTokenUnlocked', message='$message', deviceId='$deviceId', jwtTokenLocked='$jwtTokenLocked')"
	}
}