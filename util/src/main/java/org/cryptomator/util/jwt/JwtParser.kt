import com.nimbusds.jose.JWSObject
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT

/**
 * Utility class for JWT token operations using Nimbus JOSE+JWT library
 */
class JwtParser {
	companion object {
		/**
		 * Parse a JWT token and extract its payload.
		 *
		 * @param token The JWT token string to parse
		 * @return The JWT claims set containing the payload data
		 */
		@JvmStatic
		fun parseJwt(token: String): JWTClaimsSet {
			val signedJWT = SignedJWT.parse(token)
			return signedJWT.jwtClaimsSet
		}

		/**
		 * Extract a specific claim from the JWT token
		 *
		 * @param token The JWT token string to parse
		 * @param claimName The name of the claim to extract
		 * @return The claim value as a String or null if claim doesn't exist
		 */
		@JvmStatic
		fun getClaim(token: String, claimName: String): String? {
			val claimsSet = parseJwt(token)
			return claimsSet.getStringClaim(claimName)
		}

		/**
		 * Parse the JWT header and payload without validation
		 *
		 * @param token The JWT token string to parse
		 * @return A pair containing the header and payload
		 */
		@JvmStatic
		fun parseJwtParts(token: String): Pair<Map<String, Any>, Map<String, Any>> {
			val jwsObject = JWSObject.parse(token)
			val header = jwsObject.header.toJSONObject()
			val payload = jwsObject.payload.toJSONObject()

			@Suppress("UNCHECKED_CAST")
			return Pair(
				header as Map<String, Any>,
				payload as Map<String, Any>
			)
		}
	}
}

// Example usage
fun main() {
	val sampleToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

	try {
		// Parse JWT and get claims
		val claimsSet = JwtParser.parseJwt(sampleToken)
		println("Subject: ${claimsSet.subject}")
		println("Issued at: ${claimsSet.issueTime}")

		// Get a specific claim
		val name = JwtParser.getClaim(sampleToken, "name")
		println("Name claim: $name")

		// Get header and payload as maps
		val (header, payload) = JwtParser.parseJwtParts(sampleToken)
		println("Header: $header")
		println("Payload: $payload")

	} catch (e: Exception) {
		println("Failed to parse token: ${e.message}")
		e.printStackTrace()
	}
}