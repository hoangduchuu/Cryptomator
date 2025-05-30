package org.cryptomator.data.util

import org.cryptomator.domain.exception.BackendException
import org.cryptomator.domain.models.deployment.DeploymentInfo
import org.cryptomator.domain.models.deployment.VaultRemote

/**
 *     {
 *             "distro": "Android 16",
 *             "fqdn": "android-9bfcc910.local",
 *             "release": "16",
 *             "kernel": "6.6.66-android15-8-gb66429556fb8-ab13070261-4k",
 *             "logofile": "android",
 *             "volumePath": "/ncryptors/999/DROP999",
 *             "clientVersion": "1.12.0",
 *             "cloudPath": "dropbox://hdhuuvn@gmail.com@/ncryptors/999/DROP999",
 *             "platform": "android",
 *             "hostname": "HuuADR001",
 *             "serial": "9bfcc910-04de-3fec-8d6d-ca1d5d5e6ce3",
 *             "build": "BP22.250325.006",
 *             "codename": "Unknown",
 *             "arch": "arm64",
 *             "latestUseDate": 1747435219620
 *         }
 */
class DeploymentExtractor {

	fun extractEmail(deployment: String): String {
		val regex = Regex("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+)")
		val matchResult = regex.find(deployment)
		val email = matchResult?.value ?: return ""
		return email;
	}

	fun extractEmail(deployment: DeploymentInfo): String {
		try{
			val computer: VaultRemote.Computer = deployment.vaultRemote?.computers?.stream()
				?.filter { computer1: VaultRemote.Computer -> computer1.cloudPath != null && computer1.cloudPath!!.isNotEmpty() }
				?.filter { computer1: VaultRemote.Computer -> computer1.volumePath != null && computer1.volumePath!!.isNotEmpty() }
				?.findFirst()
				?.orElseThrow { BackendException("No valid computer found") } ?: return ""
			val regex = Regex("([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+)")
			val matchResult = regex.find("${computer.cloudPath}")
			val email = matchResult?.value ?: return ""
			return email;
		}catch (e: Exception) {
			return "";
		}
	}

	fun extractCloudType(url: String): String {
		// Handle empty or null input
		if (url.isBlank()) return ""
		
		// Extract the protocol part before the first @ or /
		val regex = Regex("^([a-zA-Z0-9_]+)://")
		val matchResult = regex.find(url)
		val cloudType = matchResult?.value?.removeSuffix("://")?.lowercase() ?: return ""
		
		// Map the cloud type to the expected format
		return when (cloudType) {
			"google_drive" -> "google_drive"
			"dropbox" -> "dropbox"
			"local" -> "local"
			"file" -> "file"
			else -> cloudType
		}
	}

	fun extractCloudType(deployment: DeploymentInfo): String {
		try {
			val computer: VaultRemote.Computer = deployment.vaultRemote?.computers?.stream()
				?.filter { computer1: VaultRemote.Computer -> computer1.cloudPath != null && computer1.cloudPath!!.isNotEmpty() }
				?.filter { computer1: VaultRemote.Computer -> computer1.volumePath != null && computer1.volumePath!!.isNotEmpty() }
				?.findFirst()
				?.orElseThrow { BackendException("No valid computer found") } ?: return ""
			return extractCloudType(computer.cloudPath ?: "")
		} catch (e: Exception) {
			return ""
		}
	}

}

