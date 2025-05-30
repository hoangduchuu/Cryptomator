package org.cryptomator.presentation.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.EndSessionRequest
import net.openid.appauth.ResponseTypeValues
import org.cryptomator.domain.Vault
import org.cryptomator.generator.Activity
import org.cryptomator.generator.InjectIntent
import org.cryptomator.presentation.CryptomatorApp
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.ActivityLayoutObscureAwareBinding
import org.cryptomator.presentation.intent.Intents.browseFilesIntent
import org.cryptomator.presentation.intent.Intents.settingsIntent
import org.cryptomator.presentation.intent.VaultListIntent
import org.cryptomator.presentation.model.CloudFolderModel
import org.cryptomator.presentation.model.ProgressModel
import org.cryptomator.presentation.model.VaultModel
import org.cryptomator.presentation.model.userprofile.UserProfileModel
import org.cryptomator.presentation.presenter.VaultListPresenter
import org.cryptomator.presentation.service.OpenWritableFileNotification
import org.cryptomator.presentation.ui.activity.view.VaultListView
import org.cryptomator.presentation.ui.bottomsheet.AddVaultBottomSheet
import org.cryptomator.presentation.ui.bottomsheet.SettingsVaultBottomSheet
import org.cryptomator.presentation.ui.bottomsheet.UserProfileBottomSheet
import org.cryptomator.presentation.ui.callback.VaultListCallback
import org.cryptomator.presentation.ui.dialog.AskForLockScreenDialog
import org.cryptomator.presentation.ui.dialog.BetaConfirmationDialog
import org.cryptomator.presentation.ui.dialog.CBCPasswordVaultsMigrationDialog
import org.cryptomator.presentation.ui.dialog.UpdateAppAvailableDialog
import org.cryptomator.presentation.ui.dialog.UpdateAppDialog
import org.cryptomator.presentation.ui.dialog.VaultDeleteConfirmationDialog
import org.cryptomator.presentation.ui.dialog.VaultRenameDialog
import org.cryptomator.presentation.ui.fragment.VaultListFragment
import org.cryptomator.presentation.ui.layout.ObscuredAwareCoordinatorLayout.Listener
import org.cryptomator.presentation.util.BiometricAuthenticationMigration
import java.util.UUID
import javax.inject.Inject
import timber.log.Timber


@Activity
class VaultListActivity : BaseActivity<ActivityLayoutObscureAwareBinding>(ActivityLayoutObscureAwareBinding::inflate), //
	VaultListView, //
	VaultListCallback, //
	AskForLockScreenDialog.Callback, //
	UpdateAppAvailableDialog.Callback, //
	UpdateAppDialog.Callback, //
	BetaConfirmationDialog.Callback, //
	CBCPasswordVaultsMigrationDialog.Callback, //
	BiometricAuthenticationMigration.Callback,
	UserProfileBottomSheet.Callback,
	VaultListFragment.SignInClickListener
{

	@Inject
	lateinit var vaultListPresenter: VaultListPresenter

	@InjectIntent
	lateinit var vaultListIntent: VaultListIntent

	//region new variable for login auth
	private lateinit var authService: AuthorizationService
	private lateinit var authState: AuthState
	private lateinit var serviceConfig: AuthorizationServiceConfiguration
	private var isConfigured = false
	private var isFirstLaunch = true
	private var returningFromSettings = false

	private val authResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		val data = result.data
		handleAuthorizationResponse(data)
	}

	private val endSessionResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		val data = result.data
		handleEndSessionResponse(data)
	}

	private val storagePermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { permissions ->
		if (permissions.all { it.value }) {
			// All permissions granted
			Toast.makeText(
				this,
				getString(R.string.storage_permission_granted),
				Toast.LENGTH_SHORT
			).show()
			// Try to get deployments if the user is logged in
			if (vaultListPresenter.isLoggedIn) {
				vaultListPresenter.getDeployment()
			}
		} else {
			// Show explanation and request again
			showStoragePermissionExplanation()
		}
	}

	companion object {
		private const val TAG = "MainActivity"
		private val CLIENT_ID = org.cryptomator.data.BuildConfig.COGNITO_CLIENT_ID
		private const val REDIRECT_URI = "ncryptor://auth"
		private val END_SESSION_REDIRECT_URI = org.cryptomator.data.BuildConfig.END_SESSION_REDIRECT_URI
		private val COGNITO_ISSUER = org.cryptomator.data.BuildConfig.COGNITO_ISSUER
	}
	//endregion new variable for login auth

	override fun onCreate(savedInstanceState: Bundle?) {
		installSplashScreen()
		super.onCreate(savedInstanceState)

		// Initialize the authorization service
		authService = AuthorizationService(this)

		// Configure the service using the issuer URL
		configureService()


//
//		// Enable WebView debugging
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//			WebView.setWebContentsDebuggingEnabled(true)
//		}
	}

	override fun onResume() {
		super.onResume()
		if (!isFirstLaunch) {
			// Always refresh user profile data
			vaultListPresenter.getUserProfile()
			
			// If returning from settings, check permissions and deployments again
			if (returningFromSettings) {
				Timber.d("Returning from settings, checking for changes")
				
				// Check if we now have all the required permissions
				if (vaultListPresenter.isLoggedIn) {
					Timber.d("User is logged in, checking storage permissions")
					// The presenter's getDeployment method will check permissions first
					vaultListPresenter.getDeployment()
				}
				
				returningFromSettings = false
			}
		}
		isFirstLaunch = false
		displayAvatar();
	}

	override fun onPause() {
		super.onPause()
		// When leaving the activity, we might be going to settings
		returningFromSettings = true
	}

	override fun onWindowFocusChanged(hasFocus: Boolean) {
		super.onWindowFocusChanged(hasFocus)
		vaultListPresenter.onWindowFocusChanged(hasFocus)
	}

	override fun setupView() {
		setupToolbar()
		vaultListPresenter.prepareView()
		binding.activityRootView.setOnFilteredTouchEventForSecurityListener(object : Listener {
			override fun onFilteredTouchEventForSecurity() {
				vaultListPresenter.onFilteredTouchEventForSecurity()
			}
		})

		if (stopEditFilePressed() && sharedPreferencesHandler.keepUnlockedWhileEditing()) {
			hideNotification()
			unSuspendLock()
		}
		
		// Check storage permissions on first launch
		if (isFirstLaunch) {
			Timber.d("First launch, checking storage permissions")
			// We'll check permissions first before getting deployments
			checkStoragePermissionsOnFirstLaunch()
		}
	}

	override fun onStart() {
		super.onStart()
		// We'll let the presenter handle checking permissions and getting deployments
		// based on login status
	}

	private fun stopEditFilePressed(): Boolean {
		return vaultListIntent.stopEditFileNotification() != null && vaultListIntent.stopEditFileNotification()
	}

	private fun hideNotification() {
		OpenWritableFileNotification(context(), Uri.EMPTY).hide()
	}

	private fun unSuspendLock() {
		val cryptomatorApp = activity().application as CryptomatorApp
		cryptomatorApp.unSuspendLock()
	}

	override fun createFragment(): Fragment {
		val fragment = VaultListFragment()
		fragment.setSignInClickListener(this)
		return fragment
	}

	override fun snackbarView(): View {
		val fragment = getCurrentFragment(R.id.fragment_container)
		return if (fragment != null && fragment is VaultListFragment) {
			fragment.rootView()
		} else {
			binding.activityRootView // Fallback to the activity root view
		}
	}

	override fun getCustomMenuResource(): Int = R.menu.menu_vault_list

	override fun onMenuItemSelected(itemId: Int): Boolean = when (itemId) {
		R.id.action_settings -> {
			vaultListPresenter.startIntent(settingsIntent())
			true
		}

		R.id.action_sign_in -> {
			if (vaultListPresenter.isLoggedIn) {
				vaultListPresenter.showUserProfile();
			} else {
				handleLogin()
			}
			true
		}
		R.id.action_sign_out -> {
			checkStoragePermissionsAndGetDeployment()
			true
		}

		R.id.action_refresh -> {
			vaultListPresenter.checkPolling()
			true
		}
		else -> super.onMenuItemSelected(itemId)
	}


	override fun isVaultLocked(vaultModel: VaultModel): Boolean {
		val fragment = getCurrentFragment(R.id.fragment_container)
		return if (fragment != null && fragment is VaultListFragment) {
			fragment.isVaultLocked(vaultModel)
		} else {
			true // Default to locked if fragment is not available
		}
	}

	private fun setupToolbar() {
		binding.mtToolbar.toolbar.title = getString(R.string.app_name).uppercase()
		setSupportActionBar(binding.mtToolbar.toolbar)
		displayAvatar()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		 super.onCreateOptionsMenu(menu)
		displayAvatar()
		return true
	}

	override fun showAddVaultBottomSheet() {
		if(vaultListPresenter.isLoggedIn){
			showDialog(AddVaultBottomSheet())
		}else{
			showError2("Please login to add vault")
		}

	}

	override fun showRenameDialog(vaultModel: VaultModel) {
		showDialog(VaultRenameDialog.newInstance(vaultModel))
	}

	override fun rowMoved(fromPosition: Int, toPosition: Int) {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.rowMoved(fromPosition, toPosition)
		}
	}

	override fun vaultMoved(vaults: List<VaultModel>) {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.vaultMoved(vaults)
		}
	}

	override fun migrateCBCEncryptedPasswordVaults(vaults: List<VaultModel>) {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			val biometricAuthenticationMigration = BiometricAuthenticationMigration(this, context(), sharedPreferencesHandler.useConfirmationInFaceUnlockBiometricAuthentication())
			biometricAuthenticationMigration.migrateVaultsPassword(fragment, vaults)
		}
	}

	override fun showProfileInfoBottomSheet(userProfile: UserProfileModel) {
		val bottomSheet = UserProfileBottomSheet.newInstance(userProfile,sharedPreferencesHandler)
		bottomSheet.show(supportFragmentManager, "UserProfileBottomSheet")
	}

	override fun updateUserProfile(userProfile: UserProfileModel) {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.updateUserProfile(userProfile)
		}
		
		//region update user profile
		val menuItem = binding.mtToolbar.toolbar.menu.findItem(R.id.action_sign_in)
		menuItem?.let { item ->
			item.setIcon(R.drawable.ic_user)
			val imageView = ImageView(this)
			imageView.layoutParams = ViewGroup.LayoutParams(
				resources.getDimensionPixelSize(R.dimen.avatar_size),
				resources.getDimensionPixelSize(R.dimen.avatar_size)
			)
			item.setActionView(imageView)
			imageView.setOnClickListener {
				vaultListPresenter.showUserProfile()
			}
			vaultListPresenter.renderAvatar(imageView)
		}
		
		// Update the UserProfileBottomSheet if it's showing
		val userProfileBottomSheet = supportFragmentManager.findFragmentByTag("UserProfileBottomSheet") as? UserProfileBottomSheet
		if (userProfileBottomSheet != null) {
			Timber.d("Found UserProfileBottomSheet, updating with new profile data")
			userProfileBottomSheet.updateUserProfile(userProfile)
		} else {
			Timber.d("UserProfileBottomSheet not found")
		}
		//endregion update user profile
	}

	override fun updateUserProfileToLoggedOut() {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.updateUserProfileToLoggedOut()
		}
		
		val menuItem = binding.mtToolbar.toolbar.menu.findItem(R.id.action_sign_in)
		menuItem?.let { item ->
			item.setIcon(R.drawable.ic_user)
			item.setActionView(null)
			// setOnClickListener to the imageView
			item.setOnMenuItemClickListener {
				handleLogin()
				true
			}
		}
	}

	override fun showLoading() {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.showLoading()
		}
	}

	override fun hideLoading() {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.hideLoading()
		}
	}

	override fun displayAvatar() {
		val menuItem = binding.mtToolbar.toolbar.menu.findItem(R.id.action_sign_in)
		menuItem?.let { item ->
			if (vaultListPresenter.isLoggedIn) {
				val imageView = ImageView(this)
				imageView.layoutParams = ViewGroup.LayoutParams(
					resources.getDimensionPixelSize(R.dimen.avatar_size),
					resources.getDimensionPixelSize(R.dimen.avatar_size)
				)
				item.setActionView(imageView)
				imageView.setOnClickListener {
					vaultListPresenter.showUserProfile()
				}
				vaultListPresenter.renderAvatar(imageView)
			} else {
				item.setIcon(R.drawable.ic_user)
				item.setActionView(null)
				item.setOnMenuItemClickListener {
					handleLogin()
					true
				}
			}
		}
	}

	override fun hidePtrProgress() {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.hidePtrProgress()
		}
	}

	override fun showVaultIsDisableNotice() {
		showMessage(getString(R.string.this_vault_is_disabled_please_enable_it_in_the_dashboard))
	}

	override fun showVaultSettingsDialog(vaultModel: VaultModel) {
		val vaultSettingDialog = //
			SettingsVaultBottomSheet.newInstance(vaultModel)
		vaultSettingDialog.show(supportFragmentManager, "VaultSettings")
	}

	override fun renderVaultList(vaultModelCollection: List<VaultModel>) {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.showVaults(vaultModelCollection)
		}
	}

	override fun showVaultCreationHint() {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.showVaultCreationHint()
		}
	}

	override fun hideVaultCreationHint() {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.hideVaultCreationHint()
		}
	}

	override fun deleteVaultFromAdapter(vaultId: Long) {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.deleteVaultFromAdapter(vaultId)
		}
	}

	override fun addOrUpdateVault(vault: VaultModel) {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.addOrUpdateVault(vault)
		}
	}

	override fun navigateToVaultContent(vault: VaultModel, decryptedRoot: CloudFolderModel) {
		vaultListPresenter.startIntent(browseFilesIntent()
			.withTitle(vault.name)
			.withFolder(decryptedRoot)
			.withVault(vault))
	}

	override fun renameVault(vaultModel: VaultModel) {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.addOrUpdateVault(vaultModel)
		}
	}

	override fun onAddExistingVault() {
		vaultListPresenter.onAddExistingVault()
	}

	override fun onCreateVault() {
		vaultListPresenter.onCreateVault()
	}

	override fun onDeleteVaultClick(vaultModel: VaultModel) {
		VaultDeleteConfirmationDialog.newInstance(vaultModel) //
			.show(supportFragmentManager, "VaultDeleteConfirmationDialog")
	}

	override fun onRenameVaultClick(vaultModel: VaultModel) {
		vaultListPresenter.onRenameVaultClicked(vaultModel)
	}

	override fun onLockVaultClick(vaultModel: VaultModel) {
		vaultListPresenter.onVaultLockClicked(vaultModel)
	}

	override fun onChangePasswordClick(vaultModel: VaultModel) {
		vaultListPresenter.onChangePasswordClicked(vaultModel)
	}

	override fun onRenameClick(vaultModel: VaultModel, newVaultName: String) {
		vaultListPresenter.renameVault(vaultModel, newVaultName)
	}

	override fun onDeleteConfirmedClick(vaultModel: VaultModel) {
		vaultListPresenter.deleteVault(vaultModel)
	}

	override fun onAskForLockScreenFinished(setScreenLock: Boolean) {
		vaultListPresenter.onAskForLockScreenFinished(setScreenLock)
	}

	private fun vaultListFragment(): VaultListFragment {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment == null || fragment !is VaultListFragment) {
			Timber.tag("VaultListActivity").w("VaultListFragment not yet available")
			return VaultListFragment() // Return a new instance but don't use it
		}
		return fragment as VaultListFragment
	}

	override fun onUpdateAppDialogLoaded() {
		showProgress(ProgressModel.GENERIC)
	}

	override fun installUpdate() {
		vaultListPresenter.installUpdate()
	}

	override fun cancelUpdateClicked() {
		closeDialog()
	}

	override fun showUpdateWebsite() {
		val url = "https://ncryptor.com/android/"
		val intent = Intent(Intent.ACTION_VIEW)
		intent.data = Uri.parse(url)
		startActivity(intent)
	}

	override fun onAskForBetaConfirmationFinished() {
		sharedPreferencesHandler.setBetaScreenDialogAlreadyShown(true)
	}

	override fun onCBCPasswordVaultsMigrationClicked(cbcVaults: List<Vault>) {
		vaultListPresenter.cBCPasswordVaultsMigrationClicked(cbcVaults)
	}

	override fun onCBCPasswordVaultsMigrationRejected(cbcVaults: List<Vault>) {
		vaultListPresenter.cBCPasswordVaultsMigrationRejected(cbcVaults)
	}

	override fun onBiometricAuthenticationMigrationFinished(vaults: List<VaultModel>) {
		vaultListPresenter.biometricAuthenticationMigrationFinished(vaults)
	}

	override fun onBiometricAuthenticationFailed(vaults: List<VaultModel>) {
		vaultListPresenter.biometricAuthenticationFailed(vaults)
	}

	override fun onBiometricKeyInvalidated(vaults: List<VaultModel>) {
		vaultListPresenter.biometricKeyInvalidated(vaults)
	}


	//region login auth

	private fun handleLogin() {
		if (isConfigured) {
			doAuthorization()
		} else {
			showError2("Please wait, service is still being configured")
			configureService()
		}
	}

	@SuppressLint("HardwareIds")
	fun getHashedDeviceUuid(context: Context): String {
		val androidId = Settings.Secure.getString(
			context.contentResolver,
			Settings.Secure.ANDROID_ID
		)
		Log.d(TAG, "Android ID: $androidId")
		return UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
	}

	private fun configureService() {
		AuthorizationServiceConfiguration.fetchFromIssuer(
			Uri.parse(COGNITO_ISSUER)
		) { fetchedConfig, ex ->
			if (ex != null) {
				Log.e(TAG, "Failed to fetch configuration", ex)
				showError2("Failed to initialize authentication service")
				isConfigured = false
				return@fetchFromIssuer
			}

			fetchedConfig?.let {
				serviceConfig = it
				authState = AuthState(serviceConfig)
				isConfigured = true
				handleIntent(intent)
			}
		}
	}

	private fun doAuthorization() {
		if (!isConfigured) {
			showError2("Service not configured yet")
			return
		}

		try {
			val authRequestBuilder = AuthorizationRequest.Builder(
				serviceConfig,
				CLIENT_ID,
				ResponseTypeValues.CODE,
				Uri.parse(REDIRECT_URI)
			).setScope("email openid phone profile aws.cognito.signin.user.admin")

			val authRequest = authRequestBuilder.build()
			val authIntent = authService.getAuthorizationRequestIntent(authRequest)
			authResultLauncher.launch(authIntent)
		} catch (e: Exception) {
			Log.e(TAG, "Failed to create authorization request", e)
			showError2(getString(R.string.failed_to_start_authorization, e.message))
		}
	}

	private fun handleAuthorizationResponse(data: Intent?) {
		if (data == null) {
			showError2(getString(R.string.no_authentication_data_received))
			return
		}

		val response = AuthorizationResponse.fromIntent(data)
		val error = AuthorizationException.fromIntent(data)

		when {
			response != null -> {
				authState.update(response, error)
				performTokenRequest(response)
			}
			error != null -> {
				Log.e(TAG, "Authorization failed", error)
				when (error.type) {
					AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW.type -> {
						showError2(getString(R.string.authentication_cancelled_by_user))
					}
					AuthorizationException.GeneralErrors.NETWORK_ERROR.type -> {
						showError2(getString(R.string.network_error_occurred))
					}
					else -> {
						showError2(getString(R.string.authentication_failed, error.message))
					}
				}
			}
			else -> {
				showError2(getString(R.string.no_response_or_error_received))
			}
		}
	}

	private fun endSession() {
		if (!isConfigured) {
			showError2(getString(R.string.service_not_configured_yet))
			return
		}

		try {
			// Create end session request with required parameters
			val endSessionRequest = EndSessionRequest.Builder(serviceConfig)
				.setIdTokenHint(authState.idToken)
				.setPostLogoutRedirectUri(Uri.parse(END_SESSION_REDIRECT_URI))
				.setAdditionalParameters(
					mapOf(
						"client_id" to CLIENT_ID,
						"logout_uri" to END_SESSION_REDIRECT_URI
					)
				)
				.build()

			// Get the end session intent and launch it
			val endSessionIntent = authService.getEndSessionRequestIntent(endSessionRequest)
			endSessionResultLauncher.launch(endSessionIntent)
		} catch (e: Exception) {
			Log.e(TAG, "Failed to create end session request", e)
			showError2(getString(R.string.failed_to_start_logout, e.message))
			// Even if end session fails, we should still clear local state
			clearLocalAuthState()
		}
	}

	private fun handleEndSessionResponse(data: Intent?) {
		if (data == null) {
			showError2(getString(R.string.no_logout_data_received))
			clearLocalAuthState()
			return
		}

		val error = AuthorizationException.fromIntent(data)

		if (error != null) {
			Log.e(TAG, "End session failed", error)
			// Even if end session fails, we should still clear local state
			clearLocalAuthState()
		} else {
			// Successfully logged out, clear local state
			clearLocalAuthState()
			showMessage(getString(R.string.successfully_logged_out))
		}
	}

	private fun clearLocalAuthState() {
		// Clear the auth state
		authState = AuthState(serviceConfig)
		// Clear any stored tokens
		sharedPreferencesHandler.setCognitoAccessToken("")
		sharedPreferencesHandler.setCognitoRefreshToken("")
		sharedPreferencesHandler.setUserProfileCacheExpires(0)
		sharedPreferencesHandler.clearSSOUserFullName();
	}

	private fun performTokenRequest(response: AuthorizationResponse) {
		val tokenRequest = response.createTokenExchangeRequest()
		authService.performTokenRequest(tokenRequest) { tokenResponse, exception ->
			when {
				tokenResponse != null -> {
					authState.update(tokenResponse, exception)
					Log.i(TAG, getString(R.string.token_exchange_completed_successfully))
					vaultListPresenter.performLogin(tokenResponse)
				}
				exception != null -> {
					Log.e(TAG, "Token exchange failed", exception)
					showError2(getString(R.string.token_exchange_failed, exception.message))
				}
			}
		}
	}

	private fun handleIntent(intent: Intent) {
		if (intent.action == Intent.ACTION_VIEW) {
			val response = AuthorizationResponse.fromIntent(intent)
			val error = AuthorizationException.fromIntent(intent)

			when {
				response != null -> {
					authState.update(response, error)
					performTokenRequest(response)
				}
				error != null -> {
					Log.e(TAG, "Authorization failed", error)
					showError2(getString(R.string.authorization_failed, error.message))
				}
			}
		}
	}

	private fun showError2(message: String) {
		runOnUiThread {
			Toast.makeText(this, message, Toast.LENGTH_LONG).show()
		}
	}

	private fun showMessage(message: String) {
		runOnUiThread {
			Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		authService.dispose()
	}

	override fun onLogoutClick(keepVaultsData: Boolean) {
		vaultListPresenter.signOut(keepVaultsData = keepVaultsData)
		updateUserProfileToLoggedOut()
		endSession()
	}

	override fun onRefreshClick() {
		vaultListPresenter.getUserProfile()
	}

	override fun onSignInClick() {
		handleLogin()
	}

	//endregion login auth

	private fun checkStoragePermissionsAndGetDeployment() {
		Timber.d("Checking storage permissions for deployment")
		// Call the presenter's method which handles permission checking internally
		// This will show the permission dialog if needed, or proceed with getting deployments if permissions are granted
		vaultListPresenter.getDeployment()
	}

	private fun showManageStoragePermissionDialog() {
		// Show an explanatory dialog first
		val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
			.setTitle(R.string.storage_permission_title)
			.setMessage(R.string.storage_permission_explanation)
			.setPositiveButton(R.string.snack_bar_action_title_settings) { _, _ ->
				// Open system settings for all files access permission
				val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
					data = Uri.parse("package:$packageName")
				}
				try {
					startActivity(intent)
					returningFromSettings = true
				} catch (e: Exception) {
					// Fallback if the specific setting is not available
					val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
					startActivity(fallbackIntent)
					returningFromSettings = true
				}
			}
			.setNegativeButton(R.string.dialog_button_cancel, null)
			.create()
			
		alertDialog.show()
	}

	private fun showStoragePermissionExplanation() {
		Toast.makeText(
			this,
			getString(R.string.storage_permission_explanation),
			Toast.LENGTH_LONG
		).show()
		
		// Show option to open settings directly
		val snackbar = com.google.android.material.snackbar.Snackbar.make(
			findViewById(android.R.id.content),
			getString(R.string.error_storage_permission_required),
			com.google.android.material.snackbar.Snackbar.LENGTH_LONG
		)
			
		snackbar.setAction(R.string.snack_bar_action_title_settings) {
			openAppSettings()
		}.show()
	}
	
	private fun openAppSettings() {
		val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
			data = Uri.fromParts("package", packageName, null)
			addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		}
		startActivity(intent)
		returningFromSettings = true
	}

	/**
	 * Check for storage permissions on first app launch and request them if not granted
	 */
	private fun checkStoragePermissionsOnFirstLaunch() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			if (!Environment.isExternalStorageManager()) {
				Timber.d("All files access permission not granted, showing dialog")
				showManageStoragePermissionDialog()
			} else {
				Timber.d("All files access permission granted on first launch")
			}
		} else {
			val permissions = arrayOf(
				Manifest.permission.READ_EXTERNAL_STORAGE,
				Manifest.permission.WRITE_EXTERNAL_STORAGE
			)
			
			if (!permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
				Timber.d("Storage permissions not granted, requesting permissions")
				storagePermissionLauncher.launch(permissions)
			} else {
				Timber.d("Storage permissions already granted on first launch")
			}
		}
	}

	override fun showRequestStoragePermissionForDeployments() {
		Timber.d("Showing storage permission request for deployments")
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// For Android 11+, need to request All Files Access permission
			val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
				.setTitle(R.string.storage_permission_title)
				.setMessage(R.string.storage_permission_needed_for_deployments)
				.setPositiveButton(R.string.snack_bar_action_title_settings) { _, _ ->
					// Open system settings for all files access permission
					val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
						data = Uri.parse("package:$packageName")
					}
					try {
						startActivity(intent)
						returningFromSettings = true
					} catch (e: Exception) {
						// Fallback if the specific setting is not available
						val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
						startActivity(fallbackIntent)
						returningFromSettings = true
					}
				}
				.setNegativeButton(R.string.dialog_button_cancel) { _, _ ->
					showMessage(getString(R.string.storage_permission_required_for_deployments))
				}
				.create()
				
			alertDialog.show()
		} else {
			// For Android 10 and below, request regular storage permissions
			val permissions = arrayOf(
				Manifest.permission.READ_EXTERNAL_STORAGE,
				Manifest.permission.WRITE_EXTERNAL_STORAGE
			)
			
			// Show explanation before requesting
			val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
				.setTitle(R.string.storage_permission_title)
				.setMessage(R.string.storage_permission_needed_for_deployments)
				.setPositiveButton(android.R.string.ok) { _, _ ->
					storagePermissionLauncher.launch(permissions)
				}
				.setNegativeButton(R.string.dialog_button_cancel, null)
				.create()
				
			alertDialog.show()
		}
	}
	
	/**
	 * Shows the import status indicator
	 * 
	 * @param currentIndex Current vault being imported (1-based)
	 * @param totalCount Total number of vaults to import
	 * @param progress Progress percentage (0-100)
	 */
	override fun showImportStatus(currentIndex: Int, totalCount: Int, progress: Int) {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.showImportStatus(currentIndex, totalCount, progress)
		}
	}
	
	/**
	 * Updates the import status indicator
	 * 
	 * @param currentIndex Current vault being imported (1-based)
	 * @param totalCount Total number of vaults to import
	 * @param progress Progress percentage (0-100)
	 */
	override fun updateImportStatus(currentIndex: Int, totalCount: Int, progress: Int) {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.updateImportStatus(currentIndex, totalCount, progress)
		}
	}
	
	/**
	 * Hides the import status indicator
	 */
	override fun hideImportStatus() {
		val fragment = getCurrentFragment(R.id.fragment_container)
		if (fragment != null && fragment is VaultListFragment) {
			fragment.hideImportStatus()
		}
	}
}
