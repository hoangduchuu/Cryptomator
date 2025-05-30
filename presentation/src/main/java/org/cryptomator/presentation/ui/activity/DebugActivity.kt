package org.cryptomator.presentation.ui.activity

import android.os.Bundle
import org.cryptomator.data.BuildConfig
import org.cryptomator.generator.Activity
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.ActivityDebugBinding

@Activity
class DebugActivity : BaseActivity<ActivityDebugBinding>(ActivityDebugBinding::inflate) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupToolbar()
        displayDebugInfo()
    }

    private fun setupToolbar() {
        binding.mtToolbar.title = getString(R.string.debug_info_title)
        setSupportActionBar(binding.mtToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun displayDebugInfo() {
        binding.apply {
            tvApiBaseUrl.text = "🟢 API Base URL:\n\t ${BuildConfig.API_BASE_URL}"
            tvCognitoIssuer.text = "\n\n 🟢 Cognito Issuer:\n\t ${BuildConfig.COGNITO_ISSUER}"
            tvCognitoClientId.text = "\n\n 🟢 Cognito Client ID:\n\t ${BuildConfig.COGNITO_CLIENT_ID}"
            tvEndSessionRedirectUri.text = "\n\n 🟢 End Session Redirect URI:\n\t ${BuildConfig.END_SESSION_REDIRECT_URI}"
            tvBuildType.text = "\n\n 🟢 Build Type:\n\t ${BuildConfig.BUILD_TYPE}"
            tvVersionName.text = "\n\n 🟢 Version Name:\n\t ${BuildConfig.VERSION_NAME}"
            tvVersionCode.text = "\n\n 🟢 Version Code:\n\t ${BuildConfig.VERSION_CODE}"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 