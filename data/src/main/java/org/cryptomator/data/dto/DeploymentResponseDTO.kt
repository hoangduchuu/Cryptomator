package org.cryptomator.data.dto

import com.google.gson.annotations.SerializedName

class DeploymentResponseDTO {

	// Getters
	@SerializedName("lastLogin")
	val lastLogin: Long = 0

	@SerializedName("cognitoId")
	val cognitoId: String? = null

	@SerializedName("deployments")
	val deployments: List<Deployment>? = null

	@SerializedName("lastEventUser")
	val lastEventUser: Long = 0

	@SerializedName("acquiredDate")
	val acquiredDate: Long = 0

	@SerializedName("policyId")
	val policyId: String? = null

	@SerializedName("cn")
	val cn: String? = null

	@SerializedName("subscription")
	val subscription: Subscription? = null

	@SerializedName("id")
	val id: String? = null

	@SerializedName("userName")
	val userName: String? = null

	@SerializedName("lastEventDevice")
	val lastEventDevice: Long = 0

	@SerializedName("email")
	val email: String? = null

	class Deployment {

		@SerializedName("lastModified")
		val lastModified: Long = 0

		@SerializedName("deviceId")
		val deviceId: String? = null

		@SerializedName("status")
		val status: String? = null
	}

	class Subscription {

		// Getters
		@SerializedName("startEpochSecs")
		val startEpochSecs: Long = 0

		@SerializedName("portalLink")
		val portalLink: String? = null

		@SerializedName("planName")
		val planName: String? = null

		@SerializedName("active")
		val active: String? = null

		@SerializedName("nextRenewalAmount")
		val nextRenewalAmount: String? = null

		@SerializedName("trialDaysLeft")
		val trialDaysLeft: Int = 0

		@SerializedName("userSub")
		val userSub: String? = null

		@SerializedName("accountId")
		val accountId: String? = null

		@SerializedName("lastUpdated")
		val lastUpdated: Long = 0

		@SerializedName("portalLinkExpiry")
		val portalLinkExpiry: Long = 0

		@SerializedName("endEpochSecs")
		val endEpochSecs: Long = 0

		@SerializedName("subscriptionId")
		val subscriptionId: String? = null

		@SerializedName("plan")
		val plan: String? = null

		@SerializedName("gateway")
		val gateway: String? = null

		@SerializedName("email")
		val email: String? = null
	}
}