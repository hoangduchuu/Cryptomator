package org.cryptomator.data.di;

import org.cryptomator.data.dto.mappers.DeploymentInfoMapper;
import org.cryptomator.data.dto.mappers.VaultRemoteMapper;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class MapperModule {

    @Provides
    @Singleton
    public VaultRemoteMapper provideVaultRemoteMapper() {
        return new VaultRemoteMapper();
    }

    @Provides
    @Singleton
    public DeploymentInfoMapper provideDeploymentInfoMapper(VaultRemoteMapper vaultRemoteMapper) {
        return new DeploymentInfoMapper(vaultRemoteMapper);
    }
} 