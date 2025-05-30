package org.cryptomator.domain.usecases.user;

import org.cryptomator.domain.models.userprofile.UserProfile;
import org.cryptomator.domain.repository.UserProfileCacheRepository;
import org.cryptomator.generator.UseCase;

@UseCase
class GetCachedUserProfile {

    private final UserProfileCacheRepository repository;

    public GetCachedUserProfile(UserProfileCacheRepository repository) {
        this.repository = repository;
    }

    public UserProfile execute() {
        return repository.getCachedProfile();
    }
} 