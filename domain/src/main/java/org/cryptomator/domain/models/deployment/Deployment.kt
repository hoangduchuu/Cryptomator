package org.cryptomator.domain.models.deployment

data class Deployment(
	val lastModified: Long,
	val deviceId: String,
	val status: String
) {

	fun shouldGetDeployment(): Boolean {
		return status == "ADD_PENDING" || status == "ADDED"
	}
}

//
//1. Local EXISTS:
//Remote Status    | Local DB Action          | API Action
//----------------|--------------------------|------------------
//AddPending      | Keep in DB               | Update to Added
//Added           | Do Nothing               | Do Nothing
//RemovePending   | Delete from DB           | Update to Removed
//Removed         | Delete from DB           | Do Nothing
//2. Local NOT EXISTS:
//Remote Status    | Local DB Action          | API Action
//----------------|--------------------------|------------------
//AddPending      | Save to DB               | Update to Added
//Added           | Save to DB               | Update to Added
//RemovePending   | Do Nothing               | Do Nothing
//Removed         | Do Nothing               | Do Nothing