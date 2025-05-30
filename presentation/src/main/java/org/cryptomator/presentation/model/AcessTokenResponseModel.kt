import com.google.gson.annotations.SerializedName

/**
 * Model class representing the data contained in the access token response JWT
 * using annotations for JSON serialization/deserialization
 */
data class AccessTokenResponseModel(
	// User Identification
	val sub: String? = null,
	val username: String? = null,

	// Authentication & Token Info
	@SerializedName("iss")
	val issuer: String? = null,

	val version: Int = 0,

	@SerializedName("client_id")
	val clientId: String? = null,

	@SerializedName("origin_jti")
	val originJti: String? = null,

	@SerializedName("token_use")
	val tokenUse: String? = null,

	val scope: String? = null,

	@SerializedName("auth_time")
	val authTimeSeconds: Long = 0,

	@SerializedName("exp")
	val expirationSeconds: Long = 0,

	@SerializedName("iat")
	val issuedAtSeconds: Long = 0,

	val jti: String? = null
) {
	companion object {
		/**
		 * Parse JWT token and create an AccessTokenResponseModel using Gson
		 */
		@JvmStatic
		fun parseJwt(token: String): AccessTokenResponseModel {
			// Split the token to get the payload
			val parts = token.split(".")
			if (parts.size != 3) {
				throw IllegalArgumentException("Invalid JWT token format")
			}

			// Decode the Base64Url-encoded payload
			val payload = decodeBase64Url(parts[1])

			// Use Gson to parse the JSON to AccessTokenResponseModel
			val gson = com.google.gson.Gson()
			return gson.fromJson(payload, AccessTokenResponseModel::class.java)
		}

		/**
		 * Create AccessTokenResponseModel directly from JWT payload JSON
		 */
		@JvmStatic
		fun fromJson(jsonPayload: String): AccessTokenResponseModel {
			val gson = com.google.gson.Gson()
			return gson.fromJson(jsonPayload, AccessTokenResponseModel::class.java)
		}

		/**
		 * Decode Base64Url string to a JSON string
		 */
		private fun decodeBase64Url(input: String): String {
			// Replace URL-safe characters
			val base64 = input.replace("-", "+").replace("_", "/")

			// Add padding if needed
			val padding = when (base64.length % 4) {
				0 -> ""
				2 -> "=="
				3 -> "="
				else -> ""
			}

			// Decode
			val decodedBytes = java.util.Base64.getDecoder().decode(base64 + padding)
			return String(decodedBytes, Charsets.UTF_8)
		}
	}
}