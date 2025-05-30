package org.cryptomator.data.dto

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class GetDeviceCodeDTO(
	val initDate: String? = null,
	val userIp: String? = null,
	val connectionUrl: String? = null,
	val activationStatus: String? = null,
	val activationCode: String? = null,
	val userSub: String? = null,
	val expirationDate: String? = null,

	@SerializedName("activationId")
	val activationID: String? = null
) : Serializable