package org.cryptomator.domain.usecases.vault;

import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.repository.DeviceRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;

@UseCase
class PollVaultPolicyId {

	private final DeviceRepository repository;

	private Vault vault;


	public PollVaultPolicyId(DeviceRepository repository,  @Parameter Vault vault) {
		this.repository = repository;
		this.vault = vault;
	}

	public Vault execute() throws BackendException {
		return repository.updateVaultPolicyIdIfNeeded(vault);
	}
}
