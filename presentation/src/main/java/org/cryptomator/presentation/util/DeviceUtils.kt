package org.cryptomator.presentation.util

import android.content.Context
import org.cryptomator.domain.DeviceArgs
import javax.inject.Inject
import javax.inject.Singleton

// static method return the DeviceArgs
@Singleton
class  DeviceUtils  @Inject constructor(){
	companion object {
		fun getDeviceArgs(context: Context): DeviceArgs {
			return DeviceArgs.buildFromDevice(context);
		}
	}

	/**
	 * Returns device arguments built from the current device
	 * @param context Android context
	 * @return DeviceArgs containing device information
	 */
	fun getDeviceArgs(context: Context): DeviceArgs {
		return DeviceArgs.buildFromDevice(context)
	}
}