package org.cryptomator.domain.usecases.vault;

import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.models.deployment.VaultRemote;
import org.cryptomator.domain.repository.DeviceRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;
import org.cryptomator.util.VaultStatus;

@UseCase
class GetVaultRemoteDetail {

	private final DeviceRepository repository;
	private String deviceId;

	public GetVaultRemoteDetail(DeviceRepository repository, @Parameter String deviceId) {
		this.repository = repository;
		this.deviceId = deviceId;
	}

	public VaultRemote execute() throws BackendException {
		return repository.getVaultRemote(deviceId);
	}
}
