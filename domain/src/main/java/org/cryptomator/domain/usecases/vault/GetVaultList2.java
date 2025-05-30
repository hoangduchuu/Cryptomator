package org.cryptomator.domain.usecases.vault;

import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.repository.VaultRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;

import java.util.List;

import kotlin.ParameterName;

@UseCase
class GetVaultList2 {

	private final VaultRepository vaultRepository;
	String deviceSerial;

	public GetVaultList2(VaultRepository vaultRepository,@Parameter String deviceSerial) {
		this.vaultRepository = vaultRepository;
		this.deviceSerial = deviceSerial;
	}

	public List<Vault> execute() throws BackendException {
		return vaultRepository.vaults(deviceSerial);
	}

}
