package org.cryptomator.domain.models.deployment

import org.cryptomator.util.DeploymentStatus
import java.io.Serializable

data class DeploymentWithStatus(
    val deploymentInfo: DeploymentInfo?,
    val status: DeploymentStatus
) : Serializable 