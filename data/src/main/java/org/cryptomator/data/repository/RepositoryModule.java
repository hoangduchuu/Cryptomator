package org.cryptomator.data.repository;

import android.content.Context;

import org.cryptomator.data.api.DeploymentApi;
import org.cryptomator.data.api.DeviceApi;
import org.cryptomator.data.api.RetrofitClient;
import org.cryptomator.data.api.UserApi;
import org.cryptomator.domain.repository.CloudContentRepository;
import org.cryptomator.domain.repository.CloudRepository;
import org.cryptomator.domain.repository.DeploymentRepository;
import org.cryptomator.domain.repository.DeviceRepository;
import org.cryptomator.domain.repository.HubRepository;
import org.cryptomator.domain.repository.UpdateCheckRepository;
import org.cryptomator.domain.repository.UserProfileCacheRepository;
import org.cryptomator.domain.repository.UserRepository;
import org.cryptomator.domain.repository.VaultRepository;
import org.cryptomator.util.SharedPreferencesHandler;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import retrofit2.Retrofit;

@Module
public class RepositoryModule {

	@Singleton
	@Provides
	public Retrofit provideRetrofit(Context context) {
		return RetrofitClient.getClient(context);
	}

	@Singleton
	@Provides
	public UserApi provideUserApi(Retrofit retrofit) {
		return retrofit.create(UserApi.class);
	}

	@Singleton
	@Provides
	public DeviceApi provideDeviceAPI(Retrofit retrofit) {
		return retrofit.create(DeviceApi.class);
	}

	@Singleton
	@Provides
	public DeploymentApi provideDeploymentApi(Retrofit retrofit) {
		return retrofit.create(DeploymentApi.class);
	}

	@Singleton
	@Provides
	public AuthenticationHandler provideAuthenticationHandler(SharedPreferencesHandler sharedPreferencesHandler) {
		return new AuthenticationHandler(sharedPreferencesHandler);
	}

	@Singleton
	@Provides
	public CloudRepository provideCloudRepository(CloudRepositoryImpl cloudRepository) {
		return cloudRepository;
	}

	@Singleton
	@Provides
	public VaultRepository provideVaultRepository(VaultRepositoryImpl vaultRepository) {
		return vaultRepository;
	}

	@Singleton
	@Provides
	public CloudContentRepository provideCloudContentRepository(DispatchingCloudContentRepository cloudContentRepository) {
		return cloudContentRepository;
	}

	@Singleton
	@Provides
	public HubRepository provideHubRepositoryRepository(HubRepositoryImpl hubRepository) {
		return hubRepository;
	}

	@Singleton
	@Provides
	public UpdateCheckRepository provideBetaStatusRepository(UpdateCheckRepositoryImpl updateCheckRepository) {
		return updateCheckRepository;
	}

	@Singleton
	@Provides
	public UserRepository provideUserRepository(UserRepositoryImpl userRepository){
		return userRepository;
	}

	@Singleton
	@Provides
	public UserProfileCacheRepository provideUserProfileCacheRepository(UserProfileCacheRepositoryImpl userProfileCacheRepository) {
		return userProfileCacheRepository;
	}

	@Singleton
	@Provides
	public DeviceRepository provideDeviceRepository(DeviceRepositoryImpl deviceRepository) {
		return deviceRepository;
	}

	@Singleton
	@Provides
	public DeploymentRepository provideDeploymentRepository(DeploymentRepositoryImpl deploymentRepository) {
		return deploymentRepository;
	}

}
