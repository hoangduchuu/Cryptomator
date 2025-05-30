package org.cryptomator.domain.models.policy

import java.io.Serializable

data class NCryptorPolicy(
    val allowOfflineUnlock: Boolean,
    val isDefault: Boolean,
    val driveQuota: Int,
    val restricted: Boolean,
    val autounlockcli: Boolean,
    val enabled: Boolean,
    val autounlockgui: Boolean,
    val driveLimit: Int
) : Serializable

    data class Overrides(
        val fblocker: Fblocker,
        val flogger: Flogger
    ) : Serializable

    data class Fblocker(
        val enabled: Boolean
    ) : Serializable

    data class Flogger(
        val enabled: Boolean
    ) : Serializable