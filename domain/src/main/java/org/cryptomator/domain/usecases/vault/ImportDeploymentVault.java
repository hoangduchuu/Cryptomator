package org.cryptomator.domain.usecases.vault;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.Manifest;

import androidx.core.content.ContextCompat;

import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.models.deployment.DeploymentInfo;
import org.cryptomator.domain.models.deployment.DeploymentWithStatus;
import org.cryptomator.domain.repository.DeploymentRepository;
import org.cryptomator.domain.repository.VaultRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;
import org.cryptomator.util.DeploymentStatus;
import org.cryptomator.util.SharedPreferencesHandler;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import timber.log.Timber;

@UseCase
public class ImportDeploymentVault {

	private final VaultRepository vaultRepository;
	private final DeploymentRepository deploymentRepository;
	private final DeploymentWithStatus deployment;
	private final SharedPreferencesHandler sharedPreferencesHandler;
	private final String computerId;
	private final Context context;

	public enum ImportResult {
		ADDED, UPDATED, REMOVED, SKIPPED, ERROR
	}

	@Inject
	public ImportDeploymentVault(SharedPreferencesHandler sharedPreferencesHandler, VaultRepository vaultRepository, DeploymentRepository deploymentRepository, Context context, @Parameter DeploymentWithStatus deployment, @Parameter String computerId) {
		this.vaultRepository = vaultRepository;
		this.context = context;
		this.deployment = deployment;
		this.sharedPreferencesHandler = sharedPreferencesHandler;
		this.deploymentRepository = deploymentRepository;
		this.computerId = computerId;
	}

	public ImportResult execute() throws Exception {
		try {
			String vaultId = Objects.requireNonNull(deployment.getDeploymentInfo().getVaultRemote().getFullDeviceId());
			DeploymentStatus remoteStatus = deployment.getStatus();
			List<Vault> existingVaults = vaultRepository.vaults();
			Optional<Vault> existingVault = existingVaults.stream()
				.filter(v -> v.getDeviceID().equals(vaultId))
				.findFirst();

			if (existingVault.isPresent()) {
				// Local EXISTS
				return handleExistingVault(existingVault.get(), vaultId, remoteStatus);
			} else {
				// Local NOT EXISTS
				return handleNewVault(deployment, vaultId, remoteStatus);
			}
		} catch (Exception e) {
			Timber.e(e, "Failed to import deployment vault");
			return ImportResult.ERROR;
		}
	}

	private ImportResult handleExistingVault(Vault vault, String vaultId, DeploymentStatus remoteStatus) {
		Timber.tag("ImportDeploymentVault").d("Handling existing vault: %s with remote status: %s", vault.getName(), remoteStatus);
		switch (remoteStatus) {
			case AddPending:
				deploymentRepository.updateVaultStatus(DeploymentStatus.Added, computerId, vaultId);
				return ImportResult.UPDATED;
			case Added:
				return ImportResult.SKIPPED;
			case RemovePending:
				try {
					vaultRepository.deleteFromDB(vault);
				} catch (BackendException e) {
					Timber.e(e, "Failed to delete vault: %s", vaultId);
					return ImportResult.ERROR;
				}
				deploymentRepository.updateVaultStatus(DeploymentStatus.Removed, computerId, vaultId);
				return ImportResult.REMOVED;
			case Removed:
				try {
					vaultRepository.deleteFromDB(vault);
				} catch (BackendException e) {
					Timber.e(e, "Failed to delete vault: %s", vaultId);
					return ImportResult.ERROR;
				}
				return ImportResult.REMOVED;
			default:
				Timber.w("Unknown remote status for existing vault: %s", remoteStatus);
				return ImportResult.SKIPPED;
		}
	}

	private ImportResult handleNewVault(DeploymentWithStatus deployment, String vaultId, DeploymentStatus remoteStatus) {
		switch (remoteStatus) {
			case AddPending:
			case Added:
				if (deployment.getDeploymentInfo().getVaultRemote().isSupportImportVault()) {
					try {
						vaultRepository.importVault(deployment.getDeploymentInfo(), computerId);
						deploymentRepository.updateVaultStatus(DeploymentStatus.Added, computerId, vaultId);
						return ImportResult.ADDED;
					} catch (BackendException e) {
						Timber.e(e, "Failed to import new vault: %s", vaultId);
						return ImportResult.ERROR;
					}
				} else {
					Timber.d("Vault does not support import: %s", vaultId);
					return ImportResult.SKIPPED;
				}
			case RemovePending:
			case Removed:
				return ImportResult.SKIPPED;
			default:
				Timber.w("Unknown remote status for new vault: %s", remoteStatus);
				return ImportResult.SKIPPED;
		}
	}

	private boolean hasStoragePermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			return Environment.isExternalStorageManager();
		} else {
			return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
		}
	}
}