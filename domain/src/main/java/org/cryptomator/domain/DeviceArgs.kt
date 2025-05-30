package org.cryptomator.domain

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import java.util.UUID
import timber.log.Timber


/**
 * Model representing device information with properties for system details.
 */
class DeviceArgs private constructor(
	val distro: String,
	val fqdn: String,
	val release: String,
	val kernel: String,
	val logofile: String,
	val clientVersion: String,
	val platform: String,
	val hostname: String,
	val serial: String,
	val build: String,
	val codename: String,
	val arch: String
) {

	/**
	 * Converts this DeviceArgs instance to a Map<String, String>
	 * @return Map of property names to values
	 */
	fun toMap(): Map<String, String> {
		return mapOf(
			"distro" to distro,
			"fqdn" to fqdn,
			"release" to release,
			"kernel" to kernel,
			"logofile" to logofile,
			"clientVersion" to clientVersion,
			"platform" to platform,
			"hostname" to hostname,
			"serial" to serial,
			"build" to build,
			"codename" to codename,
			"arch" to arch
		)
	}

	/**
	 * Converts this DeviceArgs instance to a nested Map that matches the JSON structure:
	 * {
	 *   "computer": {
	 *     "distro": "...",
	 *     ...
	 *   }
	 * }
	 * @return Nested Map matching the expected JSON format
	 */
	fun toJsonRequest(): Map<String, Map<String, String>> {
		return mapOf("computer" to toMap())
	}

	/**
	 * Builder class for DeviceArgs
	 */
	class Builder {

		private var distro: String = ""
		private var fqdn: String = ""
		private var release: String = ""
		private var kernel: String = ""
		private var logofile: String = ""
		private var clientVersion: String = ""
		private var platform: String = ""
		private var hostname: String = ""
		private var serial: String = ""
		private var build: String = ""
		private var codename: String = ""
		private var arch: String = ""

		fun distro(distro: String) = apply { this.distro = distro }
		fun fqdn(fqdn: String) = apply { this.fqdn = fqdn }
		fun release(release: String) = apply { this.release = release }
		fun kernel(kernel: String) = apply { this.kernel = kernel }
		fun logofile(logofile: String) = apply { this.logofile = logofile }
		fun clientVersion(clientVersion: String) = apply { this.clientVersion = clientVersion }
		fun platform(platform: String) = apply { this.platform = platform }
		fun hostname(hostname: String) = apply { this.hostname = hostname }
		fun serial(serial: String) = apply { this.serial = serial }
		fun build(build: String) = apply { this.build = build }
		fun codename(codename: String) = apply { this.codename = codename }
		fun arch(arch: String) = apply { this.arch = arch }

		fun build() = DeviceArgs(
			distro,
			fqdn,
			release,
			kernel,
			logofile,
			clientVersion,
			platform,
			hostname,
			serial,
			build,
			codename,
			arch
		)
	}

	companion object {

		fun builder(): Builder = Builder()

		/**
		 * Creates a DeviceArgs instance from a JSON-formatted Map
		 * @param jsonMap Map with format {"computer": {"distro": "...", ...}}
		 * @return DeviceArgs instance
		 */
		fun fromJsonMap(jsonMap: Map<String, Map<String, String>>): DeviceArgs {
			val computerMap = jsonMap["computer"] ?: throw IllegalArgumentException("Missing 'computer' object in JSON map")

			return builder()
				.distro(computerMap["distro"] ?: "")
				.fqdn(computerMap["fqdn"] ?: "")
				.release(computerMap["release"] ?: "")
				.kernel(computerMap["kernel"] ?: "")
				.logofile(computerMap["logofile"] ?: "")
				.clientVersion(computerMap["clientVersion"] ?: "")
				.platform(computerMap["platform"] ?: "")
				.hostname(computerMap["hostname"] ?: "")
				.serial(computerMap["serial"] ?: "")
				.build(computerMap["build"] ?: "")
				.codename(computerMap["codename"] ?: "")
				.arch(computerMap["arch"] ?: "")
				.build()
		}

		/**
		 * Gets a UUID for the device based on its Android ID
		 * @param context Android context
		 * @return UUID as string
		 */
		fun getHashedDeviceUuid(context: Context): String {
			val androidId = Settings.Secure.getString(
				context.contentResolver,
				Settings.Secure.ANDROID_ID
			)
			Log.d("DeviceArgs", "Android ID: $androidId")
			return UUID.nameUUIDFromBytes(androidId.toByteArray()).toString()
		}

		/**
		 * Extracts just the version number from the full version string
		 * @param fullVersion The full version string (e.g., "1.1.0-SNAPSHOT")
		 * @return The cleaned version (e.g., "1.1.0")
		 */
		private fun extractVersionNumber(fullVersion: String): String {
			return fullVersion.split("-")[0]
		}

		/**
		 * Builds a DeviceArgs instance with values from the current device
		 * @param context Android context
		 * @return DeviceArgs instance with device information
		 */
		fun buildFromDevice(context: Context): DeviceArgs {
			// Get device UUID once to use in multiple places
			val deviceUuid = getHashedDeviceUuid(context)
			Timber.d("Device UUID: $deviceUuid")
			val shortUuid = deviceUuid.substring(0, 8)

			// Map SDK_INT to official Android codename
			val versionCodename = when (Build.VERSION.SDK_INT) {
				Build.VERSION_CODES.BASE -> "Base"
				Build.VERSION_CODES.BASE_1_1 -> "Base 1.1"
				Build.VERSION_CODES.CUPCAKE -> "Cupcake"
				Build.VERSION_CODES.DONUT -> "Donut"
				Build.VERSION_CODES.ECLAIR -> "Eclair"
				Build.VERSION_CODES.ECLAIR_0_1 -> "Eclair 0.1"
				Build.VERSION_CODES.ECLAIR_MR1 -> "Eclair MR1"
				Build.VERSION_CODES.FROYO -> "Froyo"
				Build.VERSION_CODES.GINGERBREAD -> "Gingerbread"
				Build.VERSION_CODES.GINGERBREAD_MR1 -> "Gingerbread MR1"
				Build.VERSION_CODES.HONEYCOMB -> "Honeycomb"
				Build.VERSION_CODES.HONEYCOMB_MR1 -> "Honeycomb MR1"
				Build.VERSION_CODES.HONEYCOMB_MR2 -> "Honeycomb MR2"
				Build.VERSION_CODES.ICE_CREAM_SANDWICH -> "Ice Cream Sandwich"
				Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 -> "Ice Cream Sandwich MR1"
				Build.VERSION_CODES.JELLY_BEAN -> "Jelly Bean"
				Build.VERSION_CODES.JELLY_BEAN_MR1 -> "Jelly Bean MR1"
				Build.VERSION_CODES.JELLY_BEAN_MR2 -> "Jelly Bean MR2"
				Build.VERSION_CODES.KITKAT -> "KitKat"
				Build.VERSION_CODES.KITKAT_WATCH -> "KitKat Watch"
				Build.VERSION_CODES.LOLLIPOP -> "Lollipop"
				Build.VERSION_CODES.LOLLIPOP_MR1 -> "Lollipop MR1"
				Build.VERSION_CODES.M -> "Marshmallow"
				Build.VERSION_CODES.N -> "Nougat"
				Build.VERSION_CODES.N_MR1 -> "Nougat MR1"
				Build.VERSION_CODES.O -> "Oreo"
				Build.VERSION_CODES.O_MR1 -> "Oreo MR1"
				Build.VERSION_CODES.P -> "Pie"
				Build.VERSION_CODES.Q -> "Q"
				Build.VERSION_CODES.R -> "R"
				Build.VERSION_CODES.S -> "Snow Cone"
				Build.VERSION_CODES.S_V2 -> "Snow Cone V2"
				Build.VERSION_CODES.TIRAMISU -> "Tiramisu"
				Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "UpsideDownCake"
				else -> if (Build.VERSION.CODENAME == "REL") "Unknown" else Build.VERSION.CODENAME
			}

			// Extract clean version number from BuildConfig.VERSION_NAME
			val cleanVersionNumber = extractVersionNumber(BuildConfig.VERSION_NAME)

			return builder()
				.distro("Android ${Build.VERSION.RELEASE}")
				.fqdn("android-${shortUuid}.local")
				.release(Build.VERSION.RELEASE)
				.kernel(System.getProperty("os.version") ?: "")
				.logofile("android")
				.clientVersion(cleanVersionNumber)
				.platform("android")
				.hostname(getDeviceName(context))
				.serial(deviceUuid)
				.build(Build.FINGERPRINT.split("/").getOrNull(3) ?: Build.ID)
				.codename(versionCodename)
				.arch(Build.SUPPORTED_ABIS[0].split("-")[0]) // Get base architecture without variant
				.build()
		}

		/** Returns the consumer friendly device name - device manufacturer + model
		 * example: dev-keans-android-pixel9
		 * */
		private fun getDeviceName(context: Context): String {
			try {
				val name = Settings.Global.getString(context.contentResolver, "device_name").replace(" ", "-");
				val regex = Regex("[^a-zA-Z0-9-]")
				val hostName = regex.replace(name, "")
				return hostName;
			} catch (e: Exception) {
				val manufacturer = Build.MANUFACTURER
				val model = Build.MODEL
				val deviceName = "$manufacturer-$model"
				return capitalize(deviceName)
			}
		}

		private fun capitalize(str: String): String {
			if (TextUtils.isEmpty(str)) {
				return str
			}
			val arr = str.toCharArray()
			var capitalizeNext = true
			var phrase = ""
			for (c in arr) {
				if (capitalizeNext && Character.isLetter(c)) {
					phrase += c.uppercaseChar()
					capitalizeNext = false
					continue
				} else if (Character.isWhitespace(c)) {
					capitalizeNext = true
				}
				phrase += c
			}
			return phrase
		}

	}


}