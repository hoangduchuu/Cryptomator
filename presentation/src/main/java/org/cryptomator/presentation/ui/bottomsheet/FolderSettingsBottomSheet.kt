package org.cryptomator.presentation.ui.bottomsheet

import android.os.Bundle
import android.view.View
import org.cryptomator.generator.BottomSheet
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.DialogBottomSheetFolderSettingsBinding
import org.cryptomator.presentation.model.CloudFolderModel
import org.cryptomator.presentation.model.CloudNodeModel
import org.cryptomator.presentation.model.VaultModel
import org.cryptomator.util.VaultStatus

@BottomSheet(R.layout.dialog_bottom_sheet_folder_settings)
class FolderSettingsBottomSheet : BaseBottomSheet<FolderSettingsBottomSheet.Callback, DialogBottomSheetFolderSettingsBinding>(DialogBottomSheetFolderSettingsBinding::inflate) {

	interface Callback {

		fun onShareFolderClicked(cloudFolderModel: CloudFolderModel)
		fun onRenameFolderClicked(cloudFolderModel: CloudFolderModel)
		fun onDeleteNodeClicked(cloudFolderModel: CloudNodeModel<*>)
		fun onMoveFolderClicked(cloudFolderModel: CloudFolderModel)
		fun onExportFolderClicked(cloudFolderModel: CloudFolderModel)
	}

	override fun setupView() {
		val cloudFolderModel = requireArguments().getSerializable(FOLDER_ARG) as CloudFolderModel
		val parentFolderPath = requireArguments().getString(PARENT_FOLDER_PATH_ARG)
		val vaultModel = requireArguments().getSerializable(VAULT_ARG) as VaultModel?

		binding.tvFolderName.text = cloudFolderModel.name
		binding.tvFolderPath.text = parentFolderPath

		binding.shareFolder.setOnClickListener {
			callback?.onShareFolderClicked(cloudFolderModel)
			dismiss()
		}
		binding.renameFolder.setOnClickListener {
			callback?.onRenameFolderClicked(cloudFolderModel)
			dismiss()
		}
		binding.moveFolder.setOnClickListener {
			callback?.onMoveFolderClicked(cloudFolderModel)
			dismiss()
		}
		binding.exportFolder.visibility = View.VISIBLE
		binding.exportFolder.setOnClickListener {
			callback?.onExportFolderClicked(cloudFolderModel)
			dismiss()
		}
		binding.deleteFolder.setOnClickListener {
			callback?.onDeleteNodeClicked(cloudFolderModel)
			dismiss()
		}


		if(vaultModel?.getVaultStatus() == VaultStatus.ReadOnly || vaultModel?.getVaultStatus() == VaultStatus.ReadonlyPending) {
			binding.renameFolder.setTextColor(requireContext().getColor(R.color.colorGray))
			binding.renameFolder.isEnabled = false

			binding.moveFolder.setTextColor(requireContext().getColor(R.color.colorGray))
			binding.moveFolder.isEnabled = false

			binding.deleteFolder.setTextColor(requireContext().getColor(R.color.colorGray))
			binding.deleteFolder.isEnabled = false
		}
	}

	companion object {

		private const val FOLDER_ARG = "folder"
		private const val PARENT_FOLDER_PATH_ARG = "parentFolderPath"
		private const val VAULT_ARG = "vault"

		fun newInstance(cloudFolderModel: CloudFolderModel, parentFolderPath: String, vault: VaultModel?): FolderSettingsBottomSheet {
			val dialog = FolderSettingsBottomSheet()
			val args = Bundle()
			args.putSerializable(FOLDER_ARG, cloudFolderModel)
			args.putString(PARENT_FOLDER_PATH_ARG, parentFolderPath)
			args.putSerializable(VAULT_ARG, vault)
			dialog.arguments = args
			return dialog
		}
	}
}
