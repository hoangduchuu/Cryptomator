package org.cryptomator.domain.usecases.vault;

import org.cryptomator.domain.CloudFolder;
import org.cryptomator.domain.DeviceArgs;
import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.exception.NoSuchCloudFileException;
import org.cryptomator.domain.exception.NoSuchVaultException;
import org.cryptomator.domain.exception.UpdateVaultRemoveExeption;
import org.cryptomator.domain.models.device.PostVault;
import org.cryptomator.domain.repository.CloudContentRepository;
import org.cryptomator.domain.repository.CloudRepository;
import org.cryptomator.domain.repository.DeviceRepository;
import org.cryptomator.domain.repository.VaultRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;

import kotlin.Pair;

import static org.cryptomator.domain.Vault.aCopyOf;
import static org.cryptomator.util.ExceptionUtil.contains;

@UseCase
class RenameVault {

	private final CloudContentRepository cloudContentRepository;
	private final CloudRepository cloudRepository;
	private final VaultRepository vaultRepository;
	private final DeviceRepository deviceRepository;
	private final String newVaultName;
	private final DeviceArgs deviceArgs;
	private Vault vault;

	public RenameVault(CloudContentRepository cloudContentRepository, CloudRepository cloudRepository, VaultRepository vaultRepository, DeviceRepository deviceRepository, @Parameter Vault vault, @Parameter String newVaultName, @Parameter DeviceArgs deviceArgs) {
		this.cloudContentRepository = cloudContentRepository;
		this.vaultRepository = vaultRepository;
		this.cloudRepository = cloudRepository;
		this.deviceRepository = deviceRepository;
		this.vault = vault;
		this.newVaultName = newVaultName;
		this.deviceArgs = deviceArgs;
	}

	public Vault execute() throws BackendException {
		try {
			CloudFolder vaultLocation = cloudContentRepository.resolve(vault.getCloud(), vault.getPath());
			CloudFolder vaultLocationAfterRename = cloudContentRepository.folder(vaultLocation.getParent(), newVaultName);
			cloudContentRepository.move(vaultLocation, vaultLocationAfterRename);

			if (vault.isUnlocked()) {
				cloudRepository.lock(vault);
				vault = Vault.aCopyOf(vault) //
						.withUnlocked(false).build();
			}
			
			// Update vault name with device repository
			boolean success = deviceRepository.renameVault(vault.getDeviceID(), vault.getId().toString(), newVaultName);
			if (!success) {
				throw new UpdateVaultRemoveExeption(vault, new Throwable("Failed to update vault name on device repository"));
			}
			
			Vault renamedVault = aCopyOf(vault) //
					.withNamePathAndCloudFrom(vaultLocationAfterRename) //
					.build();
			return vaultRepository.store(renamedVault);
		} catch (BackendException e) {
			if (contains(e, NoSuchCloudFileException.class)) {
				throw new NoSuchVaultException(vault, e);
			}
			throw e;
		}
	}
}
