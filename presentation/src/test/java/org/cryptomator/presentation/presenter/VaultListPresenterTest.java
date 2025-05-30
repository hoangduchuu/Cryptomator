package org.cryptomator.presentation.presenter;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;

import org.cryptomator.data.util.NetworkConnectionCheck;
import org.cryptomator.domain.Cloud;
import org.cryptomator.domain.CloudType;
import org.cryptomator.domain.DeviceArgs;
import org.cryptomator.domain.OnedriveCloud;
import org.cryptomator.domain.Vault;
import org.cryptomator.domain.usecases.DoLicenseCheckUseCase;
import org.cryptomator.domain.usecases.DoUpdateCheckUseCase;
import org.cryptomator.domain.usecases.DoUpdateUseCase;
import org.cryptomator.domain.usecases.GetDecryptedCloudForVaultUseCase;
import org.cryptomator.domain.usecases.ResultHandler;
import org.cryptomator.domain.usecases.cloud.GetRootFolderUseCase;
import org.cryptomator.domain.usecases.user.CacheUserProfileUseCase;
import org.cryptomator.domain.usecases.user.ClearUserProfileCacheUseCase;
import org.cryptomator.domain.usecases.user.GetCachedUserProfileUseCase;
import org.cryptomator.domain.usecases.user.GetUserAvatarUseCase;
import org.cryptomator.domain.usecases.user.GetUserProfileUseCase;
import org.cryptomator.domain.usecases.user.LoginUseCase;
import org.cryptomator.domain.usecases.user.RefreshTokenUseCase;
import org.cryptomator.domain.usecases.vault.*;
import org.cryptomator.presentation.exception.ExceptionHandlers;
import org.cryptomator.presentation.model.VaultModel;
import org.cryptomator.presentation.model.mappers.CloudFolderModelMapper;
import org.cryptomator.presentation.model.mappers.UserProfileModelMapper;
import org.cryptomator.presentation.ui.activity.view.VaultListView;
import org.cryptomator.presentation.util.AvatarGenerator;
import org.cryptomator.presentation.util.DeviceUtils;
import org.cryptomator.presentation.util.FileUtil;
import org.cryptomator.presentation.workflow.AddExistingVaultWorkflow;
import org.cryptomator.presentation.workflow.AuthenticationExceptionHandler;
import org.cryptomator.presentation.workflow.CreateNewVaultWorkflow;
import org.cryptomator.util.SharedPreferencesHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.util.Arrays.asList;

public class VaultListPresenterTest {

	private static final String A_NEW_VAULT_NAME = "Haribo";

	private static final Cloud A_CLOUD = OnedriveCloud //
			.aOnedriveCloud() //
			.build();

	private static final Vault AN_UNLOCKED_VAULT = Vault.aVault() //
			.withId(1L) //
			.withPosition(1) //
			.withName("Top Secret") //
			.withPath("/top secret") //
			.withCloudType(CloudType.DROPBOX) //
			.withUnlocked(true).build();

	private static final Vault ANOTHER_VAULT_WITH_CLOUD = Vault.aVault() //
			.withId(2L) //
			.withPosition(2) //
			.withName("Trip to the moon") //
			.withPath("/trip to the moon") //
			.withCloudType(CloudType.ONEDRIVE) //
			.withCloud(A_CLOUD).build();

	private static final Vault A_VAULT_WITH_NEW_NAME = Vault.aVault() //
			.withId(3L) //
			.withPosition(3) //
			.withName(A_NEW_VAULT_NAME) //
			.withPath("/trip to the moon") //
			.withCloudType(CloudType.GOOGLE_DRIVE) //
			.withUnlocked(false) //
			.build();

	private static final VaultModel AN_UNLOCKED_VAULT_MODEL = new VaultModel(AN_UNLOCKED_VAULT);

	private static final VaultModel ANOTHER_VAULT_MODEL_WITH_CLOUD = new VaultModel(ANOTHER_VAULT_WITH_CLOUD);

	private static final VaultModel A_VAULT_MODEL_WITH_NEW_NAME = new VaultModel(A_VAULT_WITH_NEW_NAME);

	private static final Throwable AN_EXCEPTION = new Exception();
	private final CloudFolderModelMapper cloudNodeModelMapper = Mockito.mock(CloudFolderModelMapper.class);
	private final UserProfileModelMapper userProfileModelMapper = Mockito.mock(UserProfileModelMapper.class);
	public Activity activity = Mockito.mock(Activity.class);
	private Context context = Mockito.mock(Context.class);
	private ContentResolver contentResolver = Mockito.mock(ContentResolver.class);
	private Resources resources = Mockito.mock(Resources.class);
	private VaultListView vaultListView = Mockito.mock(VaultListView.class);
	private PollVaultUseCase pollVaultUseCase = Mockito.mock(PollVaultUseCase.class);
	private LogDeviceEventUseCase logdeviceEventUsecase = Mockito.mock(LogDeviceEventUseCase.class);
	private GetDeploymentInfoUseCase getDeploymentInfoUseCase = Mockito.mock(GetDeploymentInfoUseCase.class);
	private ImportDeploymentVaultUseCase importDeploymentVaultUseCase = Mockito.mock(ImportDeploymentVaultUseCase.class);
	private UpdateVaultEtagUseCase updateVaultEtagUseCase = Mockito.mock(UpdateVaultEtagUseCase.class);
	private GetUserAvatarUseCase getUserAvatarUseCase = Mockito.mock(GetUserAvatarUseCase.class);
	private LoginUseCase loginUseCase = Mockito.mock(LoginUseCase.class);
	private RefreshTokenUseCase refreshTokenUseCase = Mockito.mock(RefreshTokenUseCase.class);
	private GetUserProfileUseCase getUserProfileUseCase = Mockito.mock(GetUserProfileUseCase.class);
	private CacheUserProfileUseCase cacheUserProfileUseCase = Mockito.mock(CacheUserProfileUseCase.class);
	private GetCachedUserProfileUseCase getCachedUserProfileUseCase = Mockito.mock(GetCachedUserProfileUseCase.class);
	private ClearUserProfileCacheUseCase clearUserProfileCacheUseCase = Mockito.mock(ClearUserProfileCacheUseCase.class);
	private GetVaultListUseCase getVaultListUseCase = Mockito.mock(GetVaultListUseCase.class);
	private DeleteVaultUseCase deleteVaultUseCase = Mockito.mock(DeleteVaultUseCase.class);
	private DeleteVaultsUseCase deleteVaultsUseCase = Mockito.mock(DeleteVaultsUseCase.class);
	private DeleteVaultUseCase.Launcher deleteVaultUseCaseLauncher = Mockito.mock(DeleteVaultUseCase.Launcher.class);
	private RenameVaultUseCase renameVaultUseCase = Mockito.mock(RenameVaultUseCase.class);
	private RenameVaultUseCase.Launcher renameVaultUseCaseLauncher = Mockito.mock(RenameVaultUseCase.Launcher.class);
	private LockVaultUseCase lockVaultUseCase = Mockito.mock(LockVaultUseCase.class);
	private LockVaultUseCase.Launcher lockVaultUseCaseLauncher = Mockito.mock(LockVaultUseCase.Launcher.class);
	private GetDecryptedCloudForVaultUseCase getDecryptedCloudForVaultUseCase = Mockito.mock(GetDecryptedCloudForVaultUseCase.class);
	private UnlockToken unlockToken = Mockito.mock(UnlockToken.class);
	private GetRootFolderUseCase getRootFolderUseCase = Mockito.mock(GetRootFolderUseCase.class);
	private AddExistingVaultWorkflow addExistingVaultWorkflow = Mockito.mock(AddExistingVaultWorkflow.class);
	private CreateNewVaultWorkflow createNewVaultWorkflow = Mockito.mock(CreateNewVaultWorkflow.class);
	private SaveVaultUseCase saveVaultUseCase = Mockito.mock(SaveVaultUseCase.class);
	private MoveVaultPositionUseCase moveVaultPositionUseCase = Mockito.mock(MoveVaultPositionUseCase.class);
	private DoLicenseCheckUseCase doLicenceCheckUsecase = Mockito.mock(DoLicenseCheckUseCase.class);
	private DoUpdateCheckUseCase updateCheckUseCase = Mockito.mock(DoUpdateCheckUseCase.class);
	private DoUpdateUseCase updateUseCase = Mockito.mock(DoUpdateUseCase.class);
	private UpdateVaultParameterIfChangedRemotelyUseCase updateVaultParameterIfChangedRemotelyUseCase = Mockito.mock(UpdateVaultParameterIfChangedRemotelyUseCase.class);
	private ListCBCEncryptedPasswordVaultsUseCase listCBCEncryptedPasswordVaultsUseCase = Mockito.mock(ListCBCEncryptedPasswordVaultsUseCase.class);
	private RemoveStoredVaultPasswordsUseCase removeStoredVaultPasswordsUseCase = Mockito.mock(RemoveStoredVaultPasswordsUseCase.class);
	private SaveVaultsUseCase saveVaultsUseCase = Mockito.mock(SaveVaultsUseCase.class);
	private NetworkConnectionCheck networkConnectionCheck = Mockito.mock(NetworkConnectionCheck.class);
	private FileUtil fileUtil = Mockito.mock(FileUtil.class);
	private AuthenticationExceptionHandler authenticationExceptionHandler = Mockito.mock(AuthenticationExceptionHandler.class);
	private SharedPreferencesHandler sharedPreferencesHandler = Mockito.mock(SharedPreferencesHandler.class);
	private ExceptionHandlers exceptionMappings = Mockito.mock(ExceptionHandlers.class);
	private VaultListPresenter inTest;
	private DeviceUtils deviceUtils = Mockito.mock(DeviceUtils.class);
	private DeviceArgs deviceArgs = Mockito.mock(DeviceArgs.class);
	private AvatarGenerator avatarGenerator = Mockito.mock(AvatarGenerator.class);

	@BeforeEach
	public void setup() {
		MockitoAnnotations.initMocks(this);
		inTest = new VaultListPresenter(
				logdeviceEventUsecase,
				importDeploymentVaultUseCase, getDeploymentInfoUseCase, pollVaultUseCase,
				updateVaultEtagUseCase, getUserAvatarUseCase, loginUseCase, refreshTokenUseCase, getUserProfileUseCase,
				getCachedUserProfileUseCase, cacheUserProfileUseCase, clearUserProfileCacheUseCase, getVaultListUseCase,
				deleteVaultUseCase, deleteVaultsUseCase, renameVaultUseCase, lockVaultUseCase, getDecryptedCloudForVaultUseCase,
				getRootFolderUseCase, addExistingVaultWorkflow, createNewVaultWorkflow, saveVaultUseCase, moveVaultPositionUseCase,
				doLicenceCheckUsecase, updateCheckUseCase, updateUseCase, updateVaultParameterIfChangedRemotelyUseCase,
				listCBCEncryptedPasswordVaultsUseCase, removeStoredVaultPasswordsUseCase, saveVaultsUseCase, networkConnectionCheck,
				fileUtil, authenticationExceptionHandler, cloudNodeModelMapper, sharedPreferencesHandler, userProfileModelMapper,
				deviceUtils, avatarGenerator, exceptionMappings);

		inTest = Mockito.spy(inTest);

		doReturn(deviceArgs).when(inTest).buildDeviceArgs();

		// Override context() method to return mocked context
		doReturn(context).when(inTest).context();

		when(vaultListView.activity()).thenReturn(activity);

		inTest.setView(vaultListView);

		// Add this line to mock hasStoragePermissions method
		doReturn(true).when(inTest).hasStoragePermissions();
	}

	@Test
	public void testOnWindowsFocusChangedTrue() {
		inTest.onWindowFocusChanged(true);

		verify(vaultListView).hideVaultCreationHint();
		verify(getVaultListUseCase).run(Mockito.any());
	}

	@Test
	public void testOnWindowsFocusChangedFalse() {
		inTest.onWindowFocusChanged(false);

		verify(vaultListView, never()).hideVaultCreationHint();
		verify(getVaultListUseCase, never()).run(Mockito.any());
	}

	@Test
	public void testLoadVaultListWithEmptyVaultList() {
		ArgumentCaptor<ResultHandler<List<Vault>>> captor = ArgumentCaptor.forClass(ResultHandler.class);
// Mock the hasStoragePermissions method to return true
		doReturn(true).when(inTest).hasStoragePermissions();
		inTest.loadVaultList();
		verify(vaultListView).hideVaultCreationHint();
		verify(getVaultListUseCase).run(captor.capture());

		captor.getValue().onSuccess(Collections.emptyList());

		verify(vaultListView).showVaultCreationHint();
	}

	@Test
	public void testLoadVaultListWithVaultList() {
		ArgumentCaptor<ResultHandler<List<Vault>>> captor = ArgumentCaptor.forClass(ResultHandler.class);
// Mock the hasStoragePermissions method to return true
		doReturn(true).when(inTest).hasStoragePermissions();

		inTest.loadVaultList();
		verify(vaultListView).hideVaultCreationHint();
		verify(getVaultListUseCase).run(captor.capture());

		captor.getValue().onSuccess(asList(AN_UNLOCKED_VAULT, ANOTHER_VAULT_WITH_CLOUD));

		verify(vaultListView, times(2)).hideVaultCreationHint();
		verify(vaultListView).renderVaultList(asList(AN_UNLOCKED_VAULT_MODEL, ANOTHER_VAULT_MODEL_WITH_CLOUD));
	}

	@Test
	public void testDeleteVault() {
		ArgumentCaptor<ResultHandler<Long>> captor = ArgumentCaptor.forClass(ResultHandler.class);

		// Mock the computer ID
		when(sharedPreferencesHandler.getComputerId()).thenReturn("test-computer-id");

		// First, set up the mock chain
		DeleteVaultUseCase.Launcher launcher = deleteVaultUseCaseLauncher;
		when(deleteVaultUseCase.withVault(any(Vault.class)))
				.thenReturn(launcher);
		when(launcher.andComputerId("test-computer-id"))
				.thenReturn(launcher);
		doAnswer(invocation -> {
			ResultHandler<Long> handler = invocation.getArgument(0);
			handler.onSuccess(AN_UNLOCKED_VAULT_MODEL.getVaultId());
			return null;
		}).when(launcher).run(any(ResultHandler.class));

		// Now call the method under test
		inTest.deleteVault(AN_UNLOCKED_VAULT_MODEL);

		// Verify the interactions
		verify(launcher).run(captor.capture());
		verify(vaultListView).deleteVaultFromAdapter(AN_UNLOCKED_VAULT_MODEL.getVaultId());
	}

	@Test
	public void testRenameVaultWithSuccess() {
		ArgumentCaptor<ResultHandler<Vault>> captor = ArgumentCaptor.forClass(ResultHandler.class);

		// Mock the device args
		when(deviceUtils.getDeviceArgs(any(Context.class))).thenReturn(deviceArgs);

		// Setup the mock chain properly
		RenameVaultUseCase.Launcher launcher = renameVaultUseCaseLauncher;
		when(renameVaultUseCase.withVault(AN_UNLOCKED_VAULT_MODEL.toVault()))
				.thenReturn(launcher);
		when(launcher.andNewVaultName(A_NEW_VAULT_NAME))
				.thenReturn(launcher);
		when(launcher.andDeviceArgs(deviceArgs))
				.thenReturn(launcher);
		doAnswer(invocation -> {
			ResultHandler<Vault> handler = invocation.getArgument(0);
			handler.onSuccess(A_VAULT_WITH_NEW_NAME);
			return null;
		}).when(launcher).run(any(ResultHandler.class));

		inTest.renameVault(AN_UNLOCKED_VAULT_MODEL, A_NEW_VAULT_NAME);

		verify(launcher).run(captor.capture());
		verify(vaultListView).renameVault(A_VAULT_MODEL_WITH_NEW_NAME);
		verify(vaultListView).closeDialog();
	}

	@Test
	public void testRenameVaultWithError() {
		ArgumentCaptor<ResultHandler<Vault>> captor = ArgumentCaptor.forClass(ResultHandler.class);

		// Mock the device args
		when(deviceUtils.getDeviceArgs(any(Context.class))).thenReturn(deviceArgs);

		// Setup the mock chain properly
		RenameVaultUseCase.Launcher launcher = renameVaultUseCaseLauncher;
		when(renameVaultUseCase.withVault(AN_UNLOCKED_VAULT_MODEL.toVault()))
				.thenReturn(launcher);
		when(launcher.andNewVaultName(A_NEW_VAULT_NAME))
				.thenReturn(launcher);
		when(launcher.andDeviceArgs(deviceArgs))
				.thenReturn(launcher);
		doAnswer(invocation -> {
			ResultHandler<Vault> handler = invocation.getArgument(0);
			handler.onError(AN_EXCEPTION);
			return null;
		}).when(launcher).run(any(ResultHandler.class));

		inTest.renameVault(AN_UNLOCKED_VAULT_MODEL, A_NEW_VAULT_NAME);

		verify(launcher).run(captor.capture());
		verify(authenticationExceptionHandler)
				.handleAuthenticationException(Mockito.any(),
						Mockito.any(),
						Mockito.any());
	}

}
