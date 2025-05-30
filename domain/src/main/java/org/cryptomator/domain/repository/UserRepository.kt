package org.cryptomator.domain.repository

import org.cryptomator.domain.DeviceArgs
import org.cryptomator.domain.exception.BackendException
import org.cryptomator.domain.models.userprofile.UserProfile

interface UserRepository {

	@Throws(BackendException::class)
	fun getUserProfile(token: String, deviceArgs: DeviceArgs): UserProfile

	@Throws(BackendException::class)
	fun login(token: String, deviceArgs: DeviceArgs): UserProfile

	// refresh token
	@Throws(BackendException::class)
	fun refreshToken(token: String): String

	fun getUserAvatar(token: String, userProfile: UserProfile): String

	fun cacheUserAvatarFromBase64String(base64String: String)

	fun cacheUserAvatarFromUrl(url: String)

	@Throws(BackendException::class)
	fun getPollUserStatus(userId: String): PollResponse
}
