package org.cryptomator.data.cache

import android.util.LruCache
import org.cryptomator.domain.models.userprofile.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileCacheManager @Inject constructor() {
    private val cache: LruCache<String, UserProfile> = LruCache(1)

    fun getCachedProfile(): UserProfile? {
        return cache.get(CACHE_KEY)
    }

    fun cacheProfile(userProfile: UserProfile) {
        cache.put(CACHE_KEY, userProfile)
    }

    fun clearCache() {
        cache.evictAll()
    }

    companion object {
        private const val CACHE_KEY = "user_profile"
    }
} 