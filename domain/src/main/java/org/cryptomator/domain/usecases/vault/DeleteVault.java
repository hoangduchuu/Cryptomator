package org.cryptomator.domain.usecases.vault;

import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.repository.DeploymentRepository;
import org.cryptomator.domain.repository.VaultRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;
import org.cryptomator.util.DeploymentStatus;

@UseCase
class DeleteVault {

	private final VaultRepository vaultRepository;
	private final Vault vault;


	private final DeploymentRepository deploymentRepository;

	private final String computerId;

	public DeleteVault(VaultRepository vaultRepository, DeploymentRepository deploymentRepository, @Parameter Vault vault,@Parameter String computerId) {
		this.vaultRepository = vaultRepository;
		this.vault = vault;
		this.deploymentRepository = deploymentRepository;
		this.computerId = computerId;
	}

	public Long execute() throws BackendException {
		deploymentRepository.updateVaultStatus(DeploymentStatus.Removed,computerId,vault.getDeviceID());
		return vaultRepository.delete(vault);
	}

}
