package org.cryptomator.domain.usecases.user;

import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.models.userprofile.UserProfile;
import org.cryptomator.domain.repository.UserRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;

import java.io.File;

import timber.log.Timber;

@UseCase
class GetUserAvatar {

	private final UserRepository repository;
	private String accessToken;
	private String userId;
	private UserProfile userProfile;

	public GetUserAvatar(UserRepository repository, @Parameter String accessToken, @Parameter String userId, @Parameter UserProfile userProfile) {
		this.repository = repository;
		this.accessToken = accessToken;
		this.userId = userId;
		this.userProfile = userProfile;

	}

	public Object execute() throws BackendException {
		Object response = repository.getUserAvatar(accessToken, userProfile);
		return response;
	}
} 