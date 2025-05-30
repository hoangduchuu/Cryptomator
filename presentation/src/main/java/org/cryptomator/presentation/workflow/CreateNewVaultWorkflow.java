package org.cryptomator.presentation.workflow;

import android.content.Context;

import org.cryptomator.domain.CloudFolder;
import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.device.CreateVaultLimitExeception;
import org.cryptomator.domain.models.userprofile.UserProfile;
import org.cryptomator.domain.usecases.cloud.GetRootFolderUseCase;
import org.cryptomator.domain.usecases.user.GetCachedUserProfileUseCase;
import org.cryptomator.domain.usecases.vault.CreateVaultUseCase;
import org.cryptomator.generator.Callback;
import org.cryptomator.presentation.R;
import org.cryptomator.presentation.model.CloudFolderModel;
import org.cryptomator.presentation.model.CloudModel;
import org.cryptomator.presentation.model.ProgressModel;
import org.cryptomator.presentation.model.VaultCreationData;
import org.cryptomator.presentation.model.mappers.CloudModelMapper;
import org.cryptomator.presentation.model.userprofile.UserProfileModel;
import org.cryptomator.presentation.presenter.ChooseCloudServicePresenter;
import org.cryptomator.presentation.presenter.Presenter;
import org.cryptomator.presentation.presenter.VaultListPresenter;
import org.cryptomator.presentation.util.DeviceUtils;
import org.cryptomator.util.SharedPreferencesHandler;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

import javax.inject.Inject;

import static org.cryptomator.presentation.intent.ChooseCloudNodeSettings.chooseCloudNodeSettings;
import static org.cryptomator.presentation.intent.Intents.browseFilesIntent;
import static org.cryptomator.presentation.intent.Intents.chooseCloudServiceIntent;
import static org.cryptomator.presentation.intent.Intents.createVaultIntent;
import static org.cryptomator.presentation.intent.Intents.setPasswordIntent;
import static java.util.Collections.singletonList;

import timber.log.Timber;

public class CreateNewVaultWorkflow extends Workflow<CreateNewVaultWorkflow.State> {

	private final CreateVaultUseCase createVaultUseCase;
	private final GetRootFolderUseCase getRootFolderUseCase;
	private final CloudModelMapper cloudModelMapper;
	private final AuthenticationExceptionHandler authenticationExceptionHandler;
	private final Context context;
	private final DeviceUtils deviceUtils;
	private final SharedPreferencesHandler preferencesHandler;
	private final GetCachedUserProfileUseCase getCachedUserProfile;

	@Inject
	public CreateNewVaultWorkflow( //
			Context context, //
			CreateVaultUseCase createVaultUseCase, //
			GetRootFolderUseCase getRootFolderUseCase, //
			CloudModelMapper cloudModelMapper, //
			AuthenticationExceptionHandler authenticationExceptionHandler, 
			DeviceUtils deviceUtils, 
			SharedPreferencesHandler sharedPreferencesHandler,
			GetCachedUserProfileUseCase getCachedUserProfile
			) {
		super(new State());
		this.context = context;
		this.createVaultUseCase = createVaultUseCase;
		this.getRootFolderUseCase = getRootFolderUseCase;
		this.cloudModelMapper = cloudModelMapper;
		this.authenticationExceptionHandler = authenticationExceptionHandler;
		this.deviceUtils = deviceUtils;
		this.preferencesHandler = sharedPreferencesHandler;
		this.getCachedUserProfile = getCachedUserProfile;
	}

	@Override
	void doStart() {
		String subtitle = presenter().context().getString(R.string.screen_choose_cloud_service_subtitle_create_new_vault);
		chain(chooseCloudServiceIntent().withSubtitle(subtitle), SerializableResultCallbacks.onCloudServiceChosen());
	}

	@Callback
	void onCloudServiceChosen(SerializableResult<CloudModel> result) {
		onCloudServiceChosen(result.getResult());
	}

	private void onCloudServiceChosen(CloudModel cloud) {
		presenter().getView().showProgress(ProgressModel.GENERIC);
		getRootFolderUseCase //
				.withCloud(cloud.toCloud()) //
				.run(((ChooseCloudServicePresenter) presenter()).new ProgressCompletingResultHandler<CloudFolder>() {
					@Override
					public void onSuccess(CloudFolder cloudFolder) {
						state().cloudRoot = cloudFolder;
						chain(createVaultIntent(), SerializableResultCallbacks.nameEntered());
					}

					@Override
					public void onError(Throwable e) {
						if (!authenticationExceptionHandler.handleAuthenticationException( //
								presenter(), //
								e, //
								ActivityResultCallbacks.onCloudServiceAuthenticated())) {
							super.onError(e);
						}
					}
				});
	}

	@Callback
	void onCloudServiceAuthenticated(ActivityResult result) {
		onCloudServiceChosen(result.getSingleResult(CloudModel.class));
	}

	@Callback
	void nameEntered(SerializableResult<VaultCreationData> result) {
		VaultCreationData data = result.getResult();
		state().name = data.getName();
		state().description = data.getDescription();
		chain(browseFilesIntent() //
						.withTitle(context.getString(cloudModelMapper.toModel(state().cloudRoot.getCloud()).name())) //
						.withFolder(new CloudFolderModel(state().cloudRoot)) //
						.withChooseCloudNodeSettings( //
								chooseCloudNodeSettings() //
										.withExtraTitle(presenter().context().getString(R.string.screen_file_browser_subtitle_create_new_vault)) //
										.withExtraText(presenter().context().getString(R.string.screen_file_browser_create_new_vault_extra_text, state().name)) //
										.withButtonText(presenter().context().getString(R.string.screen_file_browser_create_new_vault_button_text)) //
										.selectingFoldersNotContaining(singletonList(state().name)) //
										.build()), //
				SerializableResultCallbacks.locationChosen());
	}

	@Callback
	void locationChosen(SerializableResult<CloudFolderModel> result) {
		state().location = result.getResult().toCloudNode();
		chain(setPasswordIntent(), SerializableResultCallbacks.passwordEntered());
	}

	@Callback
	void passwordEntered(SerializableResult<String> result) {
		state().password = result.getResult();
		finish();
	}

	@Override
	void completed() {
		// get user profileModel from cache
		presenter().getView().showProgress(ProgressModel.GENERIC);
		getCachedUserProfile.run(((VaultListPresenter) presenter()).new ProgressCompletingResultHandler<UserProfile>() {
			@Override
			public void onSuccess(UserProfile userProfile) {
				presenter().getView().showProgress(ProgressModel.COMPLETED);
				createVault(userProfile.getCognitoID());
			}

			@Override
			public void onError(Throwable e) {
				presenter().getView().showProgress(ProgressModel.COMPLETED);
				Timber.tag("CreateNewVaultWorkflow").d("No cached user profile found, using default userId");

			}
		});
	}

	private void createVault(String userId) {
		presenter().getView().showProgress(ProgressModel.GENERIC);
		createVaultUseCase //
				.withVaultName(state().name) //
				.andVaultDescription(state().description) //
				.andUserId(userId) //
				.andDeviceArgs(deviceUtils.getDeviceArgs(context))
				.andPassword(state().password) //
				.andFolder(state().location) //
				.run(((VaultListPresenter) presenter()).new ProgressCompletingResultHandler<Vault>() {
					@Override
					public void onSuccess(Vault vault) {
						presenter().getView().showProgress(ProgressModel.COMPLETED);
						((VaultListPresenter) presenter()).onAddOrCreateVaultCompleted(vault);
					}

					@Override
					public void onError(@NotNull Throwable e) {
						presenter().getView().showProgress(ProgressModel.COMPLETED);
						if (e instanceof CreateVaultLimitExeception) {
							Objects.requireNonNull(presenter().getView()).showDialogMessage(context.getString(R.string.limit_reached));
						} else {
							Objects.requireNonNull(presenter().getView()).showError(context.getString(R.string.failed_to_create_vault));
						}
					}
				});
	}

	static class State implements Serializable {

		CloudFolder cloudRoot;

		String name;
		String description;
		String password;
		CloudFolder location;

	}

}
