package org.cryptomator.presentation.ui.activity.view

import org.cryptomator.presentation.model.CloudFolderModel
import org.cryptomator.presentation.model.VaultModel
import org.cryptomator.presentation.model.userprofile.UserProfileModel

interface VaultListView : View {

	fun renderVaultList(vaultModelCollection: List<VaultModel>)
	fun showVaultCreationHint()
	fun hideVaultCreationHint()
	fun deleteVaultFromAdapter(vaultId: Long)
	fun addOrUpdateVault(vault: VaultModel)
	fun renameVault(vaultModel: VaultModel)
	fun navigateToVaultContent(vault: VaultModel, decryptedRoot: CloudFolderModel)
	fun showVaultSettingsDialog(vaultModel: VaultModel)
	fun showAddVaultBottomSheet()
	fun showRenameDialog(vaultModel: VaultModel)
	fun isVaultLocked(vaultModel: VaultModel): Boolean
	fun rowMoved(fromPosition: Int, toPosition: Int)
	fun vaultMoved(vaults: List<VaultModel>)
	fun migrateCBCEncryptedPasswordVaults(vaults: List<VaultModel>)
	fun showProfileInfoBottomSheet(userProfile: UserProfileModel)
	fun updateUserProfile(userProfile: UserProfileModel)
	fun updateUserProfileToLoggedOut()
	fun showLoading()
	fun hideLoading()
	fun displayAvatar()
	fun hidePtrProgress()
	fun showVaultIsDisableNotice()
	fun showRequestStoragePermissionForDeployments()
	
	// Import status methods
	fun showImportStatus(currentIndex: Int, totalCount: Int, progress: Int)
	fun updateImportStatus(currentIndex: Int, totalCount: Int, progress: Int)
	fun hideImportStatus()

}
