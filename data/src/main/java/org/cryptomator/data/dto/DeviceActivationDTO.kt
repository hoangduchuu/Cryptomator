package org.cryptomator.data.dto

import com.google.gson.annotations.SerializedName
import java.io.Serializable

class DeviceActivationDTO(
	@SerializedName("jwt_token_unlocked")
	val jwtTokenUnlocked: String,
	val message: String,
	val deviceId: String,
	@SerializedName("jwt_token_locked")
	val jwtTokenLocked: String
) : Serializable 