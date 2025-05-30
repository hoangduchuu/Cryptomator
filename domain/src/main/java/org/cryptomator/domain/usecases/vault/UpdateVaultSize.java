package org.cryptomator.domain.usecases.vault;

import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.repository.VaultRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;

@UseCase
class UpdateVaultSize {

	private final VaultRepository vaultRepository;
	private final Vault vault;
	private final Long size;

	public UpdateVaultSize(VaultRepository vaultRepository, @Parameter Vault vault, @Parameter Long size) {
		this.vaultRepository = vaultRepository;
		this.vault = vault;
		this.size = size;
	}

	public Vault execute() throws BackendException {
		if (size == null || !size.equals(vault.getSize())) {
			Vault updatedVault = vault.withNewSize(size != null ? size : 0L);
			return vaultRepository.store(updatedVault);
		}
		return vault;
	}
} 