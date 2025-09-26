package com.museframe.app.domain.repository

import com.museframe.app.data.api.VersionInfo
import com.museframe.app.data.api.VersionDownloadResponse

interface VersionRepository {
    suspend fun getLatestVersion(): Result<VersionInfo?>
    suspend fun downloadVersion(displayId: String): Result<VersionDownloadResponse?>
    suspend fun reportVersion(displayId: String, version: String): Result<Unit>
}