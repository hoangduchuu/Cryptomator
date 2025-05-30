package org.cryptomator.data.repository;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Environment;

import com.google.gson.Gson;

import org.cryptomator.data.cloud.crypto.CryptoCloudContentRepositoryFactory;
import org.cryptomator.data.cloud.crypto.CryptoCloudFactory;
import org.cryptomator.data.db.Database;
import org.cryptomator.data.db.entities.VaultEntity;
import org.cryptomator.data.db.mappers.VaultEntityMapper;
import org.cryptomator.data.util.DeploymentExtractor;
import org.cryptomator.domain.Cloud;
import org.cryptomator.domain.CloudType;
import org.cryptomator.domain.DropboxCloud;
import org.cryptomator.domain.GoogleDriveCloud;
import org.cryptomator.domain.LocalStorageCloud;
import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.exception.VaultAlreadyExistException;
import org.cryptomator.domain.models.deployment.DeploymentInfo;
import org.cryptomator.domain.models.deployment.VaultRemote;
import org.cryptomator.domain.models.userprofile.UserProfile;
import org.cryptomator.domain.repository.CloudRepository;
import org.cryptomator.domain.repository.VaultRepository;
import org.cryptomator.util.SharedPreferencesHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

import static org.cryptomator.domain.Vault.aCopyOf;

@Singleton
class VaultRepositoryImpl implements VaultRepository {

	private final Database database;
	private final VaultEntityMapper mapper;
	private final CryptoCloudContentRepositoryFactory cryptoCloudContentRepositoryFactory;
	private final DispatchingCloudContentRepository dispatchingCloudContentRepository;
	private final CryptoCloudFactory cryptoCloudFactory;
	private final SharedPreferencesHandler sharedPreferencesHandler;
	private final CloudRepository cloudRepository;
	private final Context context;

	@Inject
	public VaultRepositoryImpl(VaultEntityMapper mapper, CryptoCloudContentRepositoryFactory cryptoCloudContentRepositoryFactory, CryptoCloudFactory cryptoCloudFactory, DispatchingCloudContentRepository dispatchingCloudContentRepository, Database database, SharedPreferencesHandler sharedPreferencesHandler, CloudRepository cloudRepository, Context context) {
		this.mapper = mapper;
		this.database = database;
		this.cryptoCloudContentRepositoryFactory = cryptoCloudContentRepositoryFactory;
		this.cryptoCloudFactory = cryptoCloudFactory;
		this.dispatchingCloudContentRepository = dispatchingCloudContentRepository;
		this.sharedPreferencesHandler = sharedPreferencesHandler;
		this.cloudRepository = cloudRepository;
		this.context = context;
	}

	@Override
	public List<Vault> vaults() throws BackendException {
		try {
			List<Vault> result = new ArrayList<>();
			for (Vault vault : mapper.fromEntities(database.loadAll(VaultEntity.class))) {
				result.add(aCopyOf(vault).withUnlocked(isUnlocked(vault)).build());
			}

			String userCognitoId = sharedPreferencesHandler.getUserCognitoId();
			if (userCognitoId == null) {
				return result;
			}


			// filter vaults by userId
			List<Vault> filteredResult = new ArrayList<>();
			for (Vault vault : result) {
				String createdBy = vault.getCreatedBy();
				if (createdBy != null && createdBy.equals(userCognitoId)) {
					filteredResult.add(vault);
				}
			}

			return filteredResult;
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	@Override
	public List<Vault> vaults(String deviceSerial) throws BackendException {
		try {
			List<Vault> result = new ArrayList<>();
			for (Vault vault : mapper.fromEntities(database.loadAll(VaultEntity.class))) {
				result.add(aCopyOf(vault).withUnlocked(isUnlocked(vault)).build());
			}


			String userCognitoId = sharedPreferencesHandler.getUserCognitoId();
			if (userCognitoId == null) {
				return result;
			}

			// filter vaults by userId
			List<Vault> filteredResult = new ArrayList<>();
			for (Vault vault : result) {
				String createdBy = vault.getCreatedBy();
				if (createdBy != null && createdBy.equals(userCognitoId)) {
					filteredResult.add(vault);
				}
			}

			return filteredResult;
		} catch (Exception e) {
			return new ArrayList<>();
		}
	}

	@Override
	public Vault store(Vault vault) throws BackendException {
		try {
			return mapper.fromEntity(database.store(mapper.toEntity(vault))).withCreatedBy(sharedPreferencesHandler.getUserCognitoId()).withArgs(vault.getEtag(), vault.isUnlocked());
		} catch (SQLiteConstraintException e) {
			throw new VaultAlreadyExistException();
		}
	}

	@Override
	public Long delete(Vault vault) throws BackendException {
		deregisterUnlocked(vault);
		dispatchingCloudContentRepository.removeCloudContentRepositoryFor(cryptoCloudFactory.decryptedViewOf(vault));
		database.delete(mapper.toEntity(vault));
		return vault.getId();
	}

	@Override
	public Long deleteFromDB(Vault vault) throws BackendException {
		deregisterUnlocked(vault);
		database.delete(mapper.toEntity(vault));
		return vault.getId();
	}

	@Override
	public Vault load(Long id) throws BackendException {
		Vault vault = mapper.fromEntity(database.load(VaultEntity.class, id));
		return aCopyOf(vault).withUnlocked(isUnlocked(vault)).build();
	}

	private void deregisterUnlocked(Vault vault) {
		if (isUnlocked(vault)) {
			cryptoCloudContentRepositoryFactory.deregisterCryptor(vault);
		}
	}

	private boolean isUnlocked(Vault vault) {
		return cryptoCloudContentRepositoryFactory.cryptorIsRegisteredFor(vault);
	}

	@Override
	public void assertUnlocked(Vault vault) {
		cryptoCloudContentRepositoryFactory.assertCryptorRegisteredFor(vault);
	}

	@Override
	public Vault importVault(DeploymentInfo deployment, String computerId) throws BackendException {

			String cloudPath = deployment.getVaultRemote().getCloudPath();

			DeploymentExtractor extractor = new DeploymentExtractor();

			String deploymentType = extractor.extractCloudType(Objects.requireNonNull(cloudPath));

			Timber.tag("VaultRepository").d("Deployment type: %s", deploymentType);

			return switch (deploymentType) {
				case "dropbox" -> importDropbox(deployment,computerId);
				case "google_drive" -> importGoogleDrive(deployment,computerId);
				case "local", "file" -> importLocal(deployment,computerId);
				default -> throw new BackendException("Unsupported cloud type: " + deploymentType);
			};
	}

	@Override
	public Vault importLocal(DeploymentInfo deployment, String computerId) throws BackendException {
		try {
			// get the computer with cloudPath is not empty and volumePath is not empty
			String mVolumePath = deployment.getVaultRemote().getVolumePath();

			Optional<String> volumePath = Optional.of(Objects.requireNonNull(mVolumePath));

			if (!volumePath.isPresent()) {
				throw new BackendException("No valid volume path found");
			}

			String vaultName = deployment.getVaultRemote().getVolumeName();
			String baseFolder = volumePath.get();
			String vaultFolderName = vaultName;
			String folderPath = "/" + vaultFolderName;
			String folderName = vaultFolderName;

			File baseDir = new File(Environment.getExternalStorageDirectory(), baseFolder);
			if (!baseDir.exists()) {
				boolean created = baseDir.mkdirs();
				if (!created) {
					throw new BackendException("Failed to create base vault directory");
				}
			}

			ensureDirectoryPermissions(baseDir);
			File vaultDir = baseDir;
			ensureDirectoryPermissions(vaultDir);
			if (!vaultDir.canRead() || !vaultDir.canWrite()) {
				throw new BackendException("Cannot read/write to vault directory");
			}

			// Save vault files
			byte[] vaultMKData = Base64.getDecoder().decode(deployment.getVaultInfo().getVaultMK());
			File masterkeyFile = new File(vaultDir, "mk.ncryptor");
			try (FileOutputStream fos = new FileOutputStream(masterkeyFile)) {
				fos.write(vaultMKData);
			}

			String vaultData = deployment.getVaultInfo().getVaultData();
			File vaultConfigFile = new File(vaultDir, "vault.ncryptor");
			try (FileOutputStream fos = new FileOutputStream(vaultConfigFile)) {
				fos.write(vaultData.getBytes());
			}

			if (!masterkeyFile.exists() || !vaultConfigFile.exists()) {
				throw new BackendException("Failed to create vault files");
			}

			String uriPath = baseFolder;
			if (uriPath.contains("/")) {
				uriPath = uriPath.substring(0, uriPath.lastIndexOf("/"));
			}

			String safUri = "content://com.android.externalstorage.documents/tree/primary%3A" + uriPath.replace("/", "%2F");

			Cloud cloud = LocalStorageCloud.aLocalStorage().withRootUri(safUri).build();
			cloud = cloudRepository.store(cloud);

			Vault vault = Vault.aVault()
					.thatIsNew()
					.withName(folderName).withDeviceID(deployment.getVaultRemote().getFullDeviceId())
					.withPath(folderPath)
					.withCloudType(CloudType.LOCAL)
					.withCloud(cloud).withUnlocked(false)
					.withFormat(-1).withShorteningThreshold(-1)
					.withVaultDescription(deployment.getVaultRemote().getVolumeDescription())
					.withCreatedBy(sharedPreferencesHandler.getUserCognitoId())
					.withPosition(vaults().size()).build();

			return store(vault);

		} catch (IOException e) {
			throw new BackendException("Failed to save vault files", e);
		}
	}

	@Override
	public Vault importDropbox(DeploymentInfo deployment, String computerId) throws BackendException {

		String cloudPath = deployment.getVaultRemote().getCloudPath();
		String volumePath = deployment.getVaultRemote().getVolumePath();

		DeploymentExtractor extractor = new DeploymentExtractor();
		String email = extractor.extractEmail(Objects.requireNonNull(cloudPath));

		Cloud dropboxCloud = DropboxCloud.aDropboxCloud().withUsername(email).build();

		Cloud savedCloud = cloudRepository.getOrCreate(dropboxCloud);


		Vault vault = Vault.aVault()
				.thatIsNew()
				.withName(deployment.getVaultRemote().getVolumeName())
				.withDeviceID(deployment.getVaultRemote().getFullDeviceId())
				.withPath(volumePath)
				.withCloudType(CloudType.DROPBOX)
				.withCloud(savedCloud)
				.withUnlocked(false)
				.withFormat(-1)
				.withShorteningThreshold(-1)
				.withPosition(vaults().size())
				.withCreatedBy(sharedPreferencesHandler.getUserCognitoId())
				.withVaultDescription(deployment.getVaultRemote().getVolumeDescription())
				.build();
		vault = store(vault);
		return vault;
	}

	@Override
	public Vault importGoogleDrive(DeploymentInfo deployment, String computerId) throws BackendException {

		String cloudPath = deployment.getVaultRemote().getCloudPath();
		String volumePath = deployment.getVaultRemote().getVolumePath();

		DeploymentExtractor extractor = new DeploymentExtractor();

		String email = extractor.extractEmail(Objects.requireNonNull(cloudPath));

		Cloud cloud = GoogleDriveCloud.aGoogleDriveCloud().withUsername(email).withId(10L).build();

		Cloud savedCloud = cloudRepository.getOrCreate(cloud);

		Vault vault = Vault.aVault()
				.thatIsNew().withName(deployment.getVaultRemote().getVolumeName())
				.withDeviceID(deployment.getVaultRemote().getFullDeviceId())
				.withPath(volumePath)
				.withCloudType(CloudType.GOOGLE_DRIVE)
				.withCloud(savedCloud)
				.withUnlocked(false)
				.withFormat(-1)
				.withShorteningThreshold(-1)
				.withPosition(vaults().size())
				.withCreatedBy(sharedPreferencesHandler.getUserCognitoId())
				.withVaultDescription(deployment.getVaultRemote().getVolumeDescription())
				.build();
		vault = store(vault);
		return vault;
	}

	private void ensureDirectoryPermissions(File directory) throws BackendException {
		if (!directory.exists() || !directory.canRead() || !directory.canWrite()) {
			throw new BackendException("Directory permission issues: " + directory.getAbsolutePath());
		}
	}

}
