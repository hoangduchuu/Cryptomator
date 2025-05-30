package org.cryptomator.presentation.model.mappers

import org.cryptomator.domain.models.userprofile.Computer
import org.cryptomator.domain.models.userprofile.Geolocation
import org.cryptomator.domain.models.userprofile.Import
import org.cryptomator.domain.models.userprofile.Policy
import org.cryptomator.domain.models.userprofile.Subscription
import org.cryptomator.domain.models.userprofile.UserProfile
import org.cryptomator.presentation.model.userprofile.ComputerModel
import org.cryptomator.presentation.model.userprofile.GeolocationModel
import org.cryptomator.presentation.model.userprofile.ImportModel
import org.cryptomator.presentation.model.userprofile.PolicyModel
import org.cryptomator.presentation.model.userprofile.SubscriptionModel
import org.cryptomator.presentation.model.userprofile.UserProfileModel


import javax.inject.Inject

class UserProfileModelMapper @Inject constructor(
	private val policyModelMapper: PolicyModelMapper, private val subscriptionModelMapper: SubscriptionModelMapper, private val computerModelMapper: ComputerModelMapper
) : ModelMapper<UserProfileModel, UserProfile>() {

	override fun fromModel(model: UserProfileModel): UserProfile {
		return model.toUserProfile()
	}

	override fun toModel(domainObject: UserProfile): UserProfileModel {
		return UserProfileModel(domainObject)
	}
}

class PolicyModelMapper @Inject constructor() : ModelMapper<PolicyModel, Policy>() {

	override fun fromModel(model: PolicyModel): Policy {
		return model.toPolicy()
	}

	override fun toModel(domainObject: Policy): PolicyModel {
		return PolicyModel(domainObject)
	}
}

class ComputerModelMapper @Inject constructor(
	private val importModelMapper: ImportModelMapper, private val geolocationModelMapper: GeolocationModelMapper
) : ModelMapper<ComputerModel, Computer>() {

	override fun fromModel(model: ComputerModel): Computer {
		return model.toComputer()
	}

	override fun toModel(domainObject: Computer): ComputerModel {
		return ComputerModel(domainObject)
	}
}

class ImportModelMapper @Inject constructor() : ModelMapper<ImportModel, Import>() {

	override fun fromModel(model: ImportModel): Import {
		return model.toImport()
	}

	override fun toModel(domainObject: Import): ImportModel {
		return ImportModel(domainObject)
	}
}

class GeolocationModelMapper @Inject constructor() : ModelMapper<GeolocationModel, Geolocation>() {

	override fun fromModel(model: GeolocationModel): Geolocation {
		return model.toGeolocation()
	}

	override fun toModel(domainObject: Geolocation): GeolocationModel {
		return GeolocationModel(domainObject)
	}
}

class SubscriptionModelMapper @Inject constructor() : ModelMapper<SubscriptionModel, Subscription>() {

	override fun fromModel(model: SubscriptionModel): Subscription {
		return model.toSubscription()
	}

	override fun toModel(domainObject: Subscription): SubscriptionModel {
		return SubscriptionModel(domainObject)
	}
}