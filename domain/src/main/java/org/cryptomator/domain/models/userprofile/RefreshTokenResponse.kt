package org.cryptomator.domain.models.userprofile

import com.google.gson.annotations.SerializedName

class RefreshTokenResponse {

	@SerializedName("AuthenticationResult")
	val authenticationResult: AuthenticationResult? = null

	class AuthenticationResult {

		@SerializedName("AccessToken")
		val accessToken: String? = null

		@SerializedName("ExpiresIn")
		val expiresIn: Int = 0

		@SerializedName("IdToken")
		val idToken: String? = null

		@SerializedName("TokenType")
		val tokenType: String? = null
	}
}