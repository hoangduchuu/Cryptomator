import com.google.gson.annotations.SerializedName

/**
 * Model class representing the data contained in the login response JWT
 * using annotations for JSON serialization/deserialization
 */
data class IdTokenResponseModel(
	// User Identification
	val sub: String? = null,

	@SerializedName("cognito:username")
	val cognitoUsername: String? = null,

	@SerializedName("given_name")
	val givenName: String? = null,

	@SerializedName("family_name")
	val familyName: String? = null,

	val email: String? = null,

	@SerializedName("email_verified")
	val emailVerified: Boolean = false,

	// Profile and Media
	val picture: String? = null,

	@SerializedName("custom:picture")
	val customPicture: String? = null,

	// Authentication & Token Info
	@SerializedName("at_hash")
	val atHash: String? = null,

	@SerializedName("iss")
	val issuer: String? = null,

	@SerializedName("aud")
	val audience: String? = null,

	@SerializedName("token_use")
	val tokenUse: String? = null,

	@SerializedName("auth_time")
	val authTimeSeconds: Long = 0,

	@SerializedName("iat")
	val issuedAtSeconds: Long = 0,

	@SerializedName("exp")
	val expirationSeconds: Long = 0,

	val jti: String? = null,

	@SerializedName("origin_jti")
	val originJti: String? = null,

	@SerializedName("event_id")
	val eventId: String? = null,

	val nonce: String? = null,

	// Custom Claims
	@SerializedName("cognito:roles")
	val cognitoRoles: List<String> = emptyList(),

	@SerializedName("custom:publicKeyCredCreated")
	val customPublicKeyCredCreated: String? = null,

	@SerializedName("custom:sc_pkey")
	val customScPkey: String? = null
) {


	companion object {

		/**
		 * Parse JWT token and create a LoginResponseModel using Gson
		 */
		@JvmStatic
		fun parseJwt(token: String): IdTokenResponseModel {
			// Split the token to get the payload
			val parts = token.split(".")
			if (parts.size != 3) {
				throw IllegalArgumentException("Invalid JWT token format")
			}

			// Decode the Base64Url-encoded payload
			val payload = decodeBase64Url(parts[1])

			// Use Gson to parse the JSON to LoginResponseModel
			val gson = com.google.gson.Gson()
			return gson.fromJson(payload, IdTokenResponseModel::class.java)
		}

		/**
		 * Create LoginResponseModel directly from JWT payload JSON
		 */
		@JvmStatic
		fun fromJson(jsonPayload: String): IdTokenResponseModel {
			val gson = com.google.gson.Gson()
			return gson.fromJson(jsonPayload, IdTokenResponseModel::class.java)
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

	fun getFullName(): String {
		return "$givenName $familyName"
	}
}
