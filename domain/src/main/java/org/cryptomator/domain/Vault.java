package org.cryptomator.domain;

import org.cryptomator.util.crypto.CryptoMode;

import java.io.Serializable;

import javax.annotation.Nullable;

public class Vault implements Serializable {

	private static final Long NOT_SET = Long.MIN_VALUE;
	private final Long id;
	private final String name;
	private final String path;
	private final Cloud cloud;
	private final CloudType cloudType;
	private final boolean unlocked;
	private final String password;
	private final CryptoMode passwordCryptoMode;
	private final int format;
	private final int shorteningThreshold;
	private final int position;
	private final Long size;
	private final String policyId;
	private final Integer driveQuota;
	private final String policyEtag;

	private final String deviceID;
	private final String createdBy;

	private final String vaultDescription;
	private final String etag;

	private final String status;
	private final String fullLocalPath;

	private Vault(Builder builder) {
		this.id = builder.id;
		this.name = builder.name;
		this.path = builder.path;
		this.cloud = builder.cloud;
		this.unlocked = builder.unlocked;
		this.cloudType = builder.cloudType;
		this.password = builder.password;
		this.passwordCryptoMode = builder.passwordCryptoMode;
		this.format = builder.format;
		this.shorteningThreshold = builder.shorteningThreshold;
		this.position = builder.position;
		this.size = builder.size;
		this.policyId = builder.policyId;
		this.driveQuota = builder.driveQuota;
		this.policyEtag = builder.policyEtag;
		this.deviceID = builder.deviceID;
		this.createdBy = builder.createdBy;
		this.vaultDescription = builder.description;
		this.etag = builder.etag;
		this.status = builder.status;
		this.fullLocalPath = builder.fullLocalPath;
	}

	public static Builder aVault() {
		return new Builder();
	}

	public static Builder aCopyOf(Vault vault) {
		return new Builder() //
				.withId(vault.getId()) //
				.withCloud(vault.getCloud()) //
				.withCloudType(vault.getCloudType()) //
				.withName(vault.getName()) //
				.withPath(vault.getPath()) //
				.withUnlocked(vault.isUnlocked()) //
				.withSavedPassword(vault.getPassword(), vault.getPasswordCryptoMode()) //
				.withFormat(vault.getFormat()) //
				.withShorteningThreshold(vault.getShorteningThreshold()) //
				.withPosition(vault.getPosition())//
				.withDeviceID(vault.getDeviceID())//
				.withCreatedBy(vault.getCreatedBy())
				.withVaultDescription(vault.getVaultDescription())
				.withEtag(vault.getEtag())
				.withStatus(vault.getStatus())
				.withFullLocalPath(vault.getFullLocalPath())
				.withSize(vault.getSize())
				.withPolicyId(vault.getPolicyId())
				.withDriveQuota(vault.getDriveQuota())
				.withPolicyEtag(vault.getPolicyEtag());
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public Cloud getCloud() {
		return cloud;
	}

	public CloudType getCloudType() {
		return cloudType;
	}

	public boolean isUnlocked() {
		return unlocked;
	}

	public String getPassword() {
		return password;
	}

	public CryptoMode getPasswordCryptoMode() {
		return passwordCryptoMode;
	}

	public int getFormat() {
		return format;
	}

	public int getShorteningThreshold() {
		return shorteningThreshold;
	}

	public int getPosition() {
		return position;
	}

	public String getDeviceID() {
		return deviceID;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public String getVaultDescription() {
		return vaultDescription;
	}

	public String getEtag() {
		if(etag == null || etag.isEmpty()) {
			return "1";
		}
		return etag;
	}

	public String getStatus() {
		return status;
	}

	public String getFullLocalPath() {
		return fullLocalPath;
	}

	public boolean isReadOnly() {
		return false; //TODO Implement read-only check
	}

	public Long getSize() {
		return size;
	}

	public String getPolicyId() {
		return policyId;
	}

	public Integer getDriveQuota() {
		return driveQuota;
	}

	public String getPolicyEtag() {
		if(policyEtag == null || policyEtag.isEmpty()) {
			return "1";
		}
		return policyEtag;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		return internalEquals((Vault) obj);
	}

	private boolean internalEquals(Vault obj) {
		return id != null && id.equals(obj.id);
	}

	@Override
	public int hashCode() {
		return id == null ? 0 : id.hashCode();
	}

	public Vault withNewEtag(String etag) {
		return new Vault.Builder() //
				.withId(id) //
				.withCloud(cloud) //
				.withCloudType(cloudType) //
				.withName(name) //
				.withPath(path) //
				.withUnlocked(unlocked) //
				.withSavedPassword(password, passwordCryptoMode) //
				.withFormat(format) //
				.withShorteningThreshold(shorteningThreshold) //
				.withPosition(position)//
				.withDeviceID(deviceID)//
				.withCreatedBy(createdBy)
				.withVaultDescription(vaultDescription)
				.withSize(size)
				.withPolicyId(policyId)
				.withDriveQuota(driveQuota)
				.withPolicyEtag(policyEtag)
				.withEtag(etag).build();
	}

	public Vault withArgs(String etag,boolean unlocked) {
		return new Vault.Builder() //
				.withId(id) //
				.withCloud(cloud) //
				.withCloudType(cloudType) //
				.withName(name) //
				.withPath(path) //
				.withUnlocked(unlocked) //
				.withSavedPassword(password, passwordCryptoMode) //
				.withFormat(format) //
				.withShorteningThreshold(shorteningThreshold) //
				.withPosition(position)//
				.withDeviceID(deviceID)//
				.withCreatedBy(createdBy)
				.withVaultDescription(vaultDescription)
				.withStatus(status)
				.withSize(size)
				.withPolicyId(policyId)
				.withDriveQuota(driveQuota)
				.withPolicyEtag(policyEtag)
				.withEtag(etag).build();
	}

	public Vault withCloud(Cloud cloud) {
		return new Vault.Builder() //
				.withId(id) //
				.withCloud(cloud) //
				.withCloudType(cloudType) //
				.withName(name) //
				.withPath(path) //
				.withUnlocked(unlocked) //
				.withSavedPassword(password, passwordCryptoMode) //
				.withFormat(format) //
				.withShorteningThreshold(shorteningThreshold) //
				.withPosition(position)//
				.withDeviceID(deviceID)//
				.withCreatedBy(createdBy)
				.withVaultDescription(vaultDescription)
				.withStatus(status)
				.withSize(size)
				.withPolicyId(policyId)
				.withDriveQuota(driveQuota)
				.withPolicyEtag(policyEtag)
				.withEtag(etag).build();
	}

	public Vault withCreatedBy(String userCognitoId) {
		return new Vault.Builder() //
				.withId(id) //
				.withCloud(cloud) //
				.withCloudType(cloudType) //
				.withName(name) //
				.withPath(path) //
				.withUnlocked(unlocked) //
				.withSavedPassword(password, passwordCryptoMode) //
				.withFormat(format) //
				.withShorteningThreshold(shorteningThreshold) //
				.withPosition(position)//
				.withDeviceID(deviceID)//
				.withCreatedBy(createdBy)
				.withVaultDescription(vaultDescription)
				.withStatus(status)
				.withCreatedBy(userCognitoId)
				.withSize(size)
				.withPolicyId(policyId)
				.withDriveQuota(driveQuota)
				.withPolicyEtag(policyEtag)
				.withEtag(etag).build();
	}

	public Vault withNewSize(long size) {
		return new Vault.Builder() //
				.withId(id) //
				.withCloud(cloud) //
				.withCloudType(cloudType) //
				.withName(name) //
				.withPath(path) //
				.withUnlocked(unlocked) //
				.withSavedPassword(password, passwordCryptoMode) //
				.withFormat(format) //
				.withShorteningThreshold(shorteningThreshold) //
				.withPosition(position)//
				.withDeviceID(deviceID)//
				.withCreatedBy(createdBy)
				.withVaultDescription(vaultDescription)
				.withStatus(status)
				.withSize(size)
				.withPolicyId(policyId)
				.withDriveQuota(driveQuota)
				.withPolicyEtag(policyEtag)
				.withEtag(etag).build();
	}

	public static class Builder {

		private Long id = NOT_SET;
		private String name;
		private String path;
		private Cloud cloud;
		private CloudType cloudType;
		private boolean unlocked;
		private String password;
		private CryptoMode passwordCryptoMode;
		private int format = -1;
		private int shorteningThreshold = -1;
		private int position = -1;
		private Long size = 0L;
		private String policyId;
		private Integer driveQuota;
		private String policyEtag;
		private String deviceID;
		private String createdBy;

		private String description;
		private String etag;

		private String status;
		private String fullLocalPath;

		private Builder() {
		}

		public Builder thatIsNew() {
			this.id = null;
			return this;
		}

		public Builder withId(Long id) {
			if (id < 1) {
				throw new IllegalArgumentException("id must not be smaller one");
			}
			this.id = id;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withPath(String path) {
			this.path = path;
			return this;
		}

		public Builder withUnlocked(boolean unlocked) {
			this.unlocked = unlocked;
			return this;
		}

		public Builder withCloud(Cloud cloud) {
			this.cloud = cloud;

			if (cloud != null) {
				this.cloudType = cloud.type();
			}

			return this;
		}

		public Builder withCloudType(CloudType cloudType) {
			this.cloudType = cloudType;

			if (cloud != null && cloud.type() != cloudType) {
				throw new IllegalStateException("Cloud type must match cloud");
			}

			return this;
		}

		public Builder withNamePathAndCloudFrom(CloudFolder vaultFolder) {
			this.name = vaultFolder.getName();
			this.path = vaultFolder.getPath();
			this.cloud = vaultFolder.getCloud();
			this.cloudType = cloud.type();
			return this;
		}

		public Builder withSavedPassword(String password, CryptoMode cryptoMode) {
			this.password = password;
			this.passwordCryptoMode = cryptoMode;
			return this;
		}

		public Builder withFormat(int version) {
			this.format = version;
			return this;
		}

		public Builder withShorteningThreshold(int shorteningThreshold) {
			this.shorteningThreshold = shorteningThreshold;
			return this;
		}

		public Builder withPosition(int position) {
			this.position = position;
			return this;
		}

		public Builder withDeviceID(String deviceID) {
			this.deviceID = deviceID;
			return this;
		}

		public Builder withCreatedBy(String createdBy) {
			this.createdBy = createdBy;
			return this;
		}

		public Builder withVaultDescription(@Nullable String vaultDescription) {
			if (vaultDescription == null) {
				vaultDescription = "";
			}
			this.description = vaultDescription;
			return this;
		}

		public Builder withEtag(String etag) {
			this.etag = etag;
			return this;
		}

		public Builder withStatus(String status) {
			this.status = status;
			return this;
		}

		public Builder withFullLocalPath(String fullLocalPath) {
			this.fullLocalPath = fullLocalPath;
			return this;
		}

		public Builder withSize(Long size) {
			this.size = size;
			return this;
		}

		public Builder withPolicyId(String policyId) {
			this.policyId = policyId;
			return this;
		}

		public Builder withDriveQuota(Integer driveQuota) {
			this.driveQuota = driveQuota;
			return this;
		}

		public Builder withPolicyEtag(String policyEtag) {
			this.policyEtag = policyEtag;
			return this;
		}

		public Vault build() {
			validate();
			return new Vault(this);
		}

		private void validate() {
			if (NOT_SET.equals(id)) {
				throw new IllegalStateException("id must be set");
			}
			if (name == null) {
				throw new IllegalStateException("name must be set");
			}
			if (path == null) {
				throw new IllegalStateException("path must be set");
			}
			if (cloudType == null) {
				throw new IllegalStateException("cloudtype must be set");
			}
			if (position == -1) {
				throw new IllegalStateException("position must be set");
			}
			if (password != null && passwordCryptoMode == null) {
				throw new IllegalStateException("passwordCryptoMode must be set if password is set");
			}
			if (passwordCryptoMode != null && password == null) {
				throw new IllegalStateException("password must be set if passwordCryptoMode is set");
			}
		}
	}

	@Override
	public String toString() {
		return "Vault{" + "id=" + id + ", name='" + name + '\'' + ", path='" + path + '\'' + ", cloud=" + cloud + ", cloudType=" + cloudType + ", unlocked=" + unlocked + ", password='" + password + '\'' + ", passwordCryptoMode=" + passwordCryptoMode + ", format=" + format + ", shorteningThreshold=" + shorteningThreshold + ", position=" + position + ", deviceID='" + deviceID + '\'' + ", createdBy='" + createdBy + '\'' + ", vaultDescription='" + vaultDescription + '\'' + ", etag='" + etag + '\'' + ", status='" + status + '\'' + ", fullLocalPath='" + fullLocalPath + '\'' + ", driveQuota='" + driveQuota + '\'' + '}';
	}
}
