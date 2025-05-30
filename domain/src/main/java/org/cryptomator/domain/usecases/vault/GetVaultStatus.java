package org.cryptomator.domain.usecases.vault;

import org.cryptomator.domain.DeviceArgs;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.models.userprofile.UserProfile;
import org.cryptomator.domain.repository.DeviceRepository;
import org.cryptomator.domain.repository.UserRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;
import org.cryptomator.util.VaultStatus;

@UseCase
class GetVaultStatus {

	private final DeviceRepository repository;

	private String deviceId;


	public GetVaultStatus(DeviceRepository repository,  @Parameter String deviceId) {
		this.repository = repository;
		this.deviceId = deviceId;
	}

	public VaultStatus execute() throws BackendException {
		return repository.getDeviceStatus(deviceId);
	}
}
