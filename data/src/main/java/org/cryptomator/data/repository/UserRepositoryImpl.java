package org.cryptomator.data.repository;

import android.content.Context;
import android.util.Base64;

import com.google.gson.Gson;

import org.cryptomator.data.api.UserApi;
import org.cryptomator.domain.DeviceArgs;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.exception.FatalBackendException;
import org.cryptomator.domain.exception.hub.HubLicenseUpgradeRequiredException;
import org.cryptomator.domain.exception.hub.HubUserSetupRequiredException;
import org.cryptomator.domain.exception.hub.HubVaultAccessForbiddenException;
import org.cryptomator.domain.exception.hub.HubVaultIsArchivedException;
import org.cryptomator.domain.models.userprofile.RefreshTokenResponse;
import org.cryptomator.domain.models.userprofile.UserProfile;
import org.cryptomator.domain.repository.PollResponse;
import org.cryptomator.domain.repository.UserRepository;
import org.cryptomator.util.SharedPreferencesHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jetbrains.annotations.NotNull;

import retrofit2.Call;
import retrofit2.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import timber.log.Timber;

@Singleton
public class UserRepositoryImpl implements UserRepository, AuthenticationHandler.TokenRefreshCallback {

	private final Context context;
	private final SharedPreferencesHandler sharedPreferencesHandler;
	private final Gson gson;
	private final UserApi userApi;
	private final AuthenticationHandler authenticationHandler;

	@Inject
	public UserRepositoryImpl(Context context, SharedPreferencesHandler sharedPreferencesHandler, UserApi userApi, AuthenticationHandler authenticationHandler) {
		this.context = context;
		this.sharedPreferencesHandler = sharedPreferencesHandler;
		this.gson = new Gson();
		this.userApi = userApi;
		this.authenticationHandler = authenticationHandler;

		// Set this repository as the token refresh callback
		this.authenticationHandler.setTokenRefreshCallback(this);
	}

	@Override
	public @NotNull UserProfile getUserProfile(@NotNull String token, @NotNull DeviceArgs deviceArgs) throws BackendException {
		Timber.d("UserRepositoryImpl: call: getUserProfile");
		
		return authenticationHandler.executeWithTokenRefresh(accessToken -> 
			userApi.getUserProfile(
				"Bearer " + accessToken,
				"application/json"
			)
		);
	}

	@Override
	public @NotNull UserProfile login(@NotNull String token, @NotNull DeviceArgs deviceArgs) throws BackendException {
		Timber.d("UserRepositoryImpl: call: login");
		
		return authenticationHandler.executeWithTokenRefresh(accessToken -> 
			userApi.login(
				"Bearer " + accessToken,
				"application/json",
				deviceArgs.toJsonRequest()
			)
		);
	}

	@Override
	public @NotNull String refreshToken(@NotNull String refreshToken) throws BackendException {
		Map<String, String> authParams = new HashMap<>();
		authParams.put("REFRESH_TOKEN", refreshToken);

		Map<String, Object> requestMap = new HashMap<>();
		requestMap.put("AuthFlow", "REFRESH_TOKEN_AUTH");
		requestMap.put("ClientId", "2pidafln9e6bjltfoh3ab7fjn0");
		requestMap.put("AuthParameters", authParams);

		Call<RefreshTokenResponse> call = userApi.refreshToken(
			"application/x-amz-json-1.1",
			"AWSCognitoIdentityProviderService.InitiateAuth",
			requestMap
		);

		try {
			Response<RefreshTokenResponse> response = call.execute();
			return Objects.requireNonNull(Objects.requireNonNull(handleResponse(response).getAuthenticationResult()).getAccessToken());
		} catch (IOException e) {
			throw new FatalBackendException("Failed to refresh token", e);
		}
	}

	@NotNull
	@Override
	public String getUserAvatar(@NotNull String token, @NotNull UserProfile userProfile) {
		Timber.d("UserRepositoryImpl: Getting avatar for userId: %s", userProfile);
		if(userProfile.isSSoUser()) {
			cacheUserAvatarFromUrl(userProfile.getPicturePreSignedURL());
			Timber.d("UserRepositoryImpl: User is SSO user, skipping avatar retrieval");
			return "";
		}

		try {
			Response<ResponseBody> response = userApi.getUserAvatar("Bearer " + token, userProfile.getId()).execute();

			if (!response.isSuccessful()) {
				throw new FatalBackendException("Failed to get avatar. Response code: " + response.code());
			}

			ResponseBody body = response.body();
			if (body == null) {
				throw new FatalBackendException("Avatar response body is null");
			}
			byte[] bytes = body.bytes();
			Timber.d("Avatar bytes length: %d", bytes.length);
			if (bytes.length >= 2) {
				Timber.d("First two bytes: %02X %02X", bytes[0] & 0xFF, bytes[1] & 0xFF);
			}

			// Convert bytes to Base64 string
			String base64String = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
			cacheUserAvatarFromBase64String(base64String);
			return base64String;
		} catch (IOException e) {
			Timber.e(e, "Error getting avatar: %s", e.getMessage());
			throw new FatalBackendException("Error getting avatar: " + e.getMessage());
		}
	}

	@Override
	public void cacheUserAvatarFromBase64String(@NotNull String base64String) {
		Timber.d("UserRepositoryImpl: Caching avatar from base64 string");

		try {
			// Decode base64 string to bytes
			byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);

			// Create cache directory if not exists
			File cacheDir = new File(context.getCacheDir(), "avatar_cache");
			if (!cacheDir.exists()) {
				cacheDir.mkdirs();
			}

			// Save to file
			File avatarFile = new File(cacheDir, "user_avatar.jpg");
			try (FileOutputStream outputStream = new FileOutputStream(avatarFile)) {
				outputStream.write(decodedBytes);
				outputStream.flush();
				Timber.d("Avatar cached successfully to: %s", avatarFile.getAbsolutePath());
			}
		} catch (IOException e) {
			Timber.e(e, "Failed to cache avatar: %s", e.getMessage());
			throw new FatalBackendException("Failed to cache avatar: " + e.getMessage());
		}
	}

	@Override
	public void cacheUserAvatarFromUrl(@NotNull String url) {
		Timber.d("UserRepositoryImpl: Caching avatar from URL: %s", url);

		try {
			// Create cache directory if not exists
			File cacheDir = new File(context.getCacheDir(), "avatar_cache");
			if (!cacheDir.exists()) {
				cacheDir.mkdirs();
			}

			// Save to file
			File avatarFile = new File(cacheDir, "user_avatar.jpg");

			// save the image from URL to the file
			URL imageUrl = new URL(url);
			InputStream inputStream = imageUrl.openStream();
			FileOutputStream outputStream = new FileOutputStream(avatarFile);
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
			}
			outputStream.close();
			inputStream.close();
			Timber.d("Avatar cached successfully to: %s", avatarFile.getAbsolutePath());
		} catch (IOException e) {
			Timber.e(e, "Failed to cache avatar: %s", e.getMessage());
			throw new FatalBackendException("Failed to cache avatar: " + e.getMessage());
		}
	}

	@Override
	public @NotNull PollResponse getPollUserStatus(@NotNull String userId) throws BackendException {
		Response<Void> response = null;
		String etag = sharedPreferencesHandler.getUserETag();
		try {
			response = userApi.pollUserStatus(userId, etag).execute();
		} catch (IOException e) {
			return new PollResponse(false, etag, response.body());
		}

		// Check if the response is null
		if (response == null || response.headers().get("etag") == null) {
			return new PollResponse(false, "1", response.body());
		}
		String eTagHeader = Objects.requireNonNull(response.headers().get("etag")).replaceAll("\"","");
		sharedPreferencesHandler.setUserETag(eTagHeader);

		if (response.code() == 304) {
			return new PollResponse(false, eTagHeader, null);
		}
		if(response.code() == 200) {
			return new PollResponse(true, eTagHeader, response.body());
		}
		return new PollResponse(false, eTagHeader, response.body());
	}

	private <T> T handleResponse(Response<T> response) throws BackendException {
		if (response.isSuccessful() && response.body() != null) {
			return response.body();
		}

		switch (response.code()) {
			case HttpURLConnection.HTTP_PAYMENT_REQUIRED:
				throw new HubLicenseUpgradeRequiredException();
			case HttpURLConnection.HTTP_FORBIDDEN:
				throw new HubVaultAccessForbiddenException();
			case HttpURLConnection.HTTP_GONE:
				throw new HubVaultIsArchivedException();
			case 449:
				throw new HubUserSetupRequiredException();
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				throw new FatalBackendException("401 Unauthorized!");
			default:
				throw new FatalBackendException("Failed with response code " + response.code());
		}
	}

}