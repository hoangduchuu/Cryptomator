package org.cryptomator.domain.usecases.vault;

import org.cryptomator.domain.DeviceArgs;
import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.repository.DeploymentRepository;
import org.cryptomator.domain.repository.DeviceRepository;
import org.cryptomator.domain.repository.VaultRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;
import org.cryptomator.util.DeploymentStatus;

@UseCase
class LogDeviceEvent {
	private final DeviceRepository deviceRepository;
	private final DeviceArgs deviceArgs;
	private final String deviceId;
	private final String type;


	public LogDeviceEvent(DeviceRepository deviceRepository,@Parameter DeviceArgs deviceArgs, @Parameter String deviceId, @Parameter String type) {
		this.deviceRepository = deviceRepository;
		this.deviceId = deviceId;
		this.deviceArgs = deviceArgs;
		this.type = type;
	}

	public Object execute() throws BackendException {
		return deviceRepository.logDeviceEvent(deviceId,type, deviceArgs);
	}

}
