package com.museframe.app.di

import com.museframe.app.data.repository.AuthRepositoryImpl
import com.museframe.app.data.repository.DeviceRepositoryImpl
import com.museframe.app.data.repository.PlaylistRepositoryImpl
import com.museframe.app.data.repository.VersionRepositoryImpl
import com.museframe.app.domain.repository.AuthRepository
import com.museframe.app.domain.repository.DeviceRepository
import com.museframe.app.domain.repository.PlaylistRepository
import com.museframe.app.domain.repository.VersionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(
        playlistRepositoryImpl: PlaylistRepositoryImpl
    ): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(
        deviceRepositoryImpl: DeviceRepositoryImpl
    ): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindVersionRepository(
        versionRepositoryImpl: VersionRepositoryImpl
    ): VersionRepository
}