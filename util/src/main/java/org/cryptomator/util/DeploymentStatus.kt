package org.cryptomator.util

/**
 * ADD_PENDING
 * ADDED
 * REMOVED
 * REMOVE_PENDING
 */
enum class DeploymentStatus {

	AddPending, Added, Removed, RemovePending, Unknown;
}

fun DeploymentStatus.shouldGetDeployment(): Boolean {
	return when (this) {
		DeploymentStatus.AddPending -> true
		else -> false
	}
}


class DeploymentStatusParser {
	companion object {

		fun parse(status: String?): DeploymentStatus {
			return if (status != null) {
				when (status) {
					"ADD_PENDING" -> DeploymentStatus.AddPending
					"ADDED" -> DeploymentStatus.Added
					"REMOVED" -> DeploymentStatus.Removed
					"REMOVE_PENDING" -> DeploymentStatus.RemovePending
					else -> DeploymentStatus.Unknown
				}
			} else {
				DeploymentStatus.Unknown
			}
		}

		fun toString(status: DeploymentStatus): String {
			return when (status) {
				DeploymentStatus.AddPending -> "ADD_PENDING"
				DeploymentStatus.Added -> "ADDED"
				DeploymentStatus.Removed -> "REMOVED"
				DeploymentStatus.RemovePending -> "REMOVE_PENDING"
				else -> "UNKNOWN"
			}
		}
	}

}
