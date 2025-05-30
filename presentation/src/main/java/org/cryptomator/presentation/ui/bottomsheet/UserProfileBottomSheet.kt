package org.cryptomator.presentation.ui.bottomsheet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager.LayoutParams
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.squareup.picasso.Picasso
import org.cryptomator.generator.BottomSheet
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.DialogBottomUserProfileBinding
import org.cryptomator.presentation.model.userprofile.UserProfileModel
import org.cryptomator.presentation.presenter.VaultListPresenter.Companion.AVATAR_CACHE_DIR
import org.cryptomator.presentation.presenter.VaultListPresenter.Companion.AVATAR_FILE_NAME
import org.cryptomator.presentation.ui.dialog.LogOutConfirmationDialog
import org.cryptomator.presentation.util.AvatarGenerator
import org.cryptomator.presentation.util.DeviceUtils
import org.cryptomator.util.SharedPreferencesHandler
import java.io.File
import javax.inject.Inject
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import timber.log.Timber

@BottomSheet(R.layout.dialog_bottom_user_profile)
class UserProfileBottomSheet @Inject constructor(
	private val sharedPreferencesHandler: SharedPreferencesHandler,
	private val avatarGenerator: AvatarGenerator
) : BaseBottomSheet<UserProfileBottomSheet.Callback, DialogBottomUserProfileBinding>(DialogBottomUserProfileBinding::inflate) {

	interface Callback {

		fun onLogoutClick(keepVaultsData: Boolean)
		fun onRefreshClick()
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		setUpDialog()
		setupView();
	}

	override fun setupView() {
		val userProfileModel = requireArguments().getSerializable(PROFILE_ARGS) as UserProfileModel
		updateProfileUI(userProfileModel)

		binding.btnClose.setOnClickListener {
			dismiss()
		}

		binding.btnRefresh.setOnClickListener {
			Timber.d("Refresh button clicked, showing loading indicator")
			showLoading(true)
			callback?.onRefreshClick()
		}

		binding.btnSignOut.setOnClickListener {
			// show another dialog to confirm logout overlay this dialo
			val logOutConfirmationDialog = LogOutConfirmationDialog.newInstance()
			logOutConfirmationDialog.setTargetFragment(this, 0)
			logOutConfirmationDialog.show(requireFragmentManager(), "logOutConfirmationDialog")
			logOutConfirmationDialog.isCancelable = false
			logOutConfirmationDialog.dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
			logOutConfirmationDialog.dialog?.window?.setDimAmount(0.0f)
		}
	}

	private fun updateProfileUI(userProfileModel: UserProfileModel) {
		binding.tvUsername.text = getUserName(userProfileModel)
		binding.tvEmail.text = userProfileModel.getEmail() ?: ""
		binding.tvSubscriptionName.text = userProfileModel.getSubscription()?.getPlanName() ?: ""
		binding.tvNextBilling.text = "${getString(R.string.next_billing)} ${userProfileModel.formatAcquiredDate()}"
		if(!userProfileModel.getSubscription()?.getNextRenewalAmount().isNullOrEmpty()){
			binding.tvRenewalAmount.text = getString(R.string.renewal_amount, userProfileModel.getSubscription()?.getNextRenewalAmount())
		}
		if (getUserAvatar() != null) {
			getUserAvatar().let {
				it?.let { it1 ->
					Picasso.get().load(it1)
						.resize(100, 100)
						.error(R.drawable.no_avatar)
						.transform(CropCircleTransformation())
						.into(object : com.squareup.picasso.Target {
							override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
								binding.ivProfileImage.setImageBitmap(bitmap)
							}

							override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
								displayFirstUserLetter(userProfileModel)
							}

							override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
								binding.ivProfileImage.setImageDrawable(placeHolderDrawable)
							}
						})
				}
			}
		} else displayFirstUserLetter(userProfileModel)
		binding.tvDeviceName.text = "Device: ${DeviceUtils().getDeviceArgs(requireContext()).hostname}"
		binding.tvSerial.text = "ID: ${DeviceUtils().getDeviceArgs(requireContext()).serial}"


		binding.btnCopyId.setOnClickListener {
			// copy user id to clipboard
			val userId = userProfileModel.getId() ?: ""
			copyToClipboard(requireContext(), userId)
		}
	}

	private fun displayFirstUserLetter(userProfileModel: UserProfileModel) {
		// Get first letter of username or email
		var firstLetter = userProfileModel.getEmail()?.firstOrNull()?.uppercase()
			?: userProfileModel.getUserName()?.firstOrNull()?.uppercase()
			?: "?"

		val letterAvatar = avatarGenerator.createLetterAvatar(firstLetter, 100)
		binding.ivProfileImage.setImageBitmap(letterAvatar)
	}

	private fun getUserName(userProfileModel: UserProfileModel): String? {
		return if (!userProfileModel.isSsoUser()) {
			userProfileModel.getUserName()
		} else {
			val cacheSSoUserName = sharedPreferencesHandler.getSSOUserFullName()
			if (cacheSSoUserName.isNullOrEmpty()) {
				userProfileModel.getUserName()
			} else {
				cacheSSoUserName
			}
		}

	}

	private fun getUserAvatar(): File? {
		val avatarFile = File(requireContext().cacheDir, "$AVATAR_CACHE_DIR/$AVATAR_FILE_NAME")
		return if (avatarFile.exists()) {
			avatarFile
		} else {
			null
		}
	}


	private fun copyToClipboard(context: Context, textToCopy: String) {
		val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
		val clipData = ClipData.newPlainText("ID", textToCopy)
		clipboardManager.setPrimaryClip(clipData)

		// Show a confirmation message (optional)
		Toast.makeText(context, "ID copied to clipboard", Toast.LENGTH_SHORT).show()
	}

	fun updateUserProfile(userProfileModel: UserProfileModel) {
		Timber.d("Updating user profile in bottom sheet")
		updateProfileUI(userProfileModel)
		showLoading(false)
	}

	private fun showLoading(show: Boolean) {
		Timber.d("Setting loading state to: $show")
		binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
		binding.btnRefresh.isEnabled = !show
	}

	private fun setUpDialog() {
		// get the user profile model from arguments
		dialog?.window?.apply {
			setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
			setFlags(
				LayoutParams.FLAG_LAYOUT_NO_LIMITS or
						LayoutParams.FLAG_LAYOUT_IN_SCREEN or
						LayoutParams.FLAG_FULLSCREEN,
				LayoutParams.FLAG_LAYOUT_NO_LIMITS or
						LayoutParams.FLAG_LAYOUT_IN_SCREEN or
						LayoutParams.FLAG_LAYOUT_NO_LIMITS
			)
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				attributes.layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
			}
		}

		dialog?.setOnShowListener { dialog ->
			val bottomSheetDialog = dialog as BottomSheetDialog
			val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
			bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
			val behavior = BottomSheetBehavior.from(bottomSheet!!)
			behavior.state = BottomSheetBehavior.STATE_EXPANDED
			behavior.skipCollapsed = true
			behavior.isDraggable = false
			behavior.peekHeight = 0
		}
	}

	fun onLogoutConfirmed(keepVaultsData: Boolean) {
		callback?.onLogoutClick(keepVaultsData = keepVaultsData)
		dismiss()
	}

	companion object {

		private const val PROFILE_ARGS = "profile_args"
		fun newInstance(vaultModel: UserProfileModel, sharedPreferencesHandler: SharedPreferencesHandler): UserProfileBottomSheet {
			val dialog = UserProfileBottomSheet(sharedPreferencesHandler, AvatarGenerator())
			val args = Bundle()
			args.putSerializable(PROFILE_ARGS, vaultModel)
			dialog.arguments = args
			return dialog
		}
	}

}
