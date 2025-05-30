package org.cryptomator.presentation.ui.dialog

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.cryptomator.generator.Dialog
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.DialogConfigmLogoutBinding
import org.cryptomator.presentation.ui.bottomsheet.UserProfileBottomSheet
import timber.log.Timber

@Dialog
class LogOutConfirmationDialog : BaseDialog<LogOutConfirmationDialog.Callback, DialogConfigmLogoutBinding>(DialogConfigmLogoutBinding::inflate) {

	interface Callback {
		fun onDeleteConfirmedClick()
	}

	public override fun setupDialog(builder: AlertDialog.Builder): android.app.Dialog {
		binding.keepVaultsData.isChecked = false
		builder.setTitle("Logout?") //
			.setPositiveButton(getString(R.string.dialog_unable_to_share_positive_button)) { _: DialogInterface, _: Int ->
				val targetFragment = targetFragment

				val keepVaultsData = binding.keepVaultsData.isChecked
				if (targetFragment is UserProfileBottomSheet) {
					targetFragment.onLogoutConfirmed(keepVaultsData = keepVaultsData)
				}
			} //
			.setNegativeButton(getString(R.string.dialog_button_cancel)) { _: DialogInterface, _: Int -> }
		return builder.create()
	}

	public override fun setupView() {
		binding.keepVaultsData.setOnCheckedChangeListener { _, isChecked ->
			binding.keepVaultsData.isChecked = isChecked
		}
	}

	companion object {
		fun newInstance(): DialogFragment {
			return LogOutConfirmationDialog()
		}
	}
}
