package com.museframe.app.data.repository

import com.museframe.app.data.api.MuseFrameApiService
import com.museframe.app.data.api.dto.TokenVerifyRequest
import com.museframe.app.data.local.PreferencesManager
import com.museframe.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: MuseFrameApiService,
    private val preferencesManager: PreferencesManager
) : AuthRepository {

    override fun isAuthenticated(): Flow<Boolean> {
        return preferencesManager.authToken.map { token ->
            !token.isNullOrEmpty()
        }
    }

    override suspend fun verifyToken(token: String): Result<Boolean> {
        return try {
            val response = apiService.verifyToken(
                token = "Bearer $token",
                request = TokenVerifyRequest(token = token)
            )

            if (response.isSuccessful && response.body()?.valid == true) {
                Result.success(true)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error verifying token")
            Result.failure(e)
        }
    }

    override suspend fun saveAuthToken(token: String) {
        preferencesManager.saveAuthToken(token)
    }

    override suspend fun clearAuthToken() {
        preferencesManager.clearAuthToken()
    }

    override suspend fun logout() {
        preferencesManager.clearAuthData()
    }
}