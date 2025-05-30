package org.cryptomator.data.dto.mappers

import org.cryptomator.data.dto.DeviceActivationDTO
import org.cryptomator.domain.models.device.DeviceActivation

class DeviceActivationMapper {

	fun mapToDeviceActivationDTO(
		jwtTokenUnlocked: String,
		message: String,
		deviceId: String,
		jwtTokenLocked: String
	): DeviceActivationDTO {
		return DeviceActivationDTO(
			jwtTokenUnlocked = jwtTokenUnlocked,
			message = message,
			deviceId = deviceId,
			jwtTokenLocked = jwtTokenLocked
		)
	}

	fun mapToDeviceActivation(
		deviceActivationDTO: DeviceActivationDTO
	): DeviceActivation {
		return DeviceActivation(
			jwtTokenUnlocked = deviceActivationDTO.jwtTokenUnlocked,
			message =deviceActivationDTO. message,
			deviceId = deviceActivationDTO.deviceId,
			jwtTokenLocked = deviceActivationDTO.jwtTokenLocked
		)
	}

}