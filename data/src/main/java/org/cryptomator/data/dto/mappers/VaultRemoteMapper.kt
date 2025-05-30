package org.cryptomator.data.dto.mappers

import org.cryptomator.data.dto.VaultRemoteDTO
import org.cryptomator.domain.models.deployment.VaultRemote
import javax.inject.Singleton

@Singleton

class VaultRemoteMapper() {
    fun toDomain(dto: VaultRemoteDTO): VaultRemote {
        return VaultRemote(
            id = dto.id,
            deviceId = dto.deviceId,
            fullDeviceId = dto.fullDeviceId,
            serial = dto.serial,
            serialBarcode = dto.serialBarcode,
            volumeName = dto.volumeName,
            volumeDescription = dto.volumeDescription,
            owner = dto.owner?.let { owner ->
                VaultRemote.Owner(
                    id = owner.id,
                    name = owner.name,
                    email = owner.email
                )
            },
            version = dto.version,
            machineID = dto.machineID,
            computers = dto.computers?.map { computer ->
                VaultRemote.Computer(
                    hostname = computer.hostname,
                    serial = computer.serial,
                    platform = computer.platform,
                    distro = computer.distro,
                    release = computer.release,
                    build = computer.build,
                    kernel = computer.kernel,
                    codename = computer.codename,
                    arch = computer.arch,
                    volumePath = computer.volumePath,
                    cloudPath = computer.cloudPath,
                    clientVersion = computer.clientVersion,
                    logofile = computer.logofile,
                    fqdn = computer.fqdn,
                    latestUseDate = computer.latestUseDate,
                    vaultStatus = computer.vaultStatus
                )
            },
            status = dto.status,
            lastMpwdUpdated = dto.lastMpwdUpdated,
            lastUserEventDate = dto.lastUserEventDate,
			cloudPath = dto.cloudPath,
			volumePath = dto.volumePath,
			policyId = dto.policyId,
			computer = VaultRemote.Computer(
				hostname = dto.computer?.hostname,
				serial = dto.computer?.serial,
				platform = dto.computer?.platform,
				distro = dto.computer?.distro,
				release = dto.computer?.release,
				build = dto.computer?.build,
				kernel = dto.computer?.kernel,
				codename = dto.computer?.codename,
				arch = dto.computer?.arch,
				volumePath = dto.computer?.volumePath,
				cloudPath = dto.computer?.cloudPath,
				clientVersion = dto.computer?.clientVersion,
				logofile = dto.computer?.logofile,
				fqdn = dto.computer?.fqdn,
				latestUseDate = dto.computer?.latestUseDate,
				vaultStatus = dto.computer?.vaultStatus
			)

        )
    }

    fun toDto(domain: VaultRemote): VaultRemoteDTO {
        return VaultRemoteDTO(
            id = domain.id,
            deviceId = domain.deviceId,
            fullDeviceId = domain.fullDeviceId,
            serial = domain.serial,
            serialBarcode = domain.serialBarcode,
            volumeName = domain.volumeName,
            volumeDescription = domain.volumeDescription,
			cloudPath = domain.cloudPath,
			volumePath = domain.volumePath,
            owner = domain.owner?.let { owner ->
                VaultRemoteDTO.OwnerDTO(
                    id = owner.id,
                    name = owner.name,
                    email = owner.email
                )
            },
            version = domain.version,
            machineID = domain.machineID,
            computers = domain.computers?.map { computer ->
                VaultRemoteDTO.ComputerDTO(
                    hostname = computer.hostname,
                    serial = computer.serial,
                    platform = computer.platform,
                    distro = computer.distro,
                    release = computer.release,
                    build = computer.build,
                    kernel = computer.kernel,
                    codename = computer.codename,
                    arch = computer.arch,
                    volumePath = computer.volumePath,
                    cloudPath = computer.cloudPath,
                    clientVersion = computer.clientVersion,
                    logofile = computer.logofile,
                    fqdn = computer.fqdn,
                    latestUseDate = computer.latestUseDate,
                    vaultStatus = computer.vaultStatus
                )
            },
			computer = VaultRemoteDTO.ComputerDTO(
				hostname = domain.computer?.hostname,
				serial = domain.computer?.serial,
				platform = domain.computer?.platform,
				distro = domain.computer?.distro,
				release = domain.computer?.release,
				build = domain.computer?.build,
				kernel = domain.computer?.kernel,
				codename = domain.computer?.codename,
				arch = domain.computer?.arch,
				volumePath = domain.computer?.volumePath,
				cloudPath = domain.computer?.cloudPath,
				clientVersion = domain.computer?.clientVersion,
				logofile = domain.computer?.logofile,
				fqdn = domain.computer?.fqdn,
				latestUseDate = domain.computer?.latestUseDate,
				vaultStatus = domain.computer?.vaultStatus
			),
            status = domain.status,
            lastMpwdUpdated = domain.lastMpwdUpdated,
            lastUserEventDate = domain.lastUserEventDate,
        )
    }
} 