package org.cryptomator.data.dto.mappers

import org.cryptomator.data.dto.DeploymentResponseDTO
import org.cryptomator.domain.models.deployment.Deployment
import org.cryptomator.domain.models.deployment.GetDeploymentResponse

class DeploymentResponseMapper {

	fun toModel(dto: DeploymentResponseDTO): GetDeploymentResponse {
		return GetDeploymentResponse(
			deployments = dto.deployments.orEmpty().map { toDeployment(it) },
		)
	}

	private fun toDeployment(dto: DeploymentResponseDTO.Deployment): Deployment {
		return Deployment(
			lastModified = dto.lastModified,
			deviceId = dto.deviceId ?: "",
			status = dto.status?: "",
		)
	}

}