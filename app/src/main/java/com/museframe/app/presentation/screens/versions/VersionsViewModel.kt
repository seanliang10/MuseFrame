package com.museframe.app.presentation.screens.versions

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.museframe.app.BuildConfig
import com.museframe.app.data.local.PreferencesManager
import com.museframe.app.domain.repository.VersionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class VersionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val versionRepository: VersionRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VersionsUiState())
    val uiState: StateFlow<VersionsUiState> = _uiState.asStateFlow()

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    init {
        loadCurrentVersion()
        checkForUpdate()
    }

    private fun loadCurrentVersion() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }

            _uiState.update {
                it.copy(
                    currentVersion = versionName,
                    currentBuildNumber = versionCode
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting current version")
            _uiState.update {
                it.copy(
                    currentVersion = "Unknown",
                    currentBuildNumber = "Unknown"
                )
            }
        }
    }

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.update { it.copy(updateStatus = UpdateStatus.CHECKING, error = null) }

            try {
                val result = versionRepository.getLatestVersion()

                if (result.isSuccess) {
                    val latestVersion = result.getOrNull()
                    val currentVersionName = _uiState.value.currentVersion

                    if (latestVersion != null && 
                        latestVersion.version != currentVersionName && 
                        latestVersion.version != "Unknown") {
                        _uiState.update {
                            it.copy(
                                updateStatus = UpdateStatus.UPDATE_AVAILABLE,
                                latestVersion = latestVersion,
                                error = null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                updateStatus = UpdateStatus.UP_TO_DATE,
                                latestVersion = latestVersion,
                                error = null
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            updateStatus = UpdateStatus.ERROR,
                            error = result.exceptionOrNull()?.message ?: "Failed to check for updates"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking for updates")
                _uiState.update {
                    it.copy(
                        updateStatus = UpdateStatus.ERROR,
                        error = e.message ?: "Network error occurred"
                    )
                }
            }
        }
    }

    fun downloadUpdate() {
        viewModelScope.launch {
            try {
                val deviceId = preferencesManager.deviceId.first()
                if (deviceId == null) {
                    _uiState.update {
                        it.copy(
                            error = "Device not registered",
                            updateStatus = UpdateStatus.ERROR
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(isDownloading = true, downloadProgress = 0f) }

                val result = versionRepository.downloadVersion(deviceId)

                if (result.isSuccess) {
                    val downloadInfo = result.getOrNull()
                    if (downloadInfo?.url != null) {
                        startDownload(downloadInfo.url, downloadInfo.version ?: "update")
                    } else {
                        _uiState.update {
                            it.copy(
                                isDownloading = false,
                                error = "No download URL available",
                                updateStatus = UpdateStatus.ERROR
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isDownloading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to get download URL",
                            updateStatus = UpdateStatus.ERROR
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error downloading update")
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        error = e.message ?: "Download failed",
                        updateStatus = UpdateStatus.ERROR
                    )
                }
            }
        }
    }

    private fun startDownload(url: String, version: String) {
        try {
            val fileName = "museframe_$version.apk"
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )

            // Delete old file if exists
            if (file.exists()) {
                file.delete()
            }

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("MuseFrame Update")
                .setDescription("Downloading version $version")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)

            _uiState.update {
                it.copy(
                    downloadId = downloadId,
                    downloadedFile = file
                )
            }

            // Start monitoring download progress
            monitorDownloadProgress(downloadId)
        } catch (e: Exception) {
            Timber.e(e, "Error starting download")
            _uiState.update {
                it.copy(
                    isDownloading = false,
                    error = "Failed to start download: ${e.message}",
                    updateStatus = UpdateStatus.ERROR
                )
            }
        }
    }

    private fun monitorDownloadProgress(downloadId: Long) {
        viewModelScope.launch {
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor.moveToFirst()) {
                    val bytesDownloaded = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    )
                    val bytesTotal = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    )
                    val status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    )

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloading = false
                            _uiState.update {
                                it.copy(
                                    downloadProgress = 1f,
                                    isDownloading = false
                                )
                            }
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloading = false
                            _uiState.update {
                                it.copy(
                                    isDownloading = false,
                                    error = "Download failed",
                                    updateStatus = UpdateStatus.ERROR
                                )
                            }
                        }
                        else -> {
                            if (bytesTotal > 0) {
                                val progress = bytesDownloaded.toFloat() / bytesTotal.toFloat()
                                _uiState.update {
                                    it.copy(downloadProgress = progress)
                                }
                            }
                        }
                    }
                }
                cursor.close()

                if (downloading) {
                    kotlinx.coroutines.delay(500)
                }
            }
        }
    }

    fun onDownloadComplete() {
        _uiState.update {
            it.copy(
                isDownloading = false,
                downloadId = null,
                updateStatus = UpdateStatus.UP_TO_DATE
            )
        }
        // Report the new version to the server
        reportCurrentVersion()
    }

    private fun reportCurrentVersion() {
        viewModelScope.launch {
            try {
                val deviceId = preferencesManager.deviceId.first() ?: return@launch
                val version = _uiState.value.currentVersion
                if (version != "Unknown") {
                    versionRepository.reportVersion(deviceId, version)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error reporting version")
            }
        }
    }
}

data class VersionsUiState(
    val currentVersion: String = "Loading...",
    val currentBuildNumber: String = "Loading...",
    val latestVersion: com.museframe.app.data.api.VersionInfo? = null,
    val updateStatus: UpdateStatus = UpdateStatus.IDLE,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadId: Long? = null,
    val downloadedFile: File? = null,
    val error: String? = null
)

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    ERROR
}