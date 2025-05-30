package org.cryptomator.domain.usecases.vault;

import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.repository.DeviceRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;
import org.cryptomator.util.VaultStatus;

@UseCase
class UpdateVaultStatus {

	private final DeviceRepository repository;

	private Vault vault;

	private VaultStatus newStatus;


	public UpdateVaultStatus(DeviceRepository repository,  @Parameter Vault vault, @Parameter VaultStatus newStatus) {
		this.repository = repository;
		this.vault = vault;
		this.newStatus = newStatus;
	}

	public Vault execute() throws BackendException {
		return repository.updateVaultIfNeeded(vault);
	}
}
