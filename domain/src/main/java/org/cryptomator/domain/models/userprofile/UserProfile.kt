package org.cryptomator.domain.models.userprofile

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class UserProfile(
	@SerializedName("cognitoId")
	val cognitoID: String? = null,

	val id: String? = null,
	val userName: String? = null,
	val email: String? = null,
	val lastLogin: Long? = null,
	val lastEventUser: Long? = null,
	val lastEventDevice: Long? = null,

	@SerializedName("picturePreSignedUrl")
	val picturePreSignedURL: String? = null,

	@SerializedName("picturePreSignedUrlExpiry")
	val picturePreSignedURLExpiry: Long? = null,

	val acquiredDate: Long? = null,
	val policyId: String? = null,
	val allowRead: String? = null,
	val cn: String? = null,
	val policies: List<Policy>? = null,
	val subscription: Subscription? = null,
	val computers: List<Computer>? = null
) : Serializable {

	val currentPlan: String
		get() = subscription?.plan ?: "basic"

	fun formatAcquiredDate(): String {
		return acquiredDate?.let { java.text.SimpleDateFormat("dd MMMM yyyy").format(java.util.Date(it)) } ?: ""
	}

	fun isSSoUser():Boolean{
		return userName?.startsWith("google_") == true || picturePreSignedURL.toString().startsWith("https://lh3.googleusercontent.com")
	}
}

data class Policy(
	val id: String? = null,
	val name: String? = null
) : Serializable

data class Computer(
	val distro: String? = null,
	val uefi: Boolean? = null,
	val fqdn: String? = null,
	val release: String? = null,
	val kernel: String? = null,
	val logofile: String? = null,
	val codepage: String? = null,
	val platform: String? = null,
	val hostname: String? = null,
	val serial: String? = null,
	val servicepack: String? = null,
	val build: String? = null,
	val codename: String? = null,
	val arch: String? = null,
	val clientVersion: String? = null,
	val hypervisor: Boolean? = null,
	val remoteSession: Boolean? = null,
	val imports: List<Import>? = null,
	val lastIp: String? = null,
	val geolocation: Geolocation? = null
) : Serializable

data class Import(
	val deviceId: String? = null,
	val status: String? = null,
	val lastModified: Long? = null,
	val clientVersion: String? = null,
	val volumePath: String? = null
) : Serializable

data class Geolocation(
	val date: String? = null,
	val hostName: String? = null,
	val country: String? = null,
	val city: String? = null,
	val isp: String? = null,
	val latitude: String? = null,
	val ipAddress: String? = null,
	val postal: String? = null,
	val region: String? = null,
	val longitude: String? = null
) : Serializable

data class Subscription(
	val startEpochSecs: Long? = null,
	val portalLink: String? = null,
	val planName: String? = null,
	val active: String? = null,
	val nextRenewalAmount: String? = null,
	val trialDaysLeft: Long? = null,
	val userSub: String? = null,

	@SerializedName("accountId")
	val accountID: String? = null,

	val lastUpdated: Long? = null,
	val portalLinkExpiry: Long? = null,
	val endEpochSecs: Long? = null,

	@SerializedName("subscriptionId")
	val subscriptionID: String? = null,

	val plan: String? = null,
	val gateway: String? = null,
	val email: String? = null
) : Serializable