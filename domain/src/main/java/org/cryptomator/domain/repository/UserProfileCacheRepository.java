package org.cryptomator.domain.repository;

import org.cryptomator.domain.models.userprofile.UserProfile;

public interface UserProfileCacheRepository {
    UserProfile getCachedProfile();
    void cacheProfile(UserProfile userProfile);
    void clearCache();
} 