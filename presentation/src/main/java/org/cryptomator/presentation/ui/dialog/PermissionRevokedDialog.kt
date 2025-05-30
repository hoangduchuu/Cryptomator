package org.cryptomator.presentation.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import org.cryptomator.presentation.R

class PermissionRevokedDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_permission_revoked_title)
            .setMessage(R.string.dialog_permission_revoked_message)
            .setPositiveButton(R.string.dialog_permission_revoked_positive_button) { _, _ ->
                // The dialog will be dismissed and the user will be prompted to select the location again
            }
            .setNegativeButton(R.string.dialog_permission_revoked_negative_button) { _, _ ->
                activity?.finish()
            }
            .setCancelable(false)
            .create()
    }

    companion object {
        fun newInstance(): PermissionRevokedDialog {
            return PermissionRevokedDialog()
        }
    }
} 