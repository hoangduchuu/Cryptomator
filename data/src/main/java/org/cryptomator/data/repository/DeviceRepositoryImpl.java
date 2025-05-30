package org.cryptomator.data.repository;

import android.content.Context;
import android.util.Pair;

import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;

import org.cryptomator.data.api.DeviceApi;
import org.cryptomator.data.cloud.local.LocalStorageAccessFolder;
import org.cryptomator.data.dto.PostVaultDTO;
import org.cryptomator.data.dto.VaultRemoteDTO;
import org.cryptomator.data.dto.mappers.PostVaultMapper;
import org.cryptomator.data.dto.mappers.VaultRemoteMapper;
import org.cryptomator.domain.CloudFolder;
import org.cryptomator.domain.CloudType;
import org.cryptomator.domain.DeviceArgs;
import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.exception.FatalBackendException;
import org.cryptomator.domain.models.deployment.VaultRemote;
import org.cryptomator.domain.models.device.PostVault;
import org.cryptomator.domain.repository.DeploymentRepository;
import org.cryptomator.domain.repository.DeviceRepository;
import org.cryptomator.domain.repository.CloudContentRepository;
import org.cryptomator.domain.repository.PollResponse;
import org.cryptomator.domain.repository.UserProfileCacheRepository;
import org.cryptomator.domain.repository.VaultRepository;
import org.cryptomator.domain.usecases.ProgressAware;
import org.cryptomator.util.BuildConfig;
import org.cryptomator.util.DeploymentStatus;
import org.cryptomator.util.SharedPreferencesHandler;
import org.cryptomator.util.VaultStatus;
import org.cryptomator.util.VaultStatusParser;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;
import timber.log.Timber;


@Singleton
public class DeviceRepositoryImpl implements DeviceRepository {

	private final Context context;
	private final SharedPreferencesHandler sharedPreferencesHandler;
	private final Gson gson;
	private final DeviceApi deviceAPI;
	private final CloudContentRepository cloudContentRepository;
	private final AuthenticationHandler authenticationHandler;
	private final UserProfileCacheRepository userProfileCacheRepository;
	private final VaultRepository vaultRepository;

	private final DeploymentRepository deploymentRepository;

	private final VaultRemoteMapper deploymentInfoMapper;

	@Inject
	public DeviceRepositoryImpl(Context context,
			SharedPreferencesHandler sharedPreferencesHandler,
			DeviceApi deviceApi,
			CloudContentRepository cloudContentRepository,
			AuthenticationHandler authenticationHandler,
			UserProfileCacheRepository userProfileCacheRepository,
			VaultRepository vaultRepository,
			DeploymentRepository deploymentRepository,
			VaultRemoteMapper deploymentInfoMapper

	) {
		this.context = context;
		this.sharedPreferencesHandler = sharedPreferencesHandler;
		this.gson = new Gson();
		this.deviceAPI = deviceApi;
		this.cloudContentRepository = cloudContentRepository;
		this.authenticationHandler = authenticationHandler;
		this.userProfileCacheRepository = userProfileCacheRepository;
		this.vaultRepository = vaultRepository;
		this.deploymentRepository = deploymentRepository;
		this.deploymentInfoMapper = deploymentInfoMapper;
	}


	@Override
	public android.util.Pair<String, PostVault> postVault(@NotNull CloudFolder vaultFolder, @NotNull DeviceArgs deviceArgs, String vaultDescription) throws BackendException {
		String cloudPath = getCloudPath(vaultFolder);
		String volumePath = getVolumePath(vaultFolder);
		PostVault postVault = postNewDeviceWithoutCode(deviceArgs, vaultFolder.getName(), vaultDescription, volumePath, cloudPath);

		var secrets = buildVaultSecrets(vaultFolder);

		long currentTimestamp = System.currentTimeMillis() / 1000;


		authenticationHandler.executeWithTokenRefresh(token -> deviceAPI.setVaultSecret(postVault.getDeviceId(), currentTimestamp, "Bearer " + token, secrets));
		deploymentRepository.updateVaultStatus(DeploymentStatus.Added, Objects.requireNonNull(sharedPreferencesHandler.getComputerId()),postVault.getDeviceId());

		Timber.tag("DeviceRepository").d("Vault secrets updated for deviceId: %s", postVault.getDeviceId());

		return new Pair<>(postVault.getDeviceId(), postVault);
	}


	public static String generateRandomHex(int length) {
		SecureRandom random = new SecureRandom();
		StringBuilder result = new StringBuilder();
		String hexChars = "0123456789abcdef";

		for (int i = 0; i < length; i++) {
			int randomIndex = random.nextInt(16);
			result.append(hexChars.charAt(randomIndex));
		}

		String prefix = BuildConfig.DEBUG ? "dadr" : "adr";
		return "NCRYPTOR."+prefix + result;
	}

	@Override
	public boolean renameVault(@NotNull String deviceId, @NotNull String vaultId, @NotNull String newName) throws BackendException {
		Map<String, Object> requestMap = new HashMap<>();
		requestMap.put("nameLastModified", System.currentTimeMillis());
		requestMap.put("volumeName", newName);

		authenticationHandler.executeWithTokenRefresh(token -> deviceAPI.updateVaultInfo(deviceId, "Bearer " + token, requestMap));

		return true;
	}

	@Override
	public @NotNull VaultStatus getDeviceStatus(@NotNull String deviceId) throws BackendException {
		VaultRemoteDTO response = authenticationHandler.executeWithTokenRefresh(token -> deviceAPI.getDeviceInfo(deviceId, token));
		String status = response.getStatus();
		if (status != null) {
			return VaultStatusParser.Companion.parse(status);
		}
		throw new FatalBackendException("Unknown device status");
	}


	@Override
	public @NotNull PostVault postNewDeviceWithoutCode(@NotNull DeviceArgs deviceArgs,
			@NotNull String volumeName,
			@NotNull String vaultDescription,
			@NotNull String volumePath,
			@NotNull String cloudPath) throws BackendException {
		Map<String, Object> requestMap = new HashMap<>();
		requestMap.put("deviceId", generateRandomHex(22));
		requestMap.put("version", deviceArgs.getClientVersion());
		requestMap.put("volumeName", volumeName);
		requestMap.put("volumeDescription", vaultDescription);
		requestMap.put("computer", deviceArgs.toMap());

		requestMap.put("volumePath", volumePath);
		requestMap.put("cloudPath", cloudPath);

		// Add volumePath to computer map
		@SuppressWarnings("unchecked") Map<String, Object> computerMap = (Map<String, Object>) requestMap.get("computer");
		computerMap.put("volumePath", volumePath);
		computerMap.put("cloudPath", cloudPath);

		PostVaultDTO dto = authenticationHandler.executeWithTokenRefresh(token -> deviceAPI.activateDeviceWithVolume(requestMap, "Bearer " + token));

		return new PostVaultMapper().toModel(dto);
	}

	@Override
	public @NotNull PollResponse getPollDeviceStatus(@NotNull String userId, @NotNull String deviceId, @NotNull String eTag) throws BackendException {
		Response<Void> response = null;
		try {
			response = deviceAPI.pollDeviceStatus(userId, deviceId, eTag).execute();
		} catch (IOException e) {
			return new PollResponse(false, eTag, response.body());
		}

		// Check if the response is null
		if (response.headers().get("etag") == null) {
			return new PollResponse(true, "1", response.body());
		}
		String eTagHeader = Objects.requireNonNull(response.headers().get("etag")).replaceAll("\"","");

		if (response.code() == 304) {
			return new PollResponse(false, eTagHeader, null);
		}
		if(response.code() == 200) {
			return new PollResponse(true, eTagHeader, response.body());
		}
		return new PollResponse(false, eTagHeader, response.body());
	}

	@Override
	public @NotNull PollResponse getPollDevicePolicyId(@NotNull String userId, @NotNull String policyId, @NotNull String eTag) throws BackendException {
		Response<Void> response = null;
		try {
			response = deviceAPI.pollDevicePolicyStatus(userId, policyId, eTag).execute();
		} catch (IOException e) {
			return new PollResponse(false, eTag, response.body());
		}

		// Check if the response is null
		if (response.headers().get("etag") == null) {
			return new PollResponse(true, "1", response.body());
		}
		String eTagHeader = Objects.requireNonNull(response.headers().get("etag")).replaceAll("\"","");

		if (response.code() == 304) {
			return new PollResponse(false, eTagHeader, null);
		}
		if(response.code() == 200) {
			return new PollResponse(true, eTagHeader, response.body());
		}
		return new PollResponse(false, eTagHeader, response.body());
	}

	@Override
	public @NotNull Vault updateVaultIfNeeded(@NotNull Vault vault) throws BackendException {
		Vault newVault = vault;
		PollResponse pollResponse = getPollDeviceStatus(vault.getCreatedBy(), vault.getDeviceID(), vault.getEtag());
		boolean shouldUpdate = pollResponse.getNeedsUpdate();
		if (shouldUpdate) {
			VaultRemoteDTO response = authenticationHandler.executeWithTokenRefresh(token -> deviceAPI.getDeviceInfo(vault.getDeviceID(), token));
			Vault syncedVault = vaultRepository.store(response.toVault(vault, pollResponse.getETag()));

			if (response.getStatus() != null) {
				String status = response.getStatus();
				VaultStatus parsedStatus = VaultStatusParser.Companion.parse(status);

				if (parsedStatus == VaultStatus.ReadonlyPending) {
					syncedVault = updateVaultStatus(newVault, VaultStatus.ReadOnly);
				}
				if (parsedStatus == VaultStatus.DisablePending) {
					syncedVault = updateVaultStatus(newVault, VaultStatus.Disabled);
				}
				if (parsedStatus == VaultStatus.ResetPending) {
					syncedVault = updateVaultStatus(newVault, VaultStatus.Reset);
				}
			}

			newVault = syncedVault;
		}
		return newVault;
	}

	@Override
	public @NotNull Vault updateVaultPolicyIdIfNeeded(@NotNull Vault vault) throws BackendException {
		Vault newVault = vault;
		String userID = sharedPreferencesHandler.getUserCognitoId();
		assert userID != null;
		PollResponse pollResponse = getPollDevicePolicyId(userID, vault.getPolicyId(), vault.getPolicyEtag());
		boolean shouldUpdate = pollResponse.getNeedsUpdate();
		if (shouldUpdate) {
			VaultRemoteDTO response = authenticationHandler.executeWithTokenRefresh(token -> deviceAPI.getDeviceInfo(vault.getDeviceID(), token));
			Vault syncedVault = vaultRepository.store(response.toVaultWithPolicyEtag(vault, Objects.requireNonNull(pollResponse.getETag())));

			if (response.getStatus() != null) {
				String status = response.getStatus();
				VaultStatus parsedStatus = VaultStatusParser.Companion.parse(status);

				if (parsedStatus == VaultStatus.ReadonlyPending) {
					syncedVault = updateVaultStatus(newVault, VaultStatus.ReadOnly);
				}
				if (parsedStatus == VaultStatus.DisablePending) {
					syncedVault = updateVaultStatus(newVault, VaultStatus.Disabled);
				}
				if (parsedStatus == VaultStatus.ResetPending) {
					syncedVault = updateVaultStatus(newVault, VaultStatus.Reset);
				}
			}

			newVault = syncedVault;
		}
		return newVault;
	}

	private String getCloudPath(CloudFolder vaultFolder) {
		CloudType type = Objects.requireNonNull(vaultFolder.getCloud()).type();
		String email = "";

		if(type == CloudType.DROPBOX){
			email = sharedPreferencesHandler.getDropboxEmail();
		}

		if(type == CloudType.GOOGLE_DRIVE){
			email = sharedPreferencesHandler.getGoogleDriverEmail();
		}

		String path = vaultFolder.getPath();
		if(vaultFolder instanceof LocalStorageAccessFolder){
			path = ((LocalStorageAccessFolder) vaultFolder).getLocalFolder();
		}
		String cloudType = Objects.requireNonNull(Objects.requireNonNull(vaultFolder.getCloud()).type()).name();

		// For cloud storage, use the format: cloudType://email@/path
		// For local files, use the format: file:///path
		String result;
		if (cloudType.equals("LOCAL")) {
			result = "file://" + path;
		} else {
			result = cloudType.toLowerCase() + "://" + email.toLowerCase() + "@" + path;
		}

		// Replace spaces with %20
		result = result.replaceAll(" ", "%20");
		return result;
	}
	private String getVolumePath(CloudFolder vaultFolder) {
		String path = vaultFolder.getPath();
		if(vaultFolder instanceof LocalStorageAccessFolder){
			path = ((LocalStorageAccessFolder) vaultFolder).getLocalFolder();
		}
		return path;
	}

	@Override
	public @NotNull Vault updateVaultStatus(@NotNull Vault vault, @NotNull VaultStatus newStatus) throws BackendException {
		var statusString = VaultStatusParser.Companion.toString(newStatus);
		Map<String, Object> requestMap = new HashMap<>();
		requestMap.put("status", statusString);
		VaultRemoteDTO dto = authenticationHandler.executeWithTokenRefresh(token -> deviceAPI.updateVaultStatus(vault.getDeviceID(), "Bearer " + token, requestMap));

		Timber.tag("DeviceRepository").d("Vault status updated From %s to ----> %s", vault.getStatus() , statusString);
		return vaultRepository.store(dto.toVault(vault, vault.getEtag()));
	}

	@Override
	public @NotNull Object getDeploymentStatus(@NotNull String computerId) throws BackendException {
		return authenticationHandler.executeWithTokenRefresh(token -> deviceAPI.getDeploymentStatus(true, computerId, "Bearer " + token));
	}

	private Map<String, Object> buildVaultSecrets(CloudFolder vaultFolder) throws BackendException {
		// Read vault file content
		ByteArrayOutputStream masterkeyData = new ByteArrayOutputStream();
		cloudContentRepository.read(
				cloudContentRepository.file(vaultFolder, "mk.ncryptor"),
				null,
				masterkeyData,
				ProgressAware.NO_OP_PROGRESS_AWARE_DOWNLOAD
		);

		// Read vault file content
		ByteArrayOutputStream vaultData = new ByteArrayOutputStream();
		cloudContentRepository.read(
				cloudContentRepository.file(vaultFolder, "vault.ncryptor"),
				null,
				vaultData,
				ProgressAware.NO_OP_PROGRESS_AWARE_DOWNLOAD
		);
		String vaultContent = vaultData.toString();

		String base64MasterkeyContent = BaseEncoding.base64().encode(masterkeyData.toByteArray());

		Map<String, Object> requestMap = new HashMap<>();
		requestMap.put("vaultMK", base64MasterkeyContent); // mk.ncryptor content
		requestMap.put("vaultData", vaultContent); // vault.ncryptor content
		requestMap.put("vaultPKI", vaultContent); // vault

		return requestMap;
	}


	@Override
	public @NotNull Object logDeviceEvent(@NotNull String deviceId, @NotNull String type, @NotNull DeviceArgs deviceArgs) throws BackendException {
		List<Map<String, Object>> req = new java.util.ArrayList<>();
		Map<String, Object> logEntry = new HashMap<>();
		logEntry.put("type", type);
		logEntry.put("version", deviceArgs.getClientVersion());
		logEntry.put("computer", deviceArgs.toMap());
		logEntry.put("time", new Date().getTime());
		req.add(logEntry);
		return authenticationHandler.executeWithTokenRefresh(token -> deviceAPI.logDeviceActivity(deviceId, "Bearer " + token, req));
	}

	@NotNull
	@Override
	public VaultRemote getVaultRemote(@NotNull String deviceId)  throws BackendException {
		VaultRemoteDTO response = authenticationHandler.executeWithTokenRefresh(token -> deviceAPI.getDeviceInfo(deviceId, token));
		if (response == null) {
			throw new FatalBackendException("Failed to fetch vault remote status for deviceId: " + deviceId);
		}
		return deploymentInfoMapper.toDomain(response);
	}
}