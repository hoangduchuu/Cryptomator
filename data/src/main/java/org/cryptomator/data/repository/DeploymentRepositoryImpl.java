package org.cryptomator.data.repository;

import android.content.Context;

import org.cryptomator.data.api.DeploymentApi;
import org.cryptomator.data.cloud.crypto.CryptoCloud;
import org.cryptomator.data.dto.DeploymentInfoDTO;
import org.cryptomator.data.dto.DeploymentResponseDTO;
import org.cryptomator.data.dto.VaultInfoDTO;
import org.cryptomator.data.dto.VaultRemoteDTO;
import org.cryptomator.data.dto.mappers.DeploymentInfoMapper;
import org.cryptomator.data.dto.mappers.DeploymentResponseMapper;
import org.cryptomator.data.dto.mappers.VaultInfoMapper;
import org.cryptomator.data.dto.mappers.VaultRemoteMapper;
import org.cryptomator.domain.*;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.models.deployment.Deployment;
import org.cryptomator.domain.models.deployment.DeploymentInfo;
import org.cryptomator.domain.models.deployment.DeploymentWithStatus;
import org.cryptomator.domain.models.deployment.GetDeploymentResponse;
import org.cryptomator.domain.models.deployment.DeploymentStatusInfo;
import org.cryptomator.domain.models.vault.VaultInfo;
import org.cryptomator.domain.repository.DeploymentRepository;
import org.cryptomator.domain.repository.VaultRepository;
import org.cryptomator.util.DeploymentStatus;
import org.cryptomator.util.DeploymentStatusParser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

import org.cryptomator.data.util.DeploymentMapperUtils;

@Singleton
public class DeploymentRepositoryImpl implements DeploymentRepository {

	private final DeploymentApi deploymentApi;
	private final AuthenticationHandler authenticationHandler;
	private final VaultRepository vaultRepository;
	private final Context context;
	private final DeploymentInfoMapper deploymentInfoMapper;
	private final VaultRemoteMapper vaultRemoteMapper;

	@Inject
	public DeploymentRepositoryImpl(DeploymentApi deploymentApi, AuthenticationHandler authenticationHandler, VaultRepository vaultRepository, Context context, DeploymentInfoMapper deploymentInfoMapper, VaultRemoteMapper vaultRemoteMapper) {
		this.deploymentApi = deploymentApi;
		this.authenticationHandler = authenticationHandler;
		this.vaultRepository = vaultRepository;
		this.context = context;
		this.deploymentInfoMapper = deploymentInfoMapper;
		this.vaultRemoteMapper = vaultRemoteMapper;
	}

	@Override
	public @NotNull List<DeploymentWithStatus> getDeploymentInfo(@NotNull String deviceId) throws BackendException {
		Timber.tag("DeploymentRepository").d("Getting deployment info for device: %s", deviceId);

		// 1. Get deployment status directly from API
		DeploymentResponseDTO deploymentResponseDTO = authenticationHandler.executeWithTokenRefresh(token -> deploymentApi.getDeploymentStatus(true, deviceId, "Bearer " + token));
		GetDeploymentResponse deploymentResponse = new DeploymentResponseMapper().toModel(deploymentResponseDTO);
		List<Deployment> deployments = deploymentResponse.getDeployments();

		if (deployments.isEmpty()) {
			return Collections.emptyList();
		}

		List<DeploymentWithStatus> deploymentInfoList = new ArrayList<>();

		// Process each deployment
		for (Deployment deployment : deployments) {
			String processVaultId = deployment.getDeviceId();
			Timber.tag("DeploymentRepository").d("Processing vault with ID: %s, Status: %s", processVaultId, deployment.getStatus());

			try {
				// 2. Get vault info
				VaultInfoDTO vaultInfoDTO = authenticationHandler.executeWithTokenRefresh(token -> deploymentApi.getVaultInfo(processVaultId, "Bearer " + token));

				// 3. Get vault details
				VaultRemoteDTO vaultRemoteDTO = authenticationHandler.executeWithTokenRefresh(token -> deploymentApi.getVault(deployment.getDeviceId(), deviceId, token));

				// 4. Create DeploymentInfoDTO
				DeploymentInfoDTO deploymentInfoDTO = new DeploymentInfoDTO(vaultRemoteDTO, vaultInfoDTO);

				// 5. Convert to domain model
				DeploymentInfo deploymentInfo = deploymentInfoMapper.toDomain(deploymentInfoDTO);
				
				// 6. Create DeploymentWithStatus
				DeploymentStatus status = DeploymentStatusParser.Companion.parse(deployment.getStatus());
				DeploymentWithStatus deploymentWithStatus = new DeploymentWithStatus(deploymentInfo, status);
				
				deploymentInfoList.add(deploymentWithStatus);

			} catch (Exception e) {
				Timber.tag("DeploymentRepository").e(e, "Error processing deployment for vault ID: %s", processVaultId);
				// Continue with next deployment if one fails
			}
		}

		return deploymentInfoList;
	}

	private Cloud createCloudFromType(CloudType cloudType, Vault vault, VaultRemoteDTO dto) {
		String userName = DeploymentMapperUtils.extractEmailFromCloudPath(Objects.requireNonNull(dto.getComputers()).get(0).getCloudPath());
		return switch (cloudType) {
			case DROPBOX -> new DropboxCloud(DropboxCloud.aDropboxCloud().withUsername(userName));
			case GOOGLE_DRIVE -> new GoogleDriveCloud(GoogleDriveCloud.aGoogleDriveCloud().withUsername(userName));
			case ONEDRIVE -> new OnedriveCloud(OnedriveCloud.aOnedriveCloud().withUsername(userName));
			case PCLOUD -> new PCloud(PCloud.aPCloud().withUsername(userName));
			case WEBDAV -> new WebDavCloud(WebDavCloud.aWebDavCloudCloud().withUsername(userName));
			case LOCAL -> new LocalStorageCloud(LocalStorageCloud.aLocalStorage());
			case S3 -> new S3Cloud(S3Cloud.aS3Cloud());
			case CRYPTO -> new CryptoCloud(vault);
		};
	}

	@NotNull
	@Override
	public void updateVaultStatus(@NotNull DeploymentStatus status, @NotNull String computerId, @NotNull String vaultId) {
		try {
			authenticationHandler.executeWithTokenRefresh(token -> {
				return deploymentApi.updateVaultStatus(true, DeploymentStatusParser.Companion.toString(status), computerId, vaultId, "Bearer " + token);
			});
		} catch (BackendException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<DeploymentStatusInfo> getDeploymentStatusInfo(String deviceSerial) throws BackendException {
		Timber.tag("DeploymentRepository").d("Getting deployment status info for device: %s", deviceSerial);
		List<DeploymentStatusInfo> statusList = new ArrayList<>();
		// 1. Get deployment status directly from API
		DeploymentResponseDTO deploymentResponseDTO = authenticationHandler.executeWithTokenRefresh(token -> deploymentApi.getDeploymentStatus(true, deviceSerial, "Bearer " + token));
		GetDeploymentResponse deploymentResponse = new DeploymentResponseMapper().toModel(deploymentResponseDTO);
		List<Deployment> deployments = deploymentResponse.getDeployments();
		for (Deployment deployment : deployments) {
			DeploymentStatus status = DeploymentStatusParser.Companion.parse(deployment.getStatus());
			statusList.add(new DeploymentStatusInfo(deployment.getDeviceId(), status));
		}
		return statusList;
	}

	@Override
	public boolean vaultExistsLocally(String vaultId) {
		try {
			List<Vault> localVaults = vaultRepository.vaults();
			for (Vault vault : localVaults) {
				if (vault.getDeviceID().equals(vaultId)) {
					return true;
				}
			}
		} catch (Exception e) {
			Timber.tag("DeploymentRepository").e(e, "Error checking local vault existence for: %s", vaultId);
		}
		return false;
	}

	@Override
	public DeploymentWithStatus getDeploymentWithStatus(String deviceSerial, String vaultId) throws BackendException {
		Timber.tag("DeploymentRepository").d("Getting full deployment info for vault: %s, device: %s", vaultId, deviceSerial);
		// 1. Get vault info
		VaultInfoDTO vaultInfoDTO = authenticationHandler.executeWithTokenRefresh(token -> deploymentApi.getVaultInfo(vaultId, "Bearer " + token));
		// 2. Get vault details
		VaultRemoteDTO vaultRemoteDTO = authenticationHandler.executeWithTokenRefresh(token -> deploymentApi.getVault(vaultId, deviceSerial, token));
		// 3. Create DeploymentInfoDTO
		DeploymentInfoDTO deploymentInfoDTO = new DeploymentInfoDTO(vaultRemoteDTO, vaultInfoDTO);
		// 4. Convert to domain model
		DeploymentInfo deploymentInfo = deploymentInfoMapper.toDomain(deploymentInfoDTO);
		// 5. Get status (from status API or by inferring)
		// We'll fetch status from the status API for this vault
		DeploymentStatus status = DeploymentStatus.Unknown;
		try {
			DeploymentResponseDTO deploymentResponseDTO = authenticationHandler.executeWithTokenRefresh(token -> deploymentApi.getDeploymentStatus(true, deviceSerial, "Bearer " + token));
			GetDeploymentResponse deploymentResponse = new DeploymentResponseMapper().toModel(deploymentResponseDTO);
			for (Deployment deployment : deploymentResponse.getDeployments()) {
				if (vaultId.equals(deployment.getDeviceId())) {
					status = DeploymentStatusParser.Companion.parse(deployment.getStatus());
					break;
				}
			}
		} catch (Exception e) {
			Timber.tag("DeploymentRepository").e(e, "Error fetching status for vault: %s", vaultId);
		}
		return new DeploymentWithStatus(deploymentInfo, status);
	}
}