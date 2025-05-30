package org.cryptomator.domain.usecases.user;

import org.cryptomator.domain.DeviceArgs;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.models.userprofile.UserProfile;
import org.cryptomator.domain.repository.UserRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;

@UseCase
class Login {

	private final UserRepository repository;

	private String accessToken;

	private DeviceArgs deviceArgs;

	public Login(UserRepository repository,  @Parameter String accessToken, @Parameter DeviceArgs deviceArgs) {
		this.repository = repository;
		this.accessToken = accessToken;
		this.deviceArgs = deviceArgs;
	}

	public UserProfile execute() throws BackendException {
		return repository.login(accessToken,deviceArgs);
	}
}
