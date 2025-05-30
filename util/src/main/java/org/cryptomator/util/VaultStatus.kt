package org.cryptomator.util


enum class VaultStatus {
	DisablePending, Disabled,
	Reset, ResetPending,
	InUse, ReadonlyPending, ReadOnly,
	Unknown,
}


fun VaultStatus.shouldRejectUnlock(): Boolean {
	return when (this) {
		VaultStatus.DisablePending, VaultStatus.Disabled -> true
		else -> false
	}
}


class VaultStatusParser {
	companion object {
		fun parse(status: String?): VaultStatus {
			return if (status != null) {
				when (status) {
					"IN_USE" -> VaultStatus.InUse
					"DISABLE_PENDING" -> VaultStatus.DisablePending
					"DISABLED" -> VaultStatus.Disabled
					"RESET" -> VaultStatus.Reset
					"RESET_PENDING" -> VaultStatus.ResetPending
					"READ_ONLY_PENDING" -> VaultStatus.ReadonlyPending
					"READ_ONLY" -> VaultStatus.ReadOnly
					else -> VaultStatus.Unknown
				}
			} else {
				VaultStatus.Unknown
			}
		}
		fun toString(status: VaultStatus): String {
			return when (status) {
				VaultStatus.InUse -> "IN_USE"
				VaultStatus.DisablePending -> "DISABLE_PENDING"
				VaultStatus.Disabled -> "DISABLED"
				VaultStatus.Reset -> "RESET"
				VaultStatus.ResetPending -> "RESET_PENDING"
				VaultStatus.ReadonlyPending -> "READ_ONLY_PENDING"
				VaultStatus.ReadOnly -> "READ_ONLY"
				else -> "UNKNOWN"
			}
		}
	}



}

