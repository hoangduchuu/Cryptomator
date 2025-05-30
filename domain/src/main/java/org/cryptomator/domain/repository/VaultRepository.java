package org.cryptomator.domain.repository;

import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.exception.MissingCryptorException;
import org.cryptomator.domain.models.deployment.DeploymentInfo;

import java.util.List;

public interface VaultRepository {

	List<Vault> vaults() throws BackendException;

	List<Vault> vaults(String deviceSerial) throws BackendException;

	Vault store(Vault vault) throws BackendException;

	Long delete(Vault vault) throws BackendException;
	Long deleteFromDB(Vault vault) throws BackendException;

	Vault load(Long id) throws BackendException;

	void assertUnlocked(Vault vault) throws MissingCryptorException;

	/**
	 * Imports a vault from deployment information
	 *
	 * @param deployment The deployment information containing vault details
	 * @param computerId
	 * @return The imported vault
	 * @throws BackendException if the import fails
	 */
	Vault importVault(DeploymentInfo deployment, String computerId) throws BackendException;
	Vault importLocal(DeploymentInfo deployment, String computerId) throws BackendException;

	Vault importDropbox(DeploymentInfo deployment, String computerId) throws BackendException;
	Vault importGoogleDrive(DeploymentInfo deployment, String computerId) throws BackendException;

}
