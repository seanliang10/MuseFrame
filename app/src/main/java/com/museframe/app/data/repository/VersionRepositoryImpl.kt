package com.museframe.app.data.repository

import com.museframe.app.data.api.MuseFrameApiService
import com.museframe.app.data.api.VersionDownloadResponse
import com.museframe.app.data.api.VersionInfo
import com.museframe.app.data.api.VersionReportRequest
import com.museframe.app.data.local.PreferencesManager
import com.museframe.app.domain.repository.VersionRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VersionRepositoryImpl @Inject constructor(
    private val apiService: MuseFrameApiService,
    private val preferencesManager: PreferencesManager
) : VersionRepository {

    override suspend fun getLatestVersion(): Result<VersionInfo?> {
        return try {
            val authToken = preferencesManager.authToken.first()
                ?: return Result.failure(Exception("No auth token"))

            val response = apiService.getLatestVersion(
                token = "Bearer $authToken"
            )

            when {
                response.isSuccessful && response.body()?.success == true -> {
                    Result.success(response.body()?.version)
                }
                response.isSuccessful -> {
                    val message = response.body()?.message ?: "No updates available"
                    Result.failure(Exception(message))
                }
                else -> {
                    Result.failure(Exception("Failed to check for updates: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting latest version")
            Result.failure(e)
        }
    }

    override suspend fun downloadVersion(displayId: String): Result<VersionDownloadResponse?> {
        return try {
            val authToken = preferencesManager.authToken.first()
                ?: return Result.failure(Exception("No auth token"))

            val response = apiService.downloadVersion(
                token = "Bearer $authToken",
                displayId = displayId
            )

            when {
                response.isSuccessful && response.body()?.success == true -> {
                    Result.success(response.body())
                }
                response.isSuccessful -> {
                    val message = response.body()?.message ?: "Failed to get download URL"
                    Result.failure(Exception(message))
                }
                else -> {
                    Result.failure(Exception("Failed to get download URL: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting download URL")
            Result.failure(e)
        }
    }

    override suspend fun reportVersion(displayId: String, version: String): Result<Unit> {
        return try {
            val authToken = preferencesManager.authToken.first()
                ?: return Result.failure(Exception("No auth token"))

            val response = apiService.reportVersion(
                token = "Bearer $authToken",
                displayId = displayId,
                request = VersionReportRequest(version = version)
            )

            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to report version: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reporting version")
            Result.failure(e)
        }
    }
}