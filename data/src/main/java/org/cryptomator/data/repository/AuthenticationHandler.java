package org.cryptomator.data.repository;

import com.google.gson.Gson;
import org.cryptomator.data.dto.ErrorResponse;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.exception.FatalBackendException;
import org.cryptomator.util.SharedPreferencesHandler;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Utility class to handle authentication token refresh and retry logic.
 * This class provides a reusable mechanism to handle 401 Unauthorized exceptions
 * by refreshing the token and retrying the original request.
 */
@Singleton
public class AuthenticationHandler {

    private final SharedPreferencesHandler sharedPreferencesHandler;
    private TokenRefreshCallback tokenRefreshCallback;
    private final Gson gson;

    @Inject
    public AuthenticationHandler(SharedPreferencesHandler sharedPreferencesHandler) {
        this.sharedPreferencesHandler = sharedPreferencesHandler;
        this.gson = new Gson();
    }

    /**
     * Sets the callback to be used when a token refresh is needed.
     * This breaks the direct dependency on UserRepository.
     *
     * @param callback The callback to use for token refresh
     */
    public void setTokenRefreshCallback(TokenRefreshCallback callback) {
        this.tokenRefreshCallback = callback;
    }

    /**
     * Executes a network call and handles 401 Unauthorized exceptions by refreshing the token and retrying.
     *
     * @param callSupplier A function that creates a new Call object with the current access token
     * @param <T> The type of the response
     * @return The response body
     * @throws BackendException If the request fails after token refresh
     */
    public <T> T executeWithTokenRefresh(CallSupplier<T> callSupplier) throws BackendException {
        String accessToken = sharedPreferencesHandler.getCognitoAccessToken();
        Call<T> call = callSupplier.createCall(accessToken);
        
        try {
            Response<T> response = call.execute();
            
            // If successful, return the response body
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            }
            
            // If we get a 401 Unauthorized, try to refresh the token and retry
            if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
                Timber.d("Received 401 Unauthorized, attempting to refresh token");
                return handleUnauthorized(callSupplier);
            }
            
            // For other error codes, throw appropriate exceptions
            handleErrorResponse(response);
            return null; // This line will never be reached due to handleErrorResponse throwing an exception
        } catch (IOException e) {
            throw new FatalBackendException(e);
        }
    }
    
    /**
     * Handles 401 Unauthorized response by refreshing the token and retrying the request.
     *
     * @param callSupplier A function that creates a new Call object with the current access token
     * @param <T> The type of the response
     * @return The response body
     * @throws BackendException If the request fails after token refresh
     */
    private <T> T handleUnauthorized(CallSupplier<T> callSupplier) throws BackendException {
        String refreshToken = sharedPreferencesHandler.getCognitoRefreshToken();
        
        if (refreshToken.isEmpty()) {
            Timber.e("No refresh token available, cannot refresh access token");
            throw new FatalBackendException("No refresh token available");
        }
        
        if (tokenRefreshCallback == null) {
            Timber.e("No token refresh callback set, cannot refresh token");
            throw new FatalBackendException("No token refresh callback set");
        }
        
        try {
            // Refresh the token using the callback
            String newAccessToken = tokenRefreshCallback.refreshToken(refreshToken);
            
            // Save the new access token
            sharedPreferencesHandler.setCognitoAccessToken(newAccessToken);

            Timber.d("Token refreshed successfully, new access token: %s", newAccessToken);
            
            // Retry the original request with the new token
            Call<T> newCall = callSupplier.createCall(newAccessToken);
            Response<T> newResponse = newCall.execute();
            
            if (newResponse.isSuccessful() && newResponse.body() != null) {
                return newResponse.body();
            }
            
            // If we still get an error, handle it
            handleErrorResponse(newResponse);
            return null; // This line will never be reached due to handleErrorResponse throwing an exception
        } catch (IOException e) {
            throw new FatalBackendException("Failed to refresh token", e);
        }
    }
    
    /**
     * Handles error responses by throwing appropriate exceptions.
     *
     * @param response The response to handle
     * @throws BackendException The appropriate exception for the error
     */
    private <T> void handleErrorResponse(Response<T> response) throws BackendException {
        String errorMessage = getErrorMessageFromResponse(response);
        
        switch (response.code()) {
            case HttpURLConnection.HTTP_PAYMENT_REQUIRED:
                throw new org.cryptomator.domain.exception.hub.HubLicenseUpgradeRequiredException();
            case HttpURLConnection.HTTP_FORBIDDEN:
                if (errorMessage != null && errorMessage.contains("subscription plan limits exceed")) {
                    throw new org.cryptomator.domain.exception.device.CreateVaultLimitExeception(errorMessage);
                } else {
                    throw new org.cryptomator.domain.exception.hub.HubVaultAccessForbiddenException();
                }
            case HttpURLConnection.HTTP_GONE:
                throw new org.cryptomator.domain.exception.hub.HubVaultIsArchivedException();
            case 449:
                throw new org.cryptomator.domain.exception.hub.HubUserSetupRequiredException();
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                throw new FatalBackendException(errorMessage != null ? errorMessage : "401 Unauthorized!");
            default:
                throw new FatalBackendException(errorMessage != null ? errorMessage : "Failed with response code " + response.code());
        }
    }

    /**
     * Extracts error message from response body
     *
     * @param response The response to extract error message from
     * @return The error message or null if not available
     */
    private <T> String getErrorMessageFromResponse(Response<T> response) {
        try {
            ResponseBody errorBody = response.errorBody();
            if (errorBody != null) {
                String errorBodyString = errorBody.string();
                if (errorBodyString != null && !errorBodyString.isEmpty()) {
                    // Get the request URL
                    String url = response.raw().request().url().toString();
                    Timber.d("Request URL: %s", url);
                    
                    ErrorResponse errorResponse = gson.fromJson(errorBodyString, ErrorResponse.class);
                    if (errorResponse != null && errorResponse.getReason() != null) {
                        return String.format("URL: %s, Reason: %s", url, errorResponse.getReason());
                    }
                    return String.format("URL: %s, Body: %s", url, errorBodyString);
                }
            }
            // If no error body but we have a request, at least log the URL
            if (response.raw() != null && response.raw().request() != null) {
                String url = response.raw().request().url().toString();
                Timber.d("Request URL (no error body): %s", url);
                return "URL: " + url;
            }
        } catch (IOException | IllegalStateException e) {
            Timber.e(e, "Failed to parse error response");
        }
        return null;
    }
    
    /**
     * Functional interface for creating a Call object with the current access token.
     *
     * @param <T> The type of the response
     */
    @FunctionalInterface
    public interface CallSupplier<T> {
        /**
         * Creates a new Call object with the current access token.
         *
         * @param accessToken The current access token
         * @return A new Call object
         */
        Call<T> createCall(String accessToken);
    }
    
    /**
     * Callback interface for token refresh operations.
     */
    public interface TokenRefreshCallback {
        /**
         * Refreshes the access token using the refresh token.
         *
         * @param refreshToken The refresh token to use
         * @return The new access token
         * @throws BackendException If the refresh fails
         */
        String refreshToken(String refreshToken) throws BackendException;
    }
} 