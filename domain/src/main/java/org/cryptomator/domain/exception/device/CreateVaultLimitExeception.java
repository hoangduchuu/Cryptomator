package org.cryptomator.domain.exception.device;

import org.cryptomator.domain.exception.BackendException;

public class CreateVaultLimitExeception extends BackendException {

	public CreateVaultLimitExeception(final String message) {
		super(message);
	}

	public CreateVaultLimitExeception(final String message, final Exception e) {
		super(message, e);
	}

	public CreateVaultLimitExeception(Exception e) {
		super(e);
	}
}
