package org.cryptomator.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.ItemVaultBinding
import org.cryptomator.presentation.model.VaultModel
import org.cryptomator.presentation.model.comparator.VaultPositionComparator
import org.cryptomator.presentation.ui.adapter.VaultsAdapter.VaultViewHolder
import org.cryptomator.presentation.util.FileSizeHelper
import org.cryptomator.util.VaultStatus
import javax.inject.Inject

class VaultsAdapter @Inject
internal constructor(private val fileSizeHelper: FileSizeHelper) : RecyclerViewBaseAdapter<VaultModel, VaultsAdapter.OnItemInteractionListener, VaultViewHolder, ItemVaultBinding>(VaultPositionComparator()), VaultsMoveListener.Listener {

	interface OnItemInteractionListener {

		fun onVaultClicked(vaultModel: VaultModel)

		fun onVaultSettingsClicked(vaultModel: VaultModel)

		fun onVaultLockClicked(vaultModel: VaultModel)

		fun onRowMoved(fromPosition: Int, toPosition: Int)

		fun onVaultMoved(fromPosition: Int, toPosition: Int)
	}

	override fun getItemBinding(inflater: LayoutInflater, parent: ViewGroup?, viewType: Int): ItemVaultBinding {
		return ItemVaultBinding.inflate(inflater, parent, false)
	}

	override fun createViewHolder(binding: ItemVaultBinding, viewType: Int): VaultViewHolder {
		return VaultViewHolder(binding)
	}

	fun deleteVault(vaultID: Long) {
		deleteItem(getVault(vaultID))
	}

	fun addOrUpdateVault(vault: VaultModel?) {
		if (contains(vault)) {
			replaceItem(vault)
		} else {
			addItem(vault)
		}
		notifyDataSetChanged()
	}

	private fun getVault(vaultId: Long): VaultModel? {
		return itemCollection.firstOrNull { it.vaultId == vaultId }
	}

	inner class VaultViewHolder(private val binding: ItemVaultBinding) : RecyclerViewBaseAdapter<*, *, *, *>.ItemViewHolder(binding.root) {

		override fun bind(position: Int) {
			val vaultModel = getItem(position)

			binding.vaultName.text = vaultModel.name
			binding.vaultPath.text = vaultModel.getDisPlayPath()

			binding.cloudImage.setImageResource(vaultModel.cloudType.vaultImageResource)

			if (vaultModel.isLocked) {
				binding.unlockedImage.visibility = View.GONE
			} else {
				binding.unlockedImage.visibility = View.VISIBLE
			}

			itemView.setOnClickListener {
				binding.cloudImage.setImageResource(vaultModel.cloudType.vaultSelectedImageResource)
				callback.onVaultClicked(vaultModel)
			}

			binding.unlockedImage.setOnClickListener { callback.onVaultLockClicked(vaultModel) }

			binding.settings.setOnClickListener {
				binding.cloudImage.setImageResource(vaultModel.cloudType.vaultSelectedImageResource)
				callback.onVaultSettingsClicked(vaultModel)
			}

			// render vault status
			if (vaultModel.getVaultStatus() == VaultStatus.DisablePending
				|| vaultModel.getVaultStatus() == VaultStatus.Disabled
				|| vaultModel.getVaultStatus() == VaultStatus.ReadOnly
				|| vaultModel.getVaultStatus() == VaultStatus.ReadonlyPending
			) {
				binding.tvStatus.visibility = View.VISIBLE
				binding.tvStatus.text = vaultModel.getDisplayStatus()
			} else {
				binding.tvStatus.visibility = View.GONE
			}

			// update color to secondary color for disabled status
			if (vaultModel.getVaultStatus() == VaultStatus.ReadonlyPending || vaultModel.getVaultStatus() == VaultStatus.ReadOnly) {
				binding.tvStatus.setBackgroundColor(itemView.resources.getColor(R.color.colorSecondary))
			}else{
				binding.tvStatus.setBackgroundColor(itemView.resources.getColor(R.color.error))
			}

			// render vault size
			if(vaultModel.size != null && vaultModel.size > 0) {
				binding.tvVaultSize.visibility = View.VISIBLE
				binding.tvVaultSize.text = fileSizeHelper.getFormattedFileSize(vaultModel.size) ?: itemView.context.getString(R.string.file_size_zero)
			} else {
				binding.tvVaultSize.visibility = View.VISIBLE
				binding.tvVaultSize.text = itemView.context.getString(R.string.file_size_zero)
			}
		}
	}

	override fun onVaultMoved(fromPosition: Int, toPosition: Int) {
		callback.onVaultMoved(fromPosition, toPosition)
	}

	override fun onRowMoved(fromPosition: Int, toPosition: Int) {
		callback.onRowMoved(fromPosition, toPosition)
	}
}
