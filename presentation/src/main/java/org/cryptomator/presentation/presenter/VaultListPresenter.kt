package org.cryptomator.presentation.presenter

import android.Manifest
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.common.base.Optional
import com.squareup.picasso.Picasso
import net.openid.appauth.TokenResponse
import org.cryptomator.data.cloud.crypto.CryptoCloud
import org.cryptomator.data.util.NetworkConnectionCheck
import org.cryptomator.domain.Cloud
import org.cryptomator.domain.CloudFolder
import org.cryptomator.domain.CloudType
import org.cryptomator.domain.DeviceArgs
import org.cryptomator.domain.LocalStorageCloud
import org.cryptomator.domain.Vault
import org.cryptomator.domain.di.PerView
import org.cryptomator.domain.exception.FatalBackendException
import org.cryptomator.domain.exception.license.LicenseNotValidException
import org.cryptomator.domain.models.deployment.DeploymentWithStatus
import org.cryptomator.domain.models.userprofile.UserProfile
import org.cryptomator.domain.usecases.DoLicenseCheckUseCase
import org.cryptomator.domain.usecases.DoUpdateCheckUseCase
import org.cryptomator.domain.usecases.DoUpdateUseCase
import org.cryptomator.domain.usecases.GetDecryptedCloudForVaultUseCase
import org.cryptomator.domain.usecases.LicenseCheck
import org.cryptomator.domain.usecases.NoOpResultHandler
import org.cryptomator.domain.usecases.UpdateCheck
import org.cryptomator.domain.usecases.cloud.GetRootFolderUseCase
import org.cryptomator.domain.usecases.user.CacheUserProfileUseCase
import org.cryptomator.domain.usecases.user.ClearUserProfileCacheUseCase
import org.cryptomator.domain.usecases.user.GetCachedUserProfileUseCase
import org.cryptomator.domain.usecases.user.GetUserAvatarUseCase
import org.cryptomator.domain.usecases.user.GetUserProfileUseCase
import org.cryptomator.domain.usecases.user.LoginUseCase
import org.cryptomator.domain.usecases.user.RefreshTokenUseCase
import org.cryptomator.domain.usecases.vault.DeleteVaultUseCase
import org.cryptomator.domain.usecases.vault.DeleteVaultsUseCase
import org.cryptomator.domain.usecases.vault.GetDeploymentInfoUseCase
import org.cryptomator.domain.usecases.vault.GetVaultListUseCase
import org.cryptomator.domain.usecases.vault.ImportDeploymentVaultUseCase
import org.cryptomator.domain.usecases.vault.ListCBCEncryptedPasswordVaultsUseCase
import org.cryptomator.domain.usecases.vault.LockVaultUseCase
import org.cryptomator.domain.usecases.vault.LogDeviceEventUseCase
import org.cryptomator.domain.usecases.vault.MoveVaultPositionUseCase
import org.cryptomator.domain.usecases.vault.PollVaultUseCase
import org.cryptomator.domain.usecases.vault.RemoveStoredVaultPasswordsUseCase
import org.cryptomator.domain.usecases.vault.RenameVaultUseCase
import org.cryptomator.domain.usecases.vault.SaveVaultUseCase
import org.cryptomator.domain.usecases.vault.SaveVaultsUseCase
import org.cryptomator.domain.usecases.vault.UpdateVaultEtagUseCase
import org.cryptomator.domain.usecases.vault.UpdateVaultParameterIfChangedRemotelyUseCase
import org.cryptomator.generator.Callback
import org.cryptomator.presentation.BuildConfig
import org.cryptomator.presentation.CryptomatorApp
import org.cryptomator.presentation.R
import org.cryptomator.presentation.exception.ExceptionHandlers
import org.cryptomator.presentation.intent.Intents
import org.cryptomator.presentation.intent.UnlockVaultIntent
import org.cryptomator.presentation.model.CloudModel
import org.cryptomator.presentation.model.CloudTypeModel
import org.cryptomator.presentation.model.ProgressModel
import org.cryptomator.presentation.model.VaultModel
import org.cryptomator.presentation.model.mappers.CloudFolderModelMapper
import org.cryptomator.presentation.model.mappers.UserProfileModelMapper
import org.cryptomator.presentation.model.userprofile.UserProfileModel
import org.cryptomator.presentation.ui.activity.LicenseCheckActivity
import org.cryptomator.presentation.ui.activity.view.VaultListView
import org.cryptomator.presentation.ui.dialog.AppIsObscuredInfoDialog
import org.cryptomator.presentation.ui.dialog.AskForLockScreenDialog
import org.cryptomator.presentation.ui.dialog.CBCPasswordVaultsMigrationDialog
import org.cryptomator.presentation.ui.dialog.EnterPasswordDialog
import org.cryptomator.presentation.ui.dialog.UpdateAppAvailableDialog
import org.cryptomator.presentation.ui.dialog.UpdateAppDialog
import org.cryptomator.presentation.ui.dialog.VaultsRemovedDuringMigrationDialog
import org.cryptomator.presentation.util.AvatarGenerator
import org.cryptomator.presentation.util.DeviceUtils
import org.cryptomator.presentation.util.FileUtil
import org.cryptomator.presentation.workflow.ActivityResult
import org.cryptomator.presentation.workflow.AddExistingVaultWorkflow
import org.cryptomator.presentation.workflow.AuthenticationExceptionHandler
import org.cryptomator.presentation.workflow.CreateNewVaultWorkflow
import org.cryptomator.presentation.workflow.PermissionsResult
import org.cryptomator.presentation.workflow.Workflow
import org.cryptomator.util.SharedPreferencesHandler
import org.cryptomator.util.crypto.CryptoMode
import org.cryptomator.util.shouldRejectUnlock
import java.io.File
import javax.inject.Inject
import AccessTokenResponseModel
import IdTokenResponseModel
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber

@PerView
class VaultListPresenter @Inject constructor( //
	private val logdeviceEventUsecase: LogDeviceEventUseCase,
	private val importDeploymentVaultUseCase: ImportDeploymentVaultUseCase, //
	private val getDeploymentInfo: GetDeploymentInfoUseCase, //
	private val pollVaultUseCase: PollVaultUseCase,//
	private val updateVaultEtagUseCase: UpdateVaultEtagUseCase,//
	private val getUserAvatarUseCase: GetUserAvatarUseCase,
	private val loginUseCase: LoginUseCase,
	private val refreshTokenUseCase: RefreshTokenUseCase,
	private val getUserProfileUseCase: GetUserProfileUseCase,
	private val getCachedUserProfile: GetCachedUserProfileUseCase,
	private val cacheUserProfile: CacheUserProfileUseCase,
	private val clearUserProfileCache: ClearUserProfileCacheUseCase,
	private val getVaultListUseCase: GetVaultListUseCase,  //
	private val deleteVaultUseCase: DeleteVaultUseCase,  //
	private val deleteVaultsUseCase: DeleteVaultsUseCase,  //
	private val renameVaultUseCase: RenameVaultUseCase,  //
	private val lockVaultUseCase: LockVaultUseCase,  //
	private val getDecryptedCloudForVaultUseCase: GetDecryptedCloudForVaultUseCase,  //
	private val getRootFolderUseCase: GetRootFolderUseCase,  //
	private val addExistingVaultWorkflow: AddExistingVaultWorkflow,  //
	private val createNewVaultWorkflow: CreateNewVaultWorkflow,  //
	private val saveVaultUseCase: SaveVaultUseCase,  //
	private val moveVaultPositionUseCase: MoveVaultPositionUseCase, //
	private val licenseCheckUseCase: DoLicenseCheckUseCase,  //
	private val updateCheckUseCase: DoUpdateCheckUseCase,  //
	private val updateUseCase: DoUpdateUseCase,  //
	private val updateVaultParameterIfChangedRemotelyUseCase: UpdateVaultParameterIfChangedRemotelyUseCase, //
	private val listCBCEncryptedPasswordVaultsUseCase: ListCBCEncryptedPasswordVaultsUseCase, //
	private val removeStoredVaultPasswordsUseCase: RemoveStoredVaultPasswordsUseCase, //
	private val saveVaultsUseCase: SaveVaultsUseCase, //
	private val networkConnectionCheck: NetworkConnectionCheck,  //
	private val fileUtil: FileUtil,  //
	private val authenticationExceptionHandler: AuthenticationExceptionHandler,  //
	private val cloudFolderModelMapper: CloudFolderModelMapper,  //
	private val sharedPreferencesHandler: SharedPreferencesHandler,  //
	private val userProfileModelMapper: UserProfileModelMapper,  //
	private val deviceUtils: DeviceUtils,  //
	private val avatarGenerator: AvatarGenerator,
	exceptionMappings: ExceptionHandlers
) : Presenter<VaultListView>(exceptionMappings) {

	// TAG
	private val TAG = VaultListPresenter::class.java.simpleName

	private var isFistTime = true

	private var vaultAction: VaultAction? = null

	var userProfileModel: UserProfileModel? = null

	val isLoggedIn: Boolean
		get() = userProfileModel != null

	var isVaultsLoaded = false
	var vaultsCount = 0;
	var mVaultList: List<VaultModel> = mutableListOf<VaultModel>()

	private val coroutineScope = CoroutineScope(Dispatchers.IO)
	private val compositeDisposable = CompositeDisposable()

	override fun workflows(): Iterable<Workflow<*>> {
		return listOf(addExistingVaultWorkflow, createNewVaultWorkflow)
	}

	fun onWindowFocusChanged(hasFocus: Boolean) {
		if (hasFocus) {
			loadVaultList()
		}
	}

	fun prepareView() {
		if (!sharedPreferencesHandler.isScreenLockDialogAlreadyShown) {
			val keyguardManager = context().getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
			if (!keyguardManager.isKeyguardSecure) {
				view?.showDialog(AskForLockScreenDialog.newInstance())
			}
			sharedPreferencesHandler.setScreenLockDialogAlreadyShown()
		}

		sharedPreferencesHandler.vaultsRemovedDuringMigration()?.let {
			val cloudNameString = getString(CloudTypeModel.valueOf(CloudType.valueOf(it.first)).displayNameResource)
			view?.showDialog(VaultsRemovedDuringMigrationDialog.newInstance(Pair(cloudNameString, it.second)))
			sharedPreferencesHandler.vaultsRemovedDuringMigration(null)
		}

		checkPermissions()
		loadUserProfile()
	}

	private fun loadUserProfile() {
		val accessToken = sharedPreferencesHandler.getCognitoAccessToken()
		if (accessToken.isEmpty()) {
			Timber.tag(TAG).d("No access token found, user is not logged in")
			return
		}

		// First try to get from cache
		getCachedUserProfile
			.run(object : DefaultResultHandler<UserProfile>() {
				override fun onSuccess(userProfile: UserProfile) {
					Timber.tag(TAG).d("Loaded user profile from cache: $userProfile")
					userProfileModel = userProfileModelMapper.toModel(userProfile)
					view?.updateUserProfile(userProfileModel!!)

					// After getting from cache, refresh from API in background
					refreshUserProfileFromAPI(accessToken)
					loadVaultList()
					
					// Check deployments after loading user profile from cache
					getDeployment()
				}

				override fun onError(e: Throwable) {
					Timber.tag(TAG).d("No cached user profile found, getting from API")
					// If no cache, get from API
					getUserProfile(accessToken)
				}
			})
	}

	private fun refreshUserProfileFromAPI(accessToken: String) {
		view?.showLoading()
		getUserProfileUseCase
			.withAccessToken(accessToken)
			.andDeviceArgs(deviceUtils.getDeviceArgs(context()))
			.run(object : DefaultResultHandler<UserProfile>() {
				override fun onSuccess(userProfile: UserProfile) {
					cacheUserProfileAfterLogin(userProfile)
					loadVaultList()
					view?.hideLoading()
					Timber.tag(TAG).d("Refreshed user profile from API: $userProfile")
					userProfileModel = userProfileModelMapper.toModel(userProfile)
					userProfileModel?.let { view?.updateUserProfile(it) }
					
					// Removed deployment check here - handled elsewhere
				}

				override fun onError(e: Throwable) {
					view?.hideLoading()
					Timber.tag(TAG).e(e, "Failed to refresh user profile from API")

					when (e) {
						is FatalBackendException -> {
							// Check if the error is due to an expired token
							if (e.message?.contains("401") == true) {
								// Try to refresh the token
								refreshToken()
							} else {
								// Handle other authentication errors
								Timber.tag(TAG).d("Authentication error during refresh: ${e.message}")
								// Clear tokens and update UI to show logged out state
								sharedPreferencesHandler.setCognitoAccessToken("")
								view?.updateUserProfileToLoggedOut()
								userProfileModel = null
							}
						}
						// Don't show other errors during background refresh
					}
				}
			})
	}

	private fun shouldUpdateProfile(newProfile: UserProfile): Boolean {
		val currentProfile = userProfileModel
		return currentProfile?.getId() != newProfile.id ||
				currentProfile?.getEmail() != newProfile.email ||
				currentProfile?.getUserName() != newProfile.userName ||
				currentProfile?.getLastLogin() != newProfile.lastLogin ||
				currentProfile?.getLastEventUser() != newProfile.lastEventUser ||
				currentProfile?.getLastEventDevice() != newProfile.lastEventDevice ||
				currentProfile?.getPolicyId() != newProfile.policyId ||
				currentProfile?.getAllowRead() != newProfile.allowRead
	}
	private fun login(accessToken: String) {
		view?.showLoading()
		loginUseCase
			.withAccessToken(accessToken)
			.andDeviceArgs(buildDeviceArgs())
			.run(object : DefaultResultHandler<UserProfile>() {
				override fun onSuccess(userProfile: UserProfile) {
					view?.hideLoading()
					Timber.tag(TAG).d("User profile: $userProfile")
					userProfileModel = userProfileModelMapper.toModel(userProfile)
					view?.updateUserProfile(userProfileModel!!)
					cacheUserProfileAfterLogin(userProfile)
					
					// Check deployments after successful login
					getDeployment()
				}

				override fun onError(e: Throwable) {
					super.onError(e)
					view?.hideLoading()
					Timber.tag(TAG).e(e, "getUserProfileUseCase Failed to get user profile ${e.message}")

					// Check if the error is due to an expired token
					if (e is FatalBackendException && e.message?.contains("401") == true) {
						// Try to refresh the token
						refreshToken()
					} else {
						// For other errors, log out the user
						sharedPreferencesHandler.setCognitoAccessToken("")
						view?.updateUserProfileToLoggedOut()
						userProfileModel = null
					}
				}
			})
	}

	private fun updateVaultEtag(vault: Vault) {

	}


	private fun getUserProfile(accessToken: String) {
		view?.showLoading()
		getUserProfileUseCase
			.withAccessToken(accessToken)
			.andDeviceArgs(buildDeviceArgs())
			.run(object : DefaultResultHandler<UserProfile>() {
				override fun onSuccess(userProfile: UserProfile) {
					loadVaultList()
					view?.hideLoading()
					Timber.tag(TAG).d("User profile: $userProfile")
					userProfileModel = userProfileModelMapper.toModel(userProfile)
					view?.updateUserProfile(userProfileModel!!)
					cacheUserProfileAfterLogin(userProfile)
					
					// Check deployments after successfully loading user profile
					getDeployment()
				}

				override fun onError(e: Throwable) {
					super.onError(e)
					view?.hideLoading()
					Timber.tag(TAG).e(e, "getUserProfileUseCase Failed to get user profile ${e.message}")

					// Check if the error is due to an expired token
					if (e is FatalBackendException && e.message?.contains("401") == true) {
						// Try to refresh the token
						refreshToken()
					} else {
						// For other errors, log out the user
						sharedPreferencesHandler.setCognitoAccessToken("")
						view?.updateUserProfileToLoggedOut()
						userProfileModel = null
					}
				}
			})
	}

	private fun refreshToken() {
		val refreshToken = sharedPreferencesHandler.getCognitoRefreshToken()
		if (refreshToken.isEmpty()) {
			Timber.tag(TAG).d("No refresh token available, logging out user")
			sharedPreferencesHandler.setCognitoAccessToken("")
			view?.updateUserProfileToLoggedOut()
			userProfileModel = null
			return
		}

		Timber.tag(TAG).d("Attempting to refresh token")
		view?.showLoading()

		// Create and execute the RefreshToken use case
		refreshTokenUseCase
			.withRefreshToken(refreshToken)
			.run(object : DefaultResultHandler<String>() {
				override fun onSuccess(newAccessToken: String) {
					Timber.tag(TAG).d("Token refreshed successfully")
					sharedPreferencesHandler.setCognitoAccessToken(newAccessToken)
					// Retry getting the user profile with the new token
					getUserProfile(newAccessToken)
					
					// Removed deployment check here - handled in getUserProfile
				}

				override fun onError(e: Throwable) {
					view?.hideLoading()
					Timber.tag(TAG).e(e, "Failed to refresh token")
					// If refresh fails, log out the user
					sharedPreferencesHandler.setCognitoAccessToken("")
					sharedPreferencesHandler.setCognitoRefreshToken("")
					view?.updateUserProfileToLoggedOut()
					userProfileModel = null
				}
			})
	}

	private fun cacheUserProfileAfterLogin(userProfile: UserProfile) {
		val accessToken = sharedPreferencesHandler.getCognitoAccessToken();
		sharedPreferencesHandler.setUserCognitoId("${userProfile.cognitoID}")
		cacheUserProfile
			.withUserProfile(userProfile)
			.run(object : NoOpResultHandler<Void?>() {
				override fun onSuccess(aVoid: Void?) {
					loadVaultList()
					Timber.tag(TAG).d("User profile cached successfully") 
					
					// Removed deployment check here - handled elsewhere
				}

				override fun onError(e: Throwable) {
					Timber.tag(TAG).e(e, "Failed to cache user profile")
				}
			})

		getUserAvatarUseCase.withAccessToken(accessToken)
			.andUserId(userProfile.id)
			.andUserProfile(userProfile)
			.run(object : DefaultResultHandler<Any>() {
				override fun onSuccess(avatarUrl: Any) {
					Timber.tag(TAG).d("User avatar URL: ")
					view?.displayAvatar()
				}

				override fun onError(e: Throwable) {
					Timber.tag(TAG).e(e, "Failed to get user avatar URL")
				}
			})
	}

	fun renderAvatar(imageView: ImageView) {
		if (getUserAvatar() == null) {
			displayFirstUserLetter(imageView)
			return
		} else {
			getUserAvatar()?.let {
				Picasso.get()
					.load(it)
					.resize(
						context().resources.getDimensionPixelSize(R.dimen.avatar_size),
						context().resources.getDimensionPixelSize(R.dimen.avatar_size) * 2
					)
					.transform(CropCircleTransformation())
					.placeholder(R.drawable.ic_user)
					.centerInside()
					.into(object : com.squareup.picasso.Target {
						override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
							imageView.setImageBitmap(bitmap)
						}

						override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
							displayFirstUserLetter(imageView)
						}

						override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
							imageView.setImageDrawable(placeHolderDrawable)
						}
					})
			}
		}

	}

	private fun displayFirstUserLetter(imageView: ImageView) {
		Timber.tag(TAG).d("Error loading avatar, showing letter avatar")
		// Get first letter of username or email
		var firstLetter = userProfileModel?.getEmail()?.firstOrNull()?.uppercase()
			?: userProfileModel?.getUserName()?.firstOrNull()?.uppercase()
			?: "?"

		val size = context().resources.getDimensionPixelSize(R.dimen.avatar_size)
		val letterAvatar = avatarGenerator.createLetterAvatar(firstLetter, size)
		imageView.setImageBitmap(letterAvatar)
	}

	private fun getUserAvatar(): File? {
		val avatarFile = File(context().cacheDir, "$AVATAR_CACHE_DIR/$AVATAR_FILE_NAME")
		return if (avatarFile.exists()) {
			avatarFile
		} else {
			null
		}
	}

	// clear cache avatar
	private fun clearUserAvatarCache() {
		try {
			val avatarFile = File(context().cacheDir, "$AVATAR_CACHE_DIR/$AVATAR_FILE_NAME")
			if (avatarFile.exists()) {
				avatarFile.delete()
				// Clear Picasso cache for this file
				Picasso.get().invalidate(avatarFile)
				Timber.tag(TAG).d("Avatar cache and Picasso cache cleared")
			} else {
				Timber.tag(TAG).d("No avatar cache to clear")
			}
		} catch (e: Exception) {
			Timber.tag(TAG).e(e, "Failed to clear avatar cache")
		}
	}

	fun performLogin(tokenResponse: TokenResponse) {
		tokenResponse.accessToken?.let {
			sharedPreferencesHandler.setCognitoAccessToken(it)
			login(it)
			AccessTokenResponseModel.parseJwt(it).let { accessTokenResponse ->
				sharedPreferencesHandler.setUserProfileCacheExpires(accessTokenResponse.expirationSeconds)
			}
		}
		tokenResponse.refreshToken?.let {
			sharedPreferencesHandler.setCognitoRefreshToken(it)
		}
		tokenResponse.idToken?.let {
			IdTokenResponseModel.parseJwt(it).let { idTokenResponse ->
				if (idTokenResponse.familyName?.isNotEmpty() == true || idTokenResponse.givenName?.isNotEmpty() == true) {
					sharedPreferencesHandler.setSSOUserFullName(idTokenResponse.getFullName())
				}
			}
		}
		
		// Removed comment about checking deployments - handled in login/getUserProfile
	}

	fun getUserProfile() {
		Timber.tag(TAG).d("Refresh clicked, refreshing user profile")
		val accessToken = sharedPreferencesHandler.getCognitoAccessToken()
		if (accessToken.isNotEmpty()) {
			refreshUserProfileFromAPI(accessToken)
		} else {
			Timber.tag(TAG).d("No access token found, user is not logged in")
		}
	}

	/**
	 * Explicitly check for deployments after user refresh action
	 */
	fun checkDeploymentsAfterRefresh() {
		// Remove this method as deployment checks are now consolidated
	}

	fun signOut(keepVaultsData: Boolean) {
		sharedPreferencesHandler.setCognitoAccessToken("")
		sharedPreferencesHandler.setCognitoRefreshToken("")
		if (!keepVaultsData) {
			getAllVaultsThenDelete()
		}
		clearUserAvatarCache()
		try {
			clearUserProfileCache.run(object : NoOpResultHandler<Void?>() {
				override fun onSuccess(aVoid: Void?) {
					Timber.d("User profile cache cleared")
				}

				override fun onError(e: Throwable) {
					Timber.e(e, "Failed to clear user profile cache")
				}
			})
		} catch (e: Exception) {
			Timber.e(e, "Failed to clear user profile cache")
		}
		view?.updateUserProfileToLoggedOut()
		userProfileModel = null
	}

	private fun getAllVaultsThenDelete() {
		getVaultListUseCase
			.run(object : DefaultResultHandler<List<Vault>>() {
			override fun onSuccess(vaults: List<Vault>) {
				deleteVaults(vaults)
			}
		})
	}

	private fun deleteVaults(vaults: List<Vault>) {
		deleteVaultsUseCase
			.withVaults(vaults)
			.run(object : DefaultResultHandler<List<Long>>() {
				override fun onSuccess(aVoid: List<Long>) {
					Timber.tag(TAG).d("Vaults deleted successfully")
				}

				override fun onError(e: Throwable) {
					Timber.tag(TAG).e(e, "Failed to delete vaults")
				}
			})
	}

	fun buildDeviceArgs(): DeviceArgs {
		var args =  DeviceArgs.buildFromDevice(context());
		Timber.tag(TAG).d("Device args: ${args.toJsonRequest()}")
		sharedPreferencesHandler.setComputerId(args.serial)
		return args
	}

	fun checkLicense() {
		if (BuildConfig.FLAVOR == "apkstore" || BuildConfig.FLAVOR == "fdroid" || BuildConfig.FLAVOR == "lite" || BuildConfig.FLAVOR == "accrescent") {
			licenseCheckUseCase //
				.withLicense("") //
				.run(object : NoOpResultHandler<LicenseCheck>() {
					override fun onSuccess(licenseCheck: LicenseCheck) {
						if (BuildConfig.FLAVOR == "apkstore" && sharedPreferencesHandler.doUpdate()) {
							checkForAppUpdates()
						}
					}

					override fun onError(e: Throwable) {
						val license = if (e is LicenseNotValidException) {
							e.license
						} else {
							""
						}
						val intent = Intent(context(), LicenseCheckActivity::class.java)
						intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
						intent.data = Uri.parse(String.format("app://cryptomator/%s", license))

						try {
							context().startActivity(intent)
						} catch (e: ActivityNotFoundException) {
							Toast.makeText(context(), "Please contact the support.", Toast.LENGTH_LONG).show()
							finish()
						}
					}
				})
		}
	}

	private fun checkForAppUpdates() {
		if (networkConnectionCheck.isPresent) {
			updateCheckUseCase //
				.withVersion(BuildConfig.VERSION_NAME) //
				.run(object : NoOpResultHandler<Optional<UpdateCheck>>() {
					override fun onSuccess(updateCheck: Optional<UpdateCheck>) {
						if (updateCheck.isPresent) {
							updateStatusRetrieved(updateCheck.get(), context())
						} else {
							Timber.tag("VaultListPresenter").i("UpdateCheck finished, latest version")
						}
						sharedPreferencesHandler.updateExecuted()
					}

					override fun onError(e: Throwable) {
						showError(e)
					}
				})
		} else {
			Timber.tag("VaultListPresenter").i("Update check not started due to no internet connection")
		}
	}

	private fun updateStatusRetrieved(updateCheck: UpdateCheck, context: Context) {
		showNextMessage(updateCheck.releaseNote(), context)
	}

	private fun showNextMessage(message: String, context: Context) {
		if (message.isNotEmpty()) {
			view?.showDialog(UpdateAppAvailableDialog.newInstance(message))
		} else {
			view?.showDialog(UpdateAppAvailableDialog.newInstance(context.getText(R.string.dialog_update_available_message).toString()))
		}
	}

	private fun checkPermissions() {
		if (sharedPreferencesHandler.usePhotoUpload()) {
			checkLocalStoragePermissionRegardingAutoUploadAndNotificationPermission()
		} else {
			checkNotificationPermission()
		}
	}

	private fun checkLocalStoragePermissionRegardingAutoUploadAndNotificationPermission() {
		val permissions = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
			arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
		} else {
			arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
		}
		requestPermissions(
			PermissionsResultCallbacks.onLocalStoragePermissionResultForAutoUploadAndCheckNotificationPermission(),  //
			R.string.permission_snackbar_auth_auto_upload,  //
			*permissions
		)
	}

	@Callback
	fun onLocalStoragePermissionResultForAutoUploadAndCheckNotificationPermission(result: PermissionsResult) {
		if (!result.granted()) {
			Timber.tag("VaultListPresenter").e("Local storage permission not granted, auto upload will not work")
		}
		checkNotificationPermission()
	}

	private fun checkNotificationPermission() {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
			requestPermissions(
				PermissionsResultCallbacks.requestNotificationPermission(),  //
				R.string.permission_snackbar_notifications,  //
				Manifest.permission.POST_NOTIFICATIONS
			)
		}
	}

	@Callback
	fun requestNotificationPermission(result: PermissionsResult) {
		if (!result.granted()) {
			Timber.tag("VaultListPresenter").e("Notification permission not granted, notifications will not show")
		}
		checkCBCEncryptedVaults()
	}

	private fun checkCBCEncryptedVaults() {
		listCBCEncryptedPasswordVaultsUseCase
			.run(object : DefaultResultHandler<List<Vault>>() {
				override fun onSuccess(vaults: List<Vault>) {
					if (vaults.isNotEmpty()) {
						view?.showDialog(CBCPasswordVaultsMigrationDialog.newInstance(vaults))
					}
				}

				override fun onError(e: Throwable) {
//					super.onError(e)
				}
			})
	}

	fun cBCPasswordVaultsMigrationClicked(cbcVaults: List<Vault>) {
		val vaultModels = cbcVaults.mapTo(ArrayList()) { VaultModel(it) }
		view?.migrateCBCEncryptedPasswordVaults(vaultModels)
	}

	fun cBCPasswordVaultsMigrationRejected(cbcVaults: List<Vault>) {
		removeStoredVaultPasswordsUseCase
			.withVaults(cbcVaults)
			.run(object : DefaultResultHandler<Void?>() {
				override fun onSuccess(ignore: Void?) {
					loadVaultList()
				}
			})
	}

	fun biometricAuthenticationMigrationFinished(vaultModels: List<VaultModel>) {
		val vaults = vaultModels.map { vaultModel -> vaultModel.toVault() }
		saveVaultsUseCase //
			.withVaults(vaults) //
			.run(object : NoOpResultHandler<List<Vault>>() {
				override fun onSuccess(migratedVaults: List<Vault>) {
					loadVaultList()
				}

				override fun onError(e: Throwable) {
					showError(e)
				}
			})
	}

	fun biometricKeyInvalidated(cbcVaults: List<VaultModel>) {
		val vaults = cbcVaults.map { vaultModel -> vaultModel.toVault() }
		removeStoredVaultPasswordsUseCase
			.withVaults(vaults)
			.run(object : DefaultResultHandler<Void?>() {
				override fun onSuccess(ignore: Void?) {
					loadVaultList()
				}
			})
	}

	fun biometricAuthenticationFailed(cbcVaults: List<VaultModel>) {
		val vaults = cbcVaults.map { vaultModel -> vaultModel.toVault() }
		view?.showDialog(CBCPasswordVaultsMigrationDialog.newInstance(vaults))
	}

	fun loadVaultList() {
		view?.hideVaultCreationHint()
		vaultList
		assertUnlockingVaultIsLocked()
	}

	private fun assertUnlockingVaultIsLocked() {
		if (view?.isShowingDialog(EnterPasswordDialog::class) == true) {
			if (view?.currentDialog() != null) {
				val vaultModel = (view?.currentDialog() as EnterPasswordDialog).vaultModel()
				if (view?.isVaultLocked(vaultModel) == false) {
					view?.closeDialog()
				}
			}
		}
	}

	fun deleteVault(vaultModel: VaultModel) {
		deleteVaultUseCase //
			.withVault(vaultModel.toVault()) //
			.andComputerId(sharedPreferencesHandler.getComputerId()) //
			.run(object : DefaultResultHandler<Long>() {
				override fun onSuccess(vaultId: Long) {
					view?.deleteVaultFromAdapter(vaultId)
				}
			})
	}

	fun renameVault(vaultModel: VaultModel, newVaultName: String?) {
		renameVaultUseCase //
			.withVault(vaultModel.toVault()) //
			.andNewVaultName(newVaultName) //
			.andDeviceArgs(deviceUtils.getDeviceArgs(context())) //
			.run(object : DefaultResultHandler<Vault>() {
				override fun onSuccess(vault: Vault) {
					view?.renameVault(VaultModel(vault))
					view?.closeDialog()
				}

				override fun onError(e: Throwable) {
					if (!authenticationExceptionHandler.handleAuthenticationException( //
							this@VaultListPresenter, e,  //
							ActivityResultCallbacks.renameVaultAfterAuthentication(vaultModel.toVault(), newVaultName)
						)
					) {
						showError(e)
					}
				}
			})
	}

	@Callback
	fun renameVaultAfterAuthentication(result: ActivityResult, vault: Vault?, newVaultName: String?) {
		val cloud = result.getSingleResult(CloudModel::class.java).toCloud()
		val vaultWithUpdatedCloud = Vault.aCopyOf(vault).withCloud(cloud).build()
		renameVault(VaultModel(vaultWithUpdatedCloud), newVaultName)
	}

	private fun browseFilesOf(vault: VaultModel) {
		Timber.tag(TAG).d("DEBUG_NAV: browseFilesOf for vault: ${vault.name} (ID: ${vault.vaultId})")
		Timber.tag(TAG).d("DEBUG_NAV: Vault details - Path: ${vault.path}, CloudType: ${vault.cloudType}")
		if (vault.toVault().cloud != null) {
			val cloud = vault.toVault().cloud
			if (cloud is LocalStorageCloud) {
				Timber.tag(TAG).d("DEBUG_NAV: LocalStorageCloud URI: ${cloud.rootUri()}")
			}
		} else {
			Timber.tag(TAG).e("DEBUG_NAV: ⚠️ Cloud is NULL for vault: ${vault.name} - This will cause navigation issues")
		}
		
		getDecryptedCloudForVaultUseCase //
			.withVault(vault.toVault()) //
			.run(object : DefaultResultHandler<Cloud>() {
				override fun onSuccess(cloud: Cloud) {
					Timber.tag(TAG).d("DEBUG_NAV: getDecryptedCloudForVaultUseCase success, cloud: $cloud")
					getRootFolderAndNavigateInto(cloud)
				}
				
				override fun onError(e: Throwable) {
					Timber.tag(TAG).e(e, "DEBUG_NAV: getDecryptedCloudForVaultUseCase failed")
					
					// Try to get error details - this might help debug the navigation issue
					val detailedMessage = e.message ?: "Unknown error"
					Timber.tag(TAG).e("DEBUG_NAV: Detailed error: $detailedMessage")
					
					// Show error to the user
					showError(e)
				}
			})
	}

	private fun getRootFolderAndNavigateInto(cloud: Cloud) {
		Timber.tag(TAG).d("DEBUG_NAV: getRootFolderAndNavigateInto for cloud: $cloud")
		getRootFolderUseCase //
			.withCloud(cloud) //
			.run(object : DefaultResultHandler<CloudFolder>() {
				override fun onSuccess(folder: CloudFolder) {
					// Get the latest vault from mVaultList
					val cryptoCloud = folder.cloud as CryptoCloud
					val latestVault = mVaultList.find { it.vaultId == cryptoCloud.vault.id }?.toVault() ?: cryptoCloud.vault
					Timber.tag(TAG).d("DEBUG_NAV: getRootFolderUseCase success, folder: $folder")
					Timber.tag(TAG).d("DEBUG_NAV: latestVault: ${latestVault.name} (ID: ${latestVault.id})")
					navigateToVaultContent(latestVault, folder)
				}
				
				override fun onError(e: Throwable) {
					Timber.tag(TAG).e(e, "DEBUG_NAV: getRootFolderUseCase failed")
				}
			})
	}

	private fun lockVault(vaultModel: VaultModel) {
		lockVaultUseCase //
			.withVault(vaultModel.toVault()) //
			.run(object : DefaultResultHandler<Vault>() {
				override fun onSuccess(vault: Vault) {
					logActivity(vault.deviceID ?: "")
					view?.addOrUpdateVault(VaultModel(vault))
				}
			})
	}

	private fun logActivity(deviceID:String) {
		logdeviceEventUsecase //
			.withType("logout")
			.andDeviceId(deviceID)
			.andDeviceArgs(buildDeviceArgs())
			.run(object : DefaultResultHandler<Any>() {
				override fun onSuccess(cloud: Any) {

				}

				override fun onError(e: Throwable) {

				}
			})
	}

	private val vaultList: Unit
		get() {
			getVaultListUseCase
				.run(object : DefaultResultHandler<List<Vault>>() {
				override fun onSuccess(vaults: List<Vault>) {
					val vaultModels = vaults.mapTo(ArrayList()) { VaultModel(it) }
					if (vaultModels.isEmpty()) {
						view?.showVaultCreationHint()
					} else {
						view?.hideVaultCreationHint()
					}
					view?.renderVaultList(vaultModels)
					isVaultsLoaded = true
					vaultsCount = vaults.size
					mVaultList = vaultModels
					if(isFistTime) {
						isFistTime = false
						Timber.tag(TAG).d("First time loading vaults, starting polling")
						checkPolling()
						// Removed deployment check here - handled after user profile is loaded
					}
				}

				override fun onError(e: Throwable) {
					isVaultsLoaded = true
				}
			})
		}

	private fun navigateToVaultContent(vault: Vault, cloudFolder: CloudFolder) {
		if (!isPaused) {
			Timber.tag(TAG).d("DEBUG_NAV: navigateToVaultContent - Vault: %s (ID: %d)", vault.name, vault.id)
			Timber.tag(TAG).d("DEBUG_NAV: navigateToVaultContent - CloudFolder: %s", cloudFolder)
			Timber.tag(TAG).d("DEBUG_NAV: navigateToVaultContent - CloudFolder path: %s", cloudFolder.path)
			if (vault.cloud != null) {
				if (vault.cloud is LocalStorageCloud) {
					val localCloud = vault.cloud as LocalStorageCloud
					Timber.tag(TAG).d("DEBUG_NAV: vault LocalStorage URI: %s", localCloud.rootUri())
				}
			}
			view?.navigateToVaultContent(VaultModel(vault), cloudFolderModelMapper.toModel(cloudFolder))
		}
	}

	fun onVaultLockClicked(vault: VaultModel) {
		lockVault(vault)
	}

	fun onVaultClicked(vault: VaultModel) {
		// Get latest update vault from cached list
		val latestVault = mVaultList.find { it.vaultId == vault.vaultId } ?: vault

		Timber.tag(TAG).d("DEBUG_NAV: onVaultClicked for vault: ${vault.name} (ID: ${vault.vaultId})")
		Timber.tag(TAG).d("DEBUG_NAV: Vault details - Path: ${vault.path}, cloudType: ${vault.cloudType}, fullLocalPath: ${vault.fullLocalPath}")
		if (vault.toVault().cloud != null) {
			val cloud = vault.toVault().cloud
			if (cloud is LocalStorageCloud) {
				Timber.tag(TAG).d("DEBUG_NAV: LocalStorageCloud URI: ${cloud.rootUri()}")
			}
		} else {
			Timber.tag(TAG).e("DEBUG_NAV: ⚠️ Cloud is NULL for vault: ${vault.name} - This will cause navigation issues")
		}

		pollVaultUseCase
			.withVault(latestVault.toVault())
			.run(object : DefaultResultHandler<Vault>() {
				override fun onSuccess(vault: Vault) {
					val vaultModel = VaultModel(vault)
					view?.addOrUpdateVault(vaultModel)
					updateItemToListVaults(vaultModel)
					Timber.tag(TAG).d("DEBUG_NAV: pollVaultUseCase successful, starting vault action")
					Timber.tag(TAG).d("DEBUG_NAV: Updated vault cloud null? ${vault.cloud == null}")
					if (vault.cloud != null && vault.cloud is LocalStorageCloud) {
						Timber.tag(TAG).d("DEBUG_NAV: Updated LocalStorageCloud URI: ${(vault.cloud as LocalStorageCloud).rootUri()}")
					}
					startVaultAction(vaultModel, VaultAction.UNLOCK)
				}

				override fun onError(e: Throwable) {
					Timber.tag(TAG).e(e, "DEBUG_NAV: pollVaultUseCase failed: ${e.message}")
				}
			})
	}

	private fun updateItemToListVaults(vaultModel: VaultModel) {
		mVaultList = mVaultList.map {
			if (it.vaultId == vaultModel.vaultId) {
				vaultModel
			} else {
				it
			}
		}
	}

	private fun startVaultAction(vault: VaultModel, vaultAction: VaultAction) {
		Timber.tag(TAG).d("DEBUG_NAV: startVaultAction for vault: ${vault.name} (ID: ${vault.vaultId}), action: $vaultAction")
		Timber.tag(TAG).d("DEBUG_NAV: Vault has cloud? ${vault.toVault().cloud != null}")
		
		if(vault.getVaultStatus().shouldRejectUnlock()) {
			view?.showVaultIsDisableNotice();
			return
		}
		if (vault.passwordCryptoMode?.equals(CryptoMode.CBC) == true) {
			listCBCEncryptedPasswordVaultsUseCase
				.run(object : DefaultResultHandler<List<Vault>>() {
					override fun onSuccess(vaults: List<Vault>) {
						if (vaults.isNotEmpty()) {
							view?.showDialog(CBCPasswordVaultsMigrationDialog.newInstance(vaults))
						}
					}
				})
		} else {
			this.vaultAction = vaultAction
			val cloud = vault.toVault().cloud
			if (cloud != null) {
				Timber.tag(TAG).d("DEBUG_NAV: Vault has cloud, calling onCloudOfVaultAuthenticated")
				onCloudOfVaultAuthenticated(vault.toVault())
			} else {
				Timber.tag(TAG).d("DEBUG_NAV: Vault has NO cloud, calling onVaultWithoutCloudClickedAndLocked")
				if (vault.isLocked) {
					onVaultWithoutCloudClickedAndLocked(vault)
				} else {
					lockVaultUseCase //
						.withVault(vault.toVault()) //
						.run(object : DefaultResultHandler<Vault>() {
							override fun onSuccess(vault: Vault) {
								onVaultWithoutCloudClickedAndLocked(VaultModel(vault))
							}
						})
				}
			}
		}
	}

	private fun onVaultWithoutCloudClickedAndLocked(vault: VaultModel) {
		Timber.tag(TAG).d("DEBUG_NAV: onVaultWithoutCloudClickedAndLocked for vault: ${vault.name} (ID: ${vault.vaultId})")
		if (isWebdavOrLocal(vault.cloudType)) {
			requestActivityResult( //
				ActivityResultCallbacks.cloudConnectionForVaultSelected(vault),  //
				Intents.cloudConnectionListIntent() //
					.withCloudType(vault.cloudType) //
					.withDialogTitle(context().getString(R.string.screen_cloud_connections_title)) //
					.withFinishOnCloudItemClick(true)
			)
		}
	}

	private fun isWebdavOrLocal(cloudType: CloudTypeModel): Boolean {
		return cloudType == CloudTypeModel.WEBDAV || cloudType == CloudTypeModel.LOCAL
	}

	@Callback
	fun cloudConnectionForVaultSelected(result: ActivityResult, vaultModel: VaultModel) {
		val cloud = result.intent().getSerializableExtra(CloudConnectionListPresenter.SELECTED_CLOUD) as Cloud
		val vault = Vault.aCopyOf(vaultModel.toVault()) //
			.withCloud(cloud) //
			.build()
		saveVaultUseCase //
			.withVault(vault) //
			.run(object : DefaultResultHandler<Vault>() {
				override fun onSuccess(vault: Vault) {
					view?.addOrUpdateVault(VaultModel(vault))
					onCloudOfVaultAuthenticated(vault)
				}
			})
	}

	private fun onCloudOfVaultAuthenticated(authenticatedVault: Vault) {
		Timber.tag(TAG).d("DEBUG_NAV: onCloudOfVaultAuthenticated for vault: ${authenticatedVault.name} (ID: ${authenticatedVault.id})")
		Timber.tag(TAG).d("DEBUG_NAV: authenticatedVault details - path: ${authenticatedVault.path}, cloud type: ${authenticatedVault.cloudType}")
		Timber.tag(TAG).d("DEBUG_NAV: authenticatedVault fullLocalPath: ${authenticatedVault.fullLocalPath}")
		
		if (authenticatedVault.cloud != null) {
			Timber.tag(TAG).d("DEBUG_NAV: Cloud is present (type: ${authenticatedVault.cloud.type()})")
			if (authenticatedVault.cloud is LocalStorageCloud) {
				val localCloud = authenticatedVault.cloud as LocalStorageCloud
				Timber.tag(TAG).d("DEBUG_NAV: LocalStorage URI: ${localCloud.rootUri()}")
			}
		} else {
			Timber.tag(TAG).e("DEBUG_NAV: ⚠️ Cloud is NULL in onCloudOfVaultAuthenticated - This is a critical issue")
		}
		
		val authenticatedVaultModel = VaultModel(authenticatedVault)
		when (vaultAction) {
			VaultAction.UNLOCK -> {
				Timber.tag(TAG).d("DEBUG_NAV: Action is UNLOCK, calling requireUserAuthentication")
				requireUserAuthentication(authenticatedVaultModel)
			}
			VaultAction.RENAME -> {
				Timber.tag(TAG).d("DEBUG_NAV: Action is RENAME, showing rename dialog")
				view?.showRenameDialog(authenticatedVaultModel)
			}
			else -> {
				Timber.tag(TAG).d("DEBUG_NAV: No action specified")
			}
		}
		vaultAction = null
	}

	private fun requireUserAuthentication(authenticatedVault: VaultModel) {
		Timber.tag(TAG).d("DEBUG_NAV: requireUserAuthentication for vault: ${authenticatedVault.name} (ID: ${authenticatedVault.vaultId})")
		Timber.tag(TAG).d("DEBUG_NAV: Vault is locked? ${authenticatedVault.isLocked}")
		
		view?.addOrUpdateVault(authenticatedVault)
		if (authenticatedVault.isLocked) {
			if (!isPaused) {
				Timber.tag(TAG).d("DEBUG_NAV: Vault is locked, requesting unlock activity result")
				requestActivityResult( //
					ActivityResultCallbacks.vaultUnlockedVaultList(), //
					Intents.unlockVaultIntent().withVaultModel(authenticatedVault).withVaultAction(UnlockVaultIntent.VaultAction.UNLOCK)
				)
			}
		} else {
			Timber.tag(TAG).d("DEBUG_NAV: Vault is already unlocked, browsing files")
			browseFilesOf(authenticatedVault)
		}
	}

	@Callback
	fun vaultUnlockedVaultList(result: ActivityResult) {
		val cloud = result.intent().getSerializableExtra(SINGLE_RESULT) as Cloud
		getRootFolderOf(cloud)
	}

	private fun getRootFolderOf(cloud: Cloud) {
		getRootFolderUseCase //
			.withCloud(cloud) //
			.run(object : DefaultResultHandler<CloudFolder>() {
				override fun onSuccess(folder: CloudFolder) {
					navigateToVaultContent(folder)
				}
			})
	}

	private fun navigateToVaultContent(folder: CloudFolder) {
		val cryptoCloud = (folder.cloud as CryptoCloud)
		updateVaultParameterIfChangedRemotelyUseCase //
			.withVault(cryptoCloud.vault) //
			.run(object : DefaultResultHandler<Vault>() {
				override fun onSuccess(vault: Vault) {
					view?.addOrUpdateVault(VaultModel(vault))
					navigateToVaultContent(vault, folder)
					view?.showProgress(ProgressModel.COMPLETED)
					if (checkToStartAutoImageUpload(vault)) {
						val cryptomatorApp = activity().application as CryptomatorApp
						cryptomatorApp.startAutoUpload(cryptoCloud)
					}
				}
			})
	}

	private fun checkToStartAutoImageUpload(vault: Vault): Boolean {
		return if (sharedPreferencesHandler.usePhotoUpload() && sharedPreferencesHandler.photoUploadVault() == vault.id) {
			!sharedPreferencesHandler.autoPhotoUploadOnlyUsingWifi() || networkConnectionCheck.checkWifiOnAndConnected()
		} else false
	}

	fun onAddExistingVault() {
		addExistingVaultWorkflow.start()
	}

	fun onCreateVault() {
		createNewVaultWorkflow.start()
	}

	fun onAddOrCreateVaultCompleted(vault: Vault) {
		view?.addOrUpdateVault(VaultModel(vault))
		view?.hideVaultCreationHint()
		view?.closeDialog()
	}

	fun onChangePasswordClicked(vaultModel: VaultModel) {
		Intents
			.unlockVaultIntent()
			.withVaultModel(vaultModel)
			.withVaultAction(UnlockVaultIntent.VaultAction.CHANGE_PASSWORD)
			.startActivity(this)
	}

	fun onVaultSettingsClicked(vaultModel: VaultModel) {
		view?.showVaultSettingsDialog(vaultModel)
	}

	fun onCreateVaultClicked() {
		// check user before creating vault
		if (!isVaultsLoaded) {
			view?.showError(R.string.please_wait_until_the_vaults_are_loaded)
			return
		}

		if (userProfileModel?.shouldBlockCreateNewVault(vaultsCount) == false) {
			view?.showAddVaultBottomSheet()
		} else {
			view?.showDialogMessage(R.string.create_vault_plan_limit_message)
		}
	}

	fun onRenameVaultClicked(vaultModel: VaultModel) {
		startVaultAction(vaultModel, VaultAction.RENAME)
	}

	fun onAskForLockScreenFinished(setScreenLock: Boolean) {
		if (setScreenLock) {
			try {
				view?.activity()?.startActivity(Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD))
			} catch (e: ActivityNotFoundException) {
				Timber.tag("VaultListPresenter").d(e, "Device Policy Manager not found")
				view?.showError(R.string.error_device_policy_manager_not_found)
			}
		}
	}

	fun onFilteredTouchEventForSecurity() {
		view?.showDialog(AppIsObscuredInfoDialog.newInstance())
	}

	fun onRowMoved(fromPosition: Int, toPosition: Int) {
		view?.rowMoved(fromPosition, toPosition)
	}

	fun onVaultMoved(fromPosition: Int, toPosition: Int) {
		moveVaultPositionUseCase
			.withFromPosition(fromPosition) //
			.andToPosition(toPosition) //
			.run(object : DefaultResultHandler<List<Vault>>() {
				override fun onSuccess(vaults: List<Vault>) {
					view?.vaultMoved(vaults.mapTo(ArrayList()) { VaultModel(it) })
				}

				override fun onError(e: Throwable) {
					Timber.tag("VaultListPresenter").e(e, "Failed to execute MoveVaultUseCase")
				}
			})
	}

	private enum class VaultAction {
		UNLOCK, RENAME
	}

	fun installUpdate() {
		view?.showDialog(UpdateAppDialog.newInstance())
		val uri = fileUtil.contentUriForNewTempFile("cryptomator.apk")
		val file = fileUtil.tempFile("cryptomator.apk")
		updateUseCase //
			.withFile(file) //
			.run(object : NoOpResultHandler<Void?>() {
				override fun onError(e: Throwable) {
					showError(e)
				}

				override fun onSuccess(aVoid: Void?) {
					super.onSuccess(aVoid)
					val intent = Intent(Intent.ACTION_VIEW)
					intent.setDataAndType(uri, "application/vnd.android.package-archive")
					intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
					intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
					context().startActivity(intent)
				}
			})
	}

	fun showUserProfile() {
		view?.showProfileInfoBottomSheet(userProfileModel!!)
	}

	fun checkPolling() {
		Flowable.fromIterable(mVaultList)
			.flatMap({ vaultModel ->
				Flowable.create({ emitter ->
					Timber.tag(TAG).d("Polling vault: ${vaultModel.name}")
					pollVaultUseCase
						.withVault(vaultModel.toVault())
						.run(object : DefaultResultHandler<Vault>() {
							override fun onSuccess(vault: Vault) {
								Timber.tag(TAG).d("pollVaultUseCase successfully")
								val updatedVaultModel = VaultModel(vault)
								view?.addOrUpdateVault(updatedVaultModel)
								// Update mVaultList with the new vault model
								mVaultList = mVaultList.map {
									if (it.vaultId == updatedVaultModel.vaultId) updatedVaultModel else it
								}
								emitter.onNext(vault)
								emitter.onComplete()
							}

							override fun onError(e: Throwable) {
								Timber.tag(TAG).e(e, "pollVaultUseCase failed")
								emitter.onError(e)
							}

							override fun onFinished() {
								// Do nothing, handled by onSuccess/onError
							}
						})
				}, io.reactivex.BackpressureStrategy.LATEST)
			}, 1) // maxConcurrency = 1 ensures sequential execution
			.subscribeOn(Schedulers.io())
			.subscribe(
				{ /* onNext */ },
				{ view?.hidePtrProgress(); },
				{
					view?.hidePtrProgress()
					Timber.tag(TAG).d("Polling completed, updated mVaultList: $mVaultList")
				}
			)
	}

	fun getDeployment() {
		// First check for storage permissions
		if (!hasStoragePermissions()) {
			Timber.tag(TAG).d("Storage permissions not granted, requesting them before getting deployments")
			view?.showRequestStoragePermissionForDeployments()
			return
		}
		
		view?.showLoading()
		getDeploymentInfo
			.withDeviceSerial(buildDeviceArgs().serial)
			.run(object : DefaultResultHandler<List<DeploymentWithStatus>>() {
				override fun onSuccess(deployment: List<DeploymentWithStatus>) {
					view?.hideLoading()
					Timber.tag(TAG).d("Deployment vault: $deployment")
					import(deployment)
//					view?.showDialogMessage("${deployment.map { it.vaultRemote.volumeName }}")
				}

				override fun onError(e: Throwable) {
					view?.hideLoading()
					Timber.tag(TAG).e(e, "Failed to get deployment")
				}
			})
	}

	fun import(list:List<DeploymentWithStatus>){
		// Ensure there are deployments to import
		if (list.isEmpty()) {
			Timber.tag(TAG).d("No deployments to import")
			return
		}
		
		// Show import status with total count
		val totalCount = list.size
		view?.showImportStatus(1, totalCount, 0)
		
		// Start with first deployment (index 0)
		importNextDeployment(list, 0, totalCount)
	}
	
	private fun importNextDeployment(deployments: List<DeploymentWithStatus>, currentIndex: Int, totalCount: Int) {
		// Check if we've completed all deployments
		if (currentIndex >= deployments.size) {
			view?.hideImportStatus()
			loadVaultList()
			return
		}
		
		// Calculate progress percentage
		val progress = ((currentIndex.toFloat() / totalCount) * 100).toInt()
		
		// Update the UI with current progress (currentIndex + 1 because UI displays 1-based index)
		view?.updateImportStatus(currentIndex + 1, totalCount, progress)
		
		// Import the current deployment
		val deployment = deployments[currentIndex]
		importDeploymentVaultUseCase
			.withDeployment(deployment)
			.andComputerId(deviceUtils.getDeviceArgs(context()).serial)
			.run(object : DefaultResultHandler<org.cryptomator.domain.usecases.vault.ImportDeploymentVault.ImportResult?>() {
				override fun onSuccess(result: org.cryptomator.domain.usecases.vault.ImportDeploymentVault.ImportResult?) {
					// Provide UI feedback based on result
					when (result) {
						org.cryptomator.domain.usecases.vault.ImportDeploymentVault.ImportResult.ADDED,
						org.cryptomator.domain.usecases.vault.ImportDeploymentVault.ImportResult.UPDATED,
						org.cryptomator.domain.usecases.vault.ImportDeploymentVault.ImportResult.REMOVED -> {
							loadVaultList() // Refresh UI immediately after each change
						}
						org.cryptomator.domain.usecases.vault.ImportDeploymentVault.ImportResult.SKIPPED,
						org.cryptomator.domain.usecases.vault.ImportDeploymentVault.ImportResult.ERROR, null -> {
							// Optionally show a message or do nothing
						}
					}
					// Import next deployment with incremented counter
					importNextDeployment(deployments, currentIndex + 1, totalCount)
				}
				
				override fun onError(e: Throwable) {
					Timber.tag(TAG).e(e, "Failed to import vault ${currentIndex + 1}/$totalCount")
					// Continue with next deployment despite error
					importNextDeployment(deployments, currentIndex + 1, totalCount)
				}
			})
	}

	/**
	 * Check if the app has the required storage permissions
	 * 
	 * @return true if permissions are granted, false otherwise
	 */
	public fun hasStoragePermissions(): Boolean {
		val context = context() ?: return false
		
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			Environment.isExternalStorageManager()
		} else {
			ContextCompat.checkSelfPermission(
				context,
				Manifest.permission.WRITE_EXTERNAL_STORAGE
			) == PackageManager.PERMISSION_GRANTED
		}
	}

	init {
		unsubscribeOnDestroy(
			logdeviceEventUsecase,
			loginUseCase,
			cacheUserProfile,
			clearUserProfileCache,
			getCachedUserProfile,
			refreshTokenUseCase, //
			getUserProfileUseCase,//
			deleteVaultUseCase,  //
			deleteVaultsUseCase,  //
			renameVaultUseCase,  //
			lockVaultUseCase,  //
			getVaultListUseCase,  //
			saveVaultUseCase,  //
			moveVaultPositionUseCase, //
			licenseCheckUseCase,  //
			updateCheckUseCase,  //
			updateUseCase, //
			listCBCEncryptedPasswordVaultsUseCase, //
			removeStoredVaultPasswordsUseCase, //
			saveVaultsUseCase, //
			updateVaultParameterIfChangedRemotelyUseCase,//
			pollVaultUseCase, //
		)
	}
	companion object {
		const val AVATAR_CACHE_DIR = "avatar_cache"
		const val AVATAR_FILE_NAME = "user_avatar.jpg"
	}

}
