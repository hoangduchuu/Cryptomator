package org.cryptomator.domain.usecases.vault;

import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.domain.models.deployment.DeploymentInfo;
import org.cryptomator.domain.models.deployment.DeploymentStatusInfo;
import org.cryptomator.domain.models.deployment.DeploymentWithStatus;
import org.cryptomator.domain.repository.DeploymentRepository;
import org.cryptomator.generator.Parameter;
import org.cryptomator.generator.UseCase;
import org.cryptomator.util.DeploymentStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@UseCase
class GetDeploymentInfo {

	private final DeploymentRepository repository;
	String deviceSerial;

	public GetDeploymentInfo(DeploymentRepository repository, @Parameter String deviceSerial) {
		this.repository = repository;
		this.deviceSerial = deviceSerial;
	}

	public List<DeploymentWithStatus> execute() throws BackendException {
		// 1. Fetch all deployment statuses (lightweight)
		List<DeploymentStatusInfo> allStatuses = repository.getDeploymentStatusInfo(deviceSerial);

		// 2. Order by status: AddPending, Added, RemovePending, Removed
		List<DeploymentStatusInfo> ordered = allStatuses.stream()
			.sorted(Comparator.comparingInt(statusInfo -> {
				switch (statusInfo.getStatus()) {
					case AddPending: return 1;
					case Added: return 2;
					case RemovePending: return 3;
					case Removed: return 4;
					default: return 5;
				}
			}))
			.collect(Collectors.toList());

		// 3. For each, decide if we need to fetch full API data
		List<DeploymentWithStatus> result = new ArrayList<>();
		for (DeploymentStatusInfo statusInfo : ordered) {
			boolean localExists = repository.vaultExistsLocally(statusInfo.getVaultId());
			if (statusInfo.getStatus() == DeploymentStatus.AddPending) {
				// Always fetch full data for AddPending
				DeploymentWithStatus full = repository.getDeploymentWithStatus(deviceSerial, statusInfo.getVaultId());
				result.add(full);
			} else if (statusInfo.getStatus() == DeploymentStatus.Added && !localExists) {
				// Fetch full data for Added only if local does not exist
				DeploymentWithStatus full = repository.getDeploymentWithStatus(deviceSerial, statusInfo.getVaultId());
				result.add(full);
			} else if ((statusInfo.getStatus() == DeploymentStatus.RemovePending || statusInfo.getStatus() == DeploymentStatus.Removed) && localExists) {
				// For RemovePending/Removed, if local exists, add a minimal DeploymentWithStatus (no API call)
				result.add(new DeploymentWithStatus(
					DeploymentInfo.Companion.minimal(statusInfo.getVaultId()),
					statusInfo.getStatus()
				));
			}
			// Unknown: do not fetch full data
		}
		return result;
	}
}
