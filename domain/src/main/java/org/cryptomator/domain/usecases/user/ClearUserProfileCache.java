package org.cryptomator.domain.usecases.user;

import org.cryptomator.domain.repository.UserProfileCacheRepository;
import org.cryptomator.generator.UseCase;

@UseCase
class ClearUserProfileCache {

    private final UserProfileCacheRepository repository;

    public ClearUserProfileCache(UserProfileCacheRepository repository) {
        this.repository = repository;
    }

    public void execute() {
        repository.clearCache();
    }
} 