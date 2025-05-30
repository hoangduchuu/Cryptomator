package org.cryptomator.data.repository;

import com.google.gson.Gson;
import org.cryptomator.domain.models.userprofile.UserProfile;
import org.cryptomator.domain.repository.UserProfileCacheRepository;
import org.cryptomator.util.SharedPreferencesHandler;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UserProfileCacheRepositoryImpl implements UserProfileCacheRepository {

    private final SharedPreferencesHandler sharedPreferencesHandler;
    private final Gson gson;

    @Inject
    public UserProfileCacheRepositoryImpl(SharedPreferencesHandler sharedPreferencesHandler) {
        this.sharedPreferencesHandler = sharedPreferencesHandler;
        this.gson = new Gson();
    }

    @Override
    public UserProfile getCachedProfile() {
        String json = sharedPreferencesHandler.getUserProfileCache();
        if (json == null) return null;
        return gson.fromJson(json, UserProfile.class);
    }

    @Override
    public void cacheProfile(UserProfile userProfile) {
        String json = gson.toJson(userProfile);
		sharedPreferencesHandler.setCurrentPlan(userProfile.getCurrentPlan());
        sharedPreferencesHandler.setUserProfileCache(json);
    }

    @Override
    public void clearCache() {
		sharedPreferencesHandler.clearSSOUserFullName();
		sharedPreferencesHandler.clearUserProfileCache();
    }
} 