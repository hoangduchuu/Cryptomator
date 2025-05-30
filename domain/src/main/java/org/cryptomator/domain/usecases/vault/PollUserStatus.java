package org.cryptomator.domain.usecases.vault;

import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.repository.DeviceRepository;
import org.cryptomator.domain.repository.PollResponse;
import org.cryptomator.domain.repository.UserRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;

@UseCase
class PollUserStatus {

	private final UserRepository repository;

	private String userId;


	public PollUserStatus(UserRepository repository,  @Parameter String userId) {
		this.repository = repository;
		this.userId = userId;
	}

	public PollResponse execute() throws BackendException {
		return repository.getPollUserStatus(userId);
	}
}
