package org.cryptomator.presentation.ui.fragment

import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import org.cryptomator.generator.Fragment
import org.cryptomator.presentation.databinding.FragmentVaultListBinding
import org.cryptomator.presentation.model.VaultModel
import org.cryptomator.presentation.model.userprofile.UserProfileModel
import org.cryptomator.presentation.presenter.VaultListPresenter
import org.cryptomator.presentation.ui.adapter.VaultsAdapter
import org.cryptomator.presentation.ui.adapter.VaultsMoveListener
import javax.inject.Inject

@Fragment
class VaultListFragment : BaseFragment<FragmentVaultListBinding>(FragmentVaultListBinding::inflate) {

	@Inject
	lateinit var vaultListPresenter: VaultListPresenter

	@Inject
	lateinit var vaultsAdapter: VaultsAdapter

	lateinit var touchHelper: ItemTouchHelper

	interface SignInClickListener {
		fun onSignInClick()
	}

	private var signInClickListener: SignInClickListener? = null

	fun setSignInClickListener(listener: SignInClickListener) {
		signInClickListener = listener
	}

	private val onItemClickListener = object : VaultsAdapter.OnItemInteractionListener {
		override fun onVaultClicked(vaultModel: VaultModel) {
			vaultListPresenter.onVaultClicked(vaultModel)
		}

		override fun onVaultSettingsClicked(vaultModel: VaultModel) {
			vaultListPresenter.onVaultSettingsClicked(vaultModel)
		}

		override fun onVaultLockClicked(vaultModel: VaultModel) {
			vaultListPresenter.onVaultLockClicked(vaultModel)
		}

		override fun onRowMoved(fromPosition: Int, toPosition: Int) {
			vaultListPresenter.onRowMoved(fromPosition, toPosition)
		}

		override fun onVaultMoved(fromPosition: Int, toPosition: Int) {
			vaultListPresenter.onVaultMoved(fromPosition, toPosition)
		}
	}

	override fun setupView() {
		setupRecyclerView()
		setupSwipeRefresh()
		binding.floatingActionButton.floatingActionButton.setOnClickListener { vaultListPresenter.onCreateVaultClicked() }
		
		// Add click listener for sign in button
		binding.llSignIn.setOnClickListener {
			signInClickListener?.onSignInClick()
		}

		binding.btnSignIn.setOnClickListener {
			signInClickListener?.onSignInClick()
		}

		binding.btnCreateAccount.setOnClickListener {
			signInClickListener?.onSignInClick()
		}

	}

	override fun onResume() {
		super.onResume()
		vaultListPresenter.loadVaultList()
	}

	private fun setupRecyclerView() {
		vaultsAdapter.setCallback(onItemClickListener)
		touchHelper = ItemTouchHelper(VaultsMoveListener(vaultsAdapter))
		touchHelper.attachToRecyclerView(binding.rvVaults.recyclerView)

		binding.rvVaults.recyclerView.layoutManager = LinearLayoutManager(context())
		binding.rvVaults.recyclerView.adapter = vaultsAdapter
		binding.rvVaults.recyclerView.setHasFixedSize(true) // smoother scrolling
		binding.rvVaults.recyclerView.setPadding(0, 0, 0, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 88f, resources.displayMetrics).toInt())
		binding.rvVaults.recyclerView.clipToPadding = false
	}

	private fun setupSwipeRefresh() {
		binding.swipeRefreshLayout.setOnRefreshListener {
			vaultListPresenter.checkPolling()
			vaultListPresenter.checkDeploymentsAfterRefresh()
		}
	}

	fun showVaults(vaultModelCollection: List<VaultModel>?) {
		vaultsAdapter.clear()
		vaultsAdapter.addAll(vaultModelCollection)
		
		// Only show the vault creation hint if the user is logged in and there are no vaults
		if (vaultListPresenter.isLoggedIn && (vaultModelCollection == null || vaultModelCollection.isEmpty())) {
			showVaultCreationHint()
		} else {
			hideVaultCreationHint()
		}

		vaultsAdapter.notifyDataSetChanged()
	}

	fun showVaultCreationHint() {
		binding.rlCreationHint.creationHint.visibility = View.VISIBLE
	}

	fun hideVaultCreationHint() {
		binding.rlCreationHint.creationHint.visibility = View.GONE
	}

	fun isVaultLocked(vaultModel: VaultModel?): Boolean {
		return vaultsAdapter.getItem(vaultsAdapter.positionOf(vaultModel)).isLocked
	}

	fun deleteVaultFromAdapter(vaultId: Long) {
		vaultsAdapter.deleteVault(vaultId)
		if (vaultsAdapter.isEmpty && vaultListPresenter.isLoggedIn) {
			showVaultCreationHint()
		} else {
			hideVaultCreationHint()
		}
	}

	fun addOrUpdateVault(vaultModel: VaultModel?) {
		vaultsAdapter.addOrUpdateVault(vaultModel)
	}

	fun vaultMoved(vaults: List<VaultModel>) {
		vaultsAdapter.clear()
		vaultsAdapter.addAll(vaults)
	}

	fun rowMoved(fromPosition: Int, toPosition: Int) {
		vaultsAdapter.notifyItemMoved(fromPosition, toPosition)
	}

	fun rootView(): View = binding.coordinatorLayout

	fun updateUserProfile(userProfile: UserProfileModel) {
		binding.floatingActionButton.floatingActionButton.visibility = View.VISIBLE
		binding.rlCreationHint.creationHint.visibility = View.VISIBLE
		binding.rvVaults.recyclerView.visibility = View.VISIBLE
		binding.llSignIn.visibility = View.GONE
	}

	fun updateUserProfileToLoggedOut() {
		binding.floatingActionButton.floatingActionButton.visibility = View.INVISIBLE
		binding.rlCreationHint.creationHint.visibility = View.INVISIBLE
		binding.rvVaults.recyclerView.visibility = View.INVISIBLE
		binding.llSignIn.visibility = View.VISIBLE
	}

	fun showLoading() {
		binding.pbLoading.visibility = View.VISIBLE
		binding.swipeRefreshLayout.isRefreshing = false
	}

	fun hideLoading() {
		binding.pbLoading.visibility = View.GONE
		binding.swipeRefreshLayout.isRefreshing = false
	}

	fun hidePtrProgress() {
		binding.swipeRefreshLayout.isRefreshing = false
	}

	/**
	 * Shows the import status indicator with the given information
	 * 
	 * @param currentIndex Current vault being imported (1-based)
	 * @param totalCount Total number of vaults to import
	 * @param progress Progress percentage (0-100)
	 */
	fun showImportStatus(currentIndex: Int, totalCount: Int, progress: Int) {
		binding.importStatusContainer.visibility = View.VISIBLE
		binding.importStatusText.text = "Importing vault $currentIndex/$totalCount"
		binding.importProgressBar.progress = progress
	}
	
	/**
	 * Updates the import status indicator with new progress information
	 * 
	 * @param currentIndex Current vault being imported (1-based)
	 * @param totalCount Total number of vaults to import
	 * @param progress Progress percentage (0-100)
	 */
	fun updateImportStatus(currentIndex: Int, totalCount: Int, progress: Int) {
		binding.importStatusText.text = "Importing vault $currentIndex/$totalCount"
		binding.importProgressBar.progress = progress
	}
	
	/**
	 * Hides the import status indicator
	 */
	fun hideImportStatus() {
		binding.importStatusContainer.visibility = View.GONE
	}
}
