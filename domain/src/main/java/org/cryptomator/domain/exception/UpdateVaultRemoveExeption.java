package org.cryptomator.domain.exception;

import org.cryptomator.domain.Vault;

public class UpdateVaultRemoveExeption extends BackendException {

	private final Vault vault;

	public UpdateVaultRemoveExeption(Vault vault, Throwable cause) {
		super(cause);
		this.vault = vault;
	}

	public Vault getVault() {
		return vault;
	}
}
