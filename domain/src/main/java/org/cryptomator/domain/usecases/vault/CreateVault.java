package org.cryptomator.domain.usecases.vault;

import android.util.Pair;

import org.cryptomator.domain.CloudFolder;
import org.cryptomator.domain.DeviceArgs;
import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.models.device.PostVault;
import org.cryptomator.domain.repository.CloudContentRepository;
import org.cryptomator.domain.repository.CloudRepository;
import org.cryptomator.domain.repository.DeviceRepository;
import org.cryptomator.domain.repository.VaultRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;
import org.cryptomator.util.SharedPreferencesHandler;

import timber.log.Timber;

import static org.cryptomator.domain.Vault.aVault;

@UseCase
class CreateVault {

	private final CloudContentRepository cloudContentRepository;
	private final DeviceRepository deviceRepository;
	private final CloudRepository cloudRepository;
	private final VaultRepository vaultRepository;
	private final CloudFolder folder;
	private final String vaultName;
	private final String vaultDescription;
	private final String password;
	private final DeviceArgs deviceArgs;

	private final String userId;

	SharedPreferencesHandler preferencesHandler;

	public CreateVault(DeviceRepository deviceRepository, CloudContentRepository cloudContentRepository, VaultRepository vaultRepository, CloudRepository cloudRepository, SharedPreferencesHandler sharedPreferencesHandler, @Parameter CloudFolder folder, @Parameter String vaultName, @Parameter String password, @Parameter DeviceArgs deviceArgs,@Parameter String userId, @Parameter String vaultDescription) {
		this.deviceRepository = deviceRepository;
		this.cloudContentRepository = cloudContentRepository;
		this.vaultRepository = vaultRepository;
		this.cloudRepository = cloudRepository;
		this.folder = folder;
		this.vaultName = vaultName;
		this.password = password;
		this.deviceArgs = deviceArgs;
		this.userId = userId;
		this.vaultDescription = vaultDescription;
		this.preferencesHandler = sharedPreferencesHandler;
		
		Timber.d("CreateVault: Initialized with folder: %s, vaultName: %s", 
				folder != null ? folder.toString() : "null", 
				vaultName);
		if (folder != null) {
			Timber.d("CreateVault: Folder details - Name: %s, Path: %s", 
					folder.getName(), folder.getPath());
			Timber.d("CreateVault: Folder cloud: %s", folder.getCloud().toString());
		}
	}

	public Vault execute() throws BackendException {
		Timber.d("CreateVault: Starting vault creation process");
		
		Timber.d("CreateVault: Creating vault folder reference with name: %s in parent folder: %s", 
				vaultName, folder != null ? folder.getPath() : "null");
		CloudFolder vaultFolder = cloudContentRepository.folder(folder, vaultName);
		Timber.d("CreateVault: Vault folder reference created: %s (Name: %s, Path: %s)", 
				vaultFolder.toString(), vaultFolder.getName(), vaultFolder.getPath());
		
		Timber.d("CreateVault: Creating actual folder in cloud storage");
		vaultFolder = cloudContentRepository.create(vaultFolder);
		Timber.d("CreateVault: Vault folder created in cloud: %s (Name: %s, Path: %s)", 
				vaultFolder.toString(), vaultFolder.getName(), vaultFolder.getPath());
		
		Timber.d("CreateVault: Creating vault in cloud repository with password");
		cloudRepository.create(vaultFolder, password);
		Timber.d("CreateVault: Vault created in cloud repository");
		
		Timber.d("CreateVault: Posting vault to device repository");
		Pair<String, PostVault> response = deviceRepository.postVault(vaultFolder, deviceArgs, vaultDescription);
		Timber.d("CreateVault: Vault posted to device repository, got deviceID: %s", response.first);
		
		Timber.d("CreateVault: Building Vault object");
		Vault vault = aVault() //
				.thatIsNew() //
				.withNamePathAndCloudFrom(vaultFolder) //
				.withPosition(vaultRepository.vaults().size()) //
				.withDeviceID(response.first) //
				.withCreatedBy(preferencesHandler.getUserCognitoId())
				.withVaultDescription(vaultDescription)
				.withEtag("")
				.withStatus("")
				.build();
		Timber.d("CreateVault: Storing vault: %s at path: %s", vault.getName(), vault.getPath());
		
		vault = vaultRepository.store(vault);
		Timber.d("CreateVault: Vault stored successfully with ID: %s", vault.getId());
		
		return vault;
	}
}
