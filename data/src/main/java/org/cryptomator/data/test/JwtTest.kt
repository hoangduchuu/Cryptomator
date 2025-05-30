package org.cryptomator.data.test

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.exceptions.JWTDecodeException

/**
 * Simple test class to verify JWT imports are working
 */
class JwtTest {
    
    fun testJwtDecode(token: String): DecodedJWT? {
        return try {
            JWT.decode(token)
        } catch (e: JWTDecodeException) {
            null
        }
    }
} 