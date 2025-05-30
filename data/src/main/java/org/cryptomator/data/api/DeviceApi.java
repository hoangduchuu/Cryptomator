package org.cryptomator.data.api;

import org.cryptomator.data.dto.GetDeviceCodeDTO;
import org.cryptomator.data.dto.DeviceActivationDTO;
import org.cryptomator.data.dto.PostVaultDTO;
import org.cryptomator.data.dto.VaultRemoteDTO;

import java.util.Map;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface DeviceApi {

	@POST("/devices/info")
	Call<Object> updateVaultInfo(@Query("deviceId") String deviceId, @Header("Authorization") String authorization, @Body Map body);

	@POST("/devices/info")
	Call<VaultRemoteDTO> updateVaultStatus(@Query("deviceId") String deviceId, @Header("Authorization") String authorization, @Body Map body);

	@GET("/devices/info")
	Call<VaultRemoteDTO> getDeviceInfo(@Query("deviceId") String deviceId, @Header("cognito_jwt") String authorization);

	@PUT("/devices/activate")
	Call<PostVaultDTO> activateDeviceWithVolume(@Body Map body, @Header("Authorization") String authorization);

	@HEAD("/devices/poll")
	Call<Void> pollDeviceStatus(@Query("userId") String userId, @Query("deviceId") String deviceId, @Query("eTag") String eTag);
	@HEAD("/policies/poll")
	Call<Void> pollDevicePolicyStatus(@Query("userId") String userId, @Query("policyId") String policyId, @Query("eTag") String eTag);

	@GET("/account")
	Call<Object> getDeploymentStatus(@Query("deploymentStatus") boolean deploymentStatus, @Query("computerId") String computerId, @Header("Authorization") String authorization);

	@POST("/devices/vault")
	Call<Object> setVaultSecret(@Query("deviceId") String deviceId, @Query("lastVaultUpdated") long lastVaultUpdated, @Header("Authorization") String authorization, @Body Map<String, Object> body);

	@PUT("/devices/log")
	Call<Object> logDeviceActivity(@Query("deviceId") String deviceId, @Header("Authorization") String authorization, @Body List<Map<String, Object>> logs);
}