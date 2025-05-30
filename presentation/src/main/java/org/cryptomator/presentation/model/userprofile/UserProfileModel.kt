package org.cryptomator.presentation.model.userprofile


import org.cryptomator.domain.models.userprofile.Computer
import org.cryptomator.domain.models.userprofile.Geolocation
import org.cryptomator.domain.models.userprofile.Import
import org.cryptomator.domain.models.userprofile.Policy
import org.cryptomator.domain.models.userprofile.Subscription
import org.cryptomator.domain.models.userprofile.UserProfile
import java.io.Serializable

class UserProfileModel(
	private val userProfile: UserProfile
) : Serializable {

	fun getCognitoID(): String? {
		return userProfile.cognitoID
	}

	fun getPicturePreSignedURL(): String? {
		return userProfile.picturePreSignedURL
	}

	fun getLastEventUser(): Long? {
		return userProfile.lastEventUser
	}

	fun getPicturePreSignedURLExpiry(): Long? {
		if (userProfile.picturePreSignedURLExpiry == null) return 0
		return userProfile.picturePreSignedURLExpiry
	}

	fun getId(): String? {
		return userProfile.id
	}

	fun getLastLogin(): Long? {
		return userProfile.lastLogin
	}

	fun getAcquiredDate(): Long? {
		return userProfile.acquiredDate
	}

	fun formatAcquiredDate(): String {
		return userProfile.formatAcquiredDate()
	}

	fun getPolicyId(): String? {
		return userProfile.policyId
	}

	fun getAllowRead(): String? {
		return userProfile.allowRead
	}

	fun getCn(): String? {
		return userProfile.cn
	}

	fun isSsoUser(): Boolean {
		return userProfile.isSSoUser()
	}

	private val policyModels: List<PolicyModel> by lazy {
		userProfile.policies?.map { PolicyModel(it) } ?: emptyList()
	}

	private val subscriptionModel: SubscriptionModel? by lazy {
		userProfile.subscription?.let { SubscriptionModel(it) }
	}

	private val computerModels: List<ComputerModel> by lazy {
		userProfile.computers?.map { ComputerModel(it) } ?: emptyList()
	}

	fun getPolicies(): List<PolicyModel> {
		return policyModels
	}

	fun getSubscription(): SubscriptionModel? {
		return subscriptionModel
	}

	fun getUserName(): String? {
		return userProfile.userName
	}

	fun getLastEventDevice(): Long? {
		return userProfile.lastEventDevice
	}

	fun getEmail(): String? {
		return userProfile.email
	}

	fun getComputers(): List<ComputerModel> {
		return computerModels
	}

	fun toUserProfile(): UserProfile {
		return userProfile
	}

	fun shouldBlockCreateNewVault(vaultsCount: Int): Boolean {
		val isFreePlan = userProfile.subscription?.plan == "basic" || userProfile.subscription == null
		return isFreePlan && vaultsCount >= 1
	}
}

class PolicyModel(
	private val policy: Policy
) : Serializable {

	fun getId(): String? {
		return policy.id
	}

	fun getName(): String? {
		return policy.name
	}

	fun toPolicy(): Policy {
		return policy
	}
}

class ComputerModel(
	private val computer: Computer
) : Serializable {

	fun getDistro(): String? {
		return computer.distro
	}

	fun isUefi(): Boolean? {
		return computer.uefi
	}

	fun getFqdn(): String? {
		return computer.fqdn
	}

	fun getRelease(): String? {
		return computer.release
	}

	fun getKernel(): String? {
		return computer.kernel
	}

	fun getLogofile(): String? {
		return computer.logofile
	}

	fun getCodepage(): String? {
		return computer.codepage
	}

	fun getPlatform(): String? {
		return computer.platform
	}

	fun getHostname(): String? {
		return computer.hostname
	}

	fun getSerial(): String? {
		return computer.serial
	}

	fun getServicepack(): String? {
		return computer.servicepack
	}

	fun getBuild(): String? {
		return computer.build
	}

	fun getCodename(): String? {
		return computer.codename
	}

	fun getArch(): String? {
		return computer.arch
	}

	fun getClientVersion(): String? {
		return computer.clientVersion
	}

	fun isHypervisor(): Boolean? {
		return computer.hypervisor
	}

	fun isRemoteSession(): Boolean? {
		return computer.remoteSession
	}

	fun getImports(): List<ImportModel>? {
		return computer.imports?.map { ImportModel(it) }
	}

	fun getLastIp(): String? {
		return computer.lastIp
	}

	fun getGeolocation(): GeolocationModel? {
		return computer.geolocation?.let { GeolocationModel(it) }
	}

	fun toComputer(): Computer {
		return computer
	}
}

class ImportModel(
	private val import: Import
) : Serializable {

	fun getDeviceId(): String? {
		return import.deviceId
	}

	fun getStatus(): String? {
		return import.status
	}

	fun getLastModified(): Long? {
		return import.lastModified
	}

	fun getClientVersion(): String? {
		return import.clientVersion
	}

	fun getVolumePath(): String? {
		return import.volumePath
	}

	fun toImport(): Import {
		return import
	}
}

class GeolocationModel(
	private val geolocation: Geolocation
) : Serializable {

	fun getDate(): String? {
		return geolocation.date
	}

	fun getHostName(): String? {
		return geolocation.hostName
	}

	fun getCountry(): String? {
		return geolocation.country
	}

	fun getCity(): String? {
		return geolocation.city
	}

	fun getIsp(): String? {
		return geolocation.isp
	}

	fun getLatitude(): String? {
		return geolocation.latitude
	}

	fun getIpAddress(): String? {
		return geolocation.ipAddress
	}

	fun getPostal(): String? {
		return geolocation.postal
	}

	fun getRegion(): String? {
		return geolocation.region
	}

	fun getLongitude(): String? {
		return geolocation.longitude
	}

	fun toGeolocation(): Geolocation {
		return geolocation
	}
}

class SubscriptionModel(
	private val subscription: Subscription
) : Serializable {

	fun getStartEpochSecs(): Long? {
		return subscription.startEpochSecs
	}

	fun getPortalLink(): String? {
		return subscription.portalLink
	}

	fun getPlanName(): String? {
		return subscription.planName
	}

	fun getActive(): String? {
		return subscription.active
	}

	fun getNextRenewalAmount(): String? {
		return subscription.nextRenewalAmount ?: "0.0"
	}

	fun getTrialDaysLeft(): Long? {
		return subscription.trialDaysLeft
	}

	fun getUserSub(): String? {
		return subscription.userSub
	}

	fun getAccountID(): String? {
		return subscription.accountID
	}

	fun getLastUpdated(): Long? {
		return subscription.lastUpdated
	}

	fun getPortalLinkExpiry(): Long? {
		return subscription.portalLinkExpiry
	}

	fun getEndEpochSecs(): Long? {
		return subscription.endEpochSecs
	}

	fun getSubscriptionID(): String? {
		return subscription.subscriptionID
	}

	fun getPlan(): String? {
		return subscription.plan
	}

	fun getGateway(): String? {
		return subscription.gateway
	}

	fun getEmail(): String? {
		return subscription.email
	}

	fun toSubscription(): Subscription {
		return subscription
	}
}