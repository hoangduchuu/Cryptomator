package org.cryptomator.domain.usecases.user;

import org.cryptomator.domain.models.userprofile.UserProfile;
import org.cryptomator.domain.repository.UserProfileCacheRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;

@UseCase
class CacheUserProfile {

    private final UserProfileCacheRepository repository;
    private final UserProfile userProfile;

    public CacheUserProfile(UserProfileCacheRepository repository, @Parameter UserProfile userProfile) {
        this.repository = repository;
        this.userProfile = userProfile;
    }

    public void execute() {
        repository.cacheProfile(userProfile);
    }
} 