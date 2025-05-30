package org.cryptomator.domain.usecases.user;

import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.models.userprofile.UserProfile;
import org.cryptomator.domain.repository.UserRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;

@UseCase
class RefreshToken {

	private final UserRepository repository;

	private String accessToken;

	public RefreshToken(UserRepository repository, @Parameter String refreshToken) {
		this.repository = repository;
		this.accessToken = refreshToken;
	}

	public String execute() throws BackendException {
		return repository.refreshToken(accessToken);
	}
}
