package org.cryptomator.presentation.example

import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.interfaces.DecodedJWT
import timber.log.Timber

class JwtDecoderExample {
    
    /**
     * Example of how to decode a JWT token without verification
     */
    fun decodeJwtWithoutVerification(token: String): DecodedJWT? {
        return try {
            // Simple decode without verification
            val decodedJwt = JWT.decode(token)
            
            // Access JWT claims
            val subject = decodedJwt.subject
            val issuer = decodedJwt.issuer
            val expiresAt = decodedJwt.expiresAt
            
            // Access custom claims
            val customClaim = decodedJwt.getClaim("custom_claim").asString()
            
            Timber.d("JWT Decoded - Subject: $subject, Issuer: $issuer, Expires: $expiresAt")
            Timber.d("Custom claim value: $customClaim")
            
            decodedJwt
        } catch (e: JWTDecodeException) {
            Timber.e(e, "Failed to decode JWT token")
            null
        }
    }
    
    /**
     * Example of how to decode and verify a JWT token with HMAC256
     */
    fun decodeAndVerifyJwt(token: String, secret: ByteArray): DecodedJWT? {
        return try {
            // Decode and verify with HMAC256
            val verifier = JWT.require(com.auth0.jwt.algorithms.Algorithm.HMAC256(secret))
                .build()
            
            val verifiedJwt = verifier.verify(token)
            
            // Access verified claims
            val subject = verifiedJwt.subject
            val issuer = verifiedJwt.issuer
            val expiresAt = verifiedJwt.expiresAt
            
            Timber.d("JWT Verified - Subject: $subject, Issuer: $issuer, Expires: $expiresAt")
            
            verifiedJwt
        } catch (e: Exception) {
            Timber.e(e, "Failed to verify JWT token")
            null
        }
    }
    
    /**
     * Example of how to decode and verify a JWT token with ECDSA256
     */
    fun decodeAndVerifyJwtWithECDSA(token: String, publicKey: java.security.interfaces.ECPublicKey): DecodedJWT? {
        return try {
            // Decode and verify with ECDSA256
            val verifier = JWT.require(com.auth0.jwt.algorithms.Algorithm.ECDSA256(publicKey, null))
                .build()
            
            val verifiedJwt = verifier.verify(token)
            
            // Access verified claims
            val subject = verifiedJwt.subject
            val issuer = verifiedJwt.issuer
            val expiresAt = verifiedJwt.expiresAt
            
            Timber.d("JWT Verified with ECDSA - Subject: $subject, Issuer: $issuer, Expires: $expiresAt")
            
            verifiedJwt
        } catch (e: Exception) {
            Timber.e(e, "Failed to verify JWT token with ECDSA")
            null
        }
    }
    
    /**
     * Decode a specific JWT token and print all claims
     */
    fun decodeSpecificToken(token: String) {
        try {
            val decodedJwt = JWT.decode(token)
            
            // Print header
            println("Header:")
            println("  Algorithm: ${decodedJwt.algorithm}")
            println("  Type: ${decodedJwt.type}")
            
            // Print standard claims
            println("\nStandard Claims:")
            println("  Subject: ${decodedJwt.subject}")
            println("  Issuer: ${decodedJwt.issuer}")
            println("  Issued At: ${decodedJwt.issuedAt}")
            println("  Expires At: ${decodedJwt.expiresAt}")
            println("  Not Before: ${decodedJwt.notBefore}")
            println("  JWT ID: ${decodedJwt.id}")
            
            // Print all claims
            println("\nAll Claims:")
            val claims = decodedJwt.claims
            claims.forEach { (key, value) ->
                println("  $key: ${value.asString()}")
            }
            
            // Print signature
            println("\nSignature:")
            println("  ${decodedJwt.signature}")
            
        } catch (e: JWTDecodeException) {
            println("Failed to decode JWT token: ${e.message}")
        }
    }
    
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Initialize Timber for logging
            Timber.plant(Timber.DebugTree())
            
            val decoder = JwtDecoderExample()
            
            // The JWT token to decode
            val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTc0NDEzMDUwMCwiZXhwIjoxNzQ0MTM0MTAwfQ.lSIO3etfuWTR8gKXkO2-vEb7syCdRw_Ru7rfp-QhLKw"
            
            // Decode the token
            decoder.decodeSpecificToken(token)
        }
    }
} 