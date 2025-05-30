package org.cryptomator.data.api;

import org.cryptomator.data.dto.DeploymentResponseDTO;
import org.cryptomator.data.dto.VaultInfoDTO;
import org.cryptomator.data.dto.VaultRemoteDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface DeploymentApi {
    @GET("/account")
    Call<DeploymentResponseDTO> getDeploymentStatus(
        @Query("deploymentStatus") boolean deploymentStatus,
        @Query("computerId") String computerId,
        @Header("Authorization") String authorization
    );

    @GET("/devices/vault")
    Call<VaultInfoDTO> getVaultInfo(
        @Query("deviceId") String deviceId,
        @Header("Authorization") String authorization
    );

    @GET("/devices/info")
    Call<VaultRemoteDTO> getVault(
        @Query("deviceId") String deviceId,
        @Header("cognito_jwt") String authorization
    );

	@GET("/devices/info")
	Call<VaultRemoteDTO> getVault(
			@Query("deviceId") String deviceId,
			@Query("computerId") String computerId,
			@Header("cognito_jwt") String authorization
	);

	@POST("/account")
	Call<List<DeploymentResponseDTO.Deployment>> updateVaultStatus(
			@Query("deploymentStatus") boolean deploymentStatus,
			@Query("status") String status,
			@Query("computerId") String computerId,
			@Query("deviceId") String deviceId,
			@Header("Authorization") String authorization);
} 