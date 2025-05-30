package org.cryptomator.data.db.mappers;

import org.cryptomator.data.db.entities.VaultEntity;
import org.cryptomator.domain.Cloud;
import org.cryptomator.domain.CloudType;
import org.cryptomator.domain.LocalStorageCloud;
import org.cryptomator.domain.Vault;
import org.cryptomator.domain.exception.BackendException;
import org.cryptomator.util.crypto.CryptoMode;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Singleton;

import static org.cryptomator.domain.Vault.aVault;

@Singleton
public class VaultEntityMapper extends EntityMapper<VaultEntity, Vault> {

	private final CloudEntityMapper cloudEntityMapper;

	@Inject
	public VaultEntityMapper(CloudEntityMapper cloudEntityMapper) {
		this.cloudEntityMapper = cloudEntityMapper;
	}

	@Override
	public Vault fromEntity(VaultEntity entity) throws BackendException {
		return aVault() //
				.withId(entity.getId()) //
				.withName(entity.getFolderName()) //
				.withPath(entity.getFolderPath()) //
				.withCloud(cloudFrom(entity)) //
				.withCloudType(CloudType.valueOf(entity.getCloudType())) //
				.withSavedPassword(entity.getPassword(), cryptoModeFrom(entity)) //
				.withPosition(entity.getPosition()) //
				.withFormat(entity.getFormat()) //
				.withShorteningThreshold(entity.getShorteningThreshold()) //
				.withDeviceID(entity.getDeviceID()) //
				.withCreatedBy(entity.getCreatedBy()) //
				.withVaultDescription(entity.getDescription()) //
				.withEtag(entity.getEtag()) //
				.withStatus(entity.getStatus()) //
				.withFullLocalPath(entity.getFullLocalPath()) //
				.withSize(entity.getSize() != null ? entity.getSize() : 0L) //
				.withPolicyId(entity.getPolicyId()) //
				.withDriveQuota(entity.getDriveQuota()) //
				.withPolicyEtag(entity.getPolicyEtag()) //
				.build();
	}

	private Cloud cloudFrom(VaultEntity entity) {
		if (entity.getFolderCloud() == null) {
			return null;
		}
		return cloudEntityMapper.fromEntity(entity.getFolderCloud());
	}

	private CryptoMode cryptoModeFrom(VaultEntity entity) {
		return entity.getPasswordCryptoMode() != null ? CryptoMode.valueOf(entity.getPasswordCryptoMode()) : null;
	}

	@Override
	public VaultEntity toEntity(Vault domainObject) {
		VaultEntity entity = new VaultEntity();
		entity.setId(domainObject.getId());
		entity.setFolderPath(domainObject.getPath());
		entity.setFolderName(domainObject.getName());
		if (domainObject.getCloud() != null) {
			entity.setFolderCloud(cloudEntityMapper.toEntity(domainObject.getCloud()));
		}
		entity.setCloudType(domainObject.getCloudType().name());
		entity.setPassword(domainObject.getPassword());
		if (domainObject.getPasswordCryptoMode() != null) {
			entity.setPasswordCryptoMode(domainObject.getPasswordCryptoMode().name());
		}
		entity.setPosition(domainObject.getPosition());
		entity.setFormat(domainObject.getFormat());
		entity.setShorteningThreshold(domainObject.getShorteningThreshold());
		entity.setDeviceID(domainObject.getDeviceID());
		entity.setCreatedBy(domainObject.getCreatedBy());
		entity.setDescription(domainObject.getVaultDescription());
		entity.setEtag(domainObject.getEtag());
		entity.setStatus(domainObject.getStatus());
		entity.setSize(domainObject.getSize());
		entity.setPolicyId(domainObject.getPolicyId());
		entity.setDriveQuota(domainObject.getDriveQuota());
		entity.setPolicyEtag(domainObject.getPolicyEtag());

		//Region full local storage path

		// For LOCAL cloud type, store the full path
		if (domainObject.getCloudType() == CloudType.LOCAL) {
			// Get the full path from the LocalStorageCloud
			LocalStorageCloud localCloud = (LocalStorageCloud) domainObject.getCloud();
			String fullPath = domainObject.getPath();
			try {
				fullPath = URLDecoder.decode(localCloud.rootUri(), StandardCharsets.UTF_8.name());
			} catch (Exception e) {
				entity.setFullLocalPath(fullPath);
			}
			fullPath = fullPath.substring(fullPath.lastIndexOf(":") + 1);

			entity.setFullLocalPath(fullPath);
		} else {
			entity.setFullLocalPath(domainObject.getPath());
		}
		//Endregion full local storage path
		return entity;
	}
}
