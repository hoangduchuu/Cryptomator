package org.cryptomator.domain.usecases.vault;

import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.repository.VaultRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;

@UseCase
class UpdateVaultEtag {

	private final VaultRepository vaultRepository;
	private final Vault vault;

	private final String etag;

	public UpdateVaultEtag(VaultRepository vaultRepository, @Parameter Vault vault,@Parameter String etag) {
		this.vaultRepository = vaultRepository;
		this.vault = vault;
		this.etag = etag;
	}

	public Vault execute() throws BackendException {
		Vault updatedVault = vault.withNewEtag(etag);
		return vaultRepository.store(updatedVault);
	}

}
