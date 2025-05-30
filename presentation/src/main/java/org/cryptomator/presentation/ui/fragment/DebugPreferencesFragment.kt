package org.cryptomator.presentation.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import org.cryptomator.data.BuildConfig
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.FragmentDebugPreferencesBinding

class DebugPreferencesFragment : PreferenceFragmentCompat() {
    private var _binding: FragmentDebugPreferencesBinding? = null
    private val binding get() = _binding!!

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.debug_preferences, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebugPreferencesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Display all configuration parameters
        binding.apply {
            tvApiBaseUrl.text = "API Base URL: ${BuildConfig.API_BASE_URL}"
            tvCognitoIssuer.text = "Cognito Issuer: ${BuildConfig.COGNITO_ISSUER}"
            tvCognitoClientId.text = "Cognito Client ID: ${BuildConfig.COGNITO_CLIENT_ID}"
            tvEndSessionRedirectUri.text = "End Session Redirect URI: ${BuildConfig.END_SESSION_REDIRECT_URI}"
            tvBuildType.text = "Build Type: ${BuildConfig.BUILD_TYPE}"
            tvVersionName.text = "Version Name: ${BuildConfig.VERSION_NAME}"
            tvVersionCode.text = "Version Code: ${BuildConfig.VERSION_CODE}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 