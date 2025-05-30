package org.cryptomator.presentation.ui.activity

import androidx.fragment.app.Fragment
import org.cryptomator.generator.Activity
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.ActivityLayoutBinding
import org.cryptomator.presentation.model.CloudModel
import org.cryptomator.presentation.model.CloudTypeModel
import org.cryptomator.presentation.presenter.CloudSettingsPresenter
import org.cryptomator.presentation.ui.activity.view.CloudSettingsView
import org.cryptomator.presentation.ui.fragment.CloudSettingsFragment
import org.cryptomator.util.SharedPreferencesHandler
import javax.inject.Inject

@Activity
class CloudSettingsActivity : BaseActivity<ActivityLayoutBinding>(ActivityLayoutBinding::inflate), CloudSettingsView {

	@Inject
	lateinit var cloudSettingsPresenter: CloudSettingsPresenter

	@Inject
	lateinit var preferencesHandler: SharedPreferencesHandler

	override fun setupView() {
		binding.mtToolbar.toolbar.setTitle(R.string.screen_cloud_settings_title)
		setSupportActionBar(binding.mtToolbar.toolbar)
	}

	override fun createFragment(): Fragment = CloudSettingsFragment()

	override fun render(cloudModels: List<CloudModel>) {
		cloudSettingsFragment().showClouds(cloudModels)
	}

	override fun update(cloud: CloudModel) {
		if(cloud.cloudType() == CloudTypeModel.DROPBOX){
			preferencesHandler.setDropboxEmail("${cloud.username()}")
		}

		if(cloud.cloudType() == CloudTypeModel.GOOGLE_DRIVE){
			preferencesHandler.setGoogleDriverEmail("${cloud.username()}")
		}
		cloudSettingsFragment().update(cloud)
	}

	private fun cloudSettingsFragment(): CloudSettingsFragment = getCurrentFragment(R.id.fragment_container) as CloudSettingsFragment
}
