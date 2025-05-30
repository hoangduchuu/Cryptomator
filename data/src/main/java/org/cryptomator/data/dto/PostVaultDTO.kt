package org.cryptomator.data.dto

import java.io.Serializable

class PostVaultDTO(
	val jwt_token_unlocked: String,
	val message: String,
	val deviceId: String,
	val jwt_token_locked: String
) : Serializable {

	override fun toString(): String {
		return "PostVaultDTO(jwt_token_unlocked='$jwt_token_unlocked', message='$message', deviceId='$deviceId', jwt_token_locked='$jwt_token_locked')"
	}
}