package org.cryptomator.presentation.di.module;

import android.content.Context;

import org.cryptomator.presentation.CryptomatorApp;
import org.cryptomator.presentation.util.DeviceUtils;
import org.cryptomator.util.SharedPreferencesHandler;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ApplicationModule {

	private final CryptomatorApp application;

	public ApplicationModule(CryptomatorApp application) {
		this.application = application;
	}

	@Provides
	@Singleton
	Context provideApplicationContext() {
		return application;
	}

	@Provides
	@Singleton
	SharedPreferencesHandler provideSharedPreferencesHandler(Context context) {
		return new SharedPreferencesHandler(context);
	}

	@Provides
	@Singleton
	DeviceUtils provideDeviceUtils() {
		return new DeviceUtils();
	}
}
