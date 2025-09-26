package com.museframe.app.presentation.screens.exhibition

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.museframe.app.domain.model.Exhibition
import com.museframe.app.domain.model.ExhibitionItem
import com.museframe.app.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ExhibitionViewModelNew @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val exhibitionId: String = savedStateHandle.get<String>("exhibitionId") ?: ""

    private val _uiState = MutableStateFlow(ExhibitionUiStateNew())
    val uiState: StateFlow<ExhibitionUiStateNew> = _uiState.asStateFlow()

    private var exhibition: Exhibition? = null
    private var currentIndex: Int = 0

    init {
        loadExhibition()
    }

    private fun loadExhibition() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val result = playlistRepository.getCurrentExhibition()

                if (result.isSuccess) {
                    exhibition = result.getOrNull()

                    if (exhibition != null && exhibition!!.items.isNotEmpty()) {
                        currentIndex = 0
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                exhibition = exhibition,
                                currentItem = exhibition!!.items[currentIndex],
                                currentIndex = currentIndex,
                                totalItems = exhibition!!.items.size,
                                error = null
                            )
                        }
                    } else if (exhibition != null && exhibition!!.items.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No artworks in current exhibition"
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No current exhibition found"
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to load exhibition"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading exhibition")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    fun nextItem() {
        val items = exhibition?.items
        if (items.isNullOrEmpty()) return

        // Check if exhibition has ended
        if (isExhibitionEnded()) {
            Timber.d("Exhibition has ended, exiting exhibition")
            _uiState.update {
                it.copy(
                    exhibitionEnded = true,
                    error = "Exhibition has ended"
                )
            }
            return
        }

        val nextIndex = currentIndex + 1

        // Check if we've reached the end of the list
        if (nextIndex >= items.size) {
            // Loop back to the beginning
            currentIndex = 0
            Timber.d("Looping exhibition back to start")
        } else {
            currentIndex = nextIndex
        }

        _uiState.update {
            it.copy(
                currentItem = items[currentIndex],
                currentIndex = currentIndex
            )
        }
    }

    private fun isExhibitionEnded(): Boolean {
        val endsAt = exhibition?.endsAt ?: return false

        return try {
            // Parse the end time (assuming ISO format: "2025-01-26T23:59:59.000000Z")
            val formatter = DateTimeFormatter.ISO_DATE_TIME
            val endDateTime = LocalDateTime.parse(endsAt.replace("Z", ""), formatter)
            val currentDateTime = LocalDateTime.now()

            currentDateTime.isAfter(endDateTime)
        } catch (e: Exception) {
            Timber.e(e, "Error parsing exhibition end time: $endsAt")
            false
        }
    }

    fun previousItem() {
        val items = exhibition?.items
        if (items.isNullOrEmpty()) return

        currentIndex = if (currentIndex > 0) currentIndex - 1 else items.size - 1
        _uiState.update {
            it.copy(
                currentItem = items[currentIndex],
                currentIndex = currentIndex
            )
        }
    }

    fun checkExhibitionEndTime() {
        if (isExhibitionEnded()) {
            Timber.d("Exhibition has ended during periodic check")
            _uiState.update {
                it.copy(
                    exhibitionEnded = true,
                    error = "Exhibition has ended"
                )
            }
        }
    }
}

data class ExhibitionUiStateNew(
    val isLoading: Boolean = false,
    val exhibition: Exhibition? = null,
    val currentItem: ExhibitionItem? = null,
    val currentIndex: Int = 0,
    val totalItems: Int = 0,
    val error: String? = null,
    val exhibitionEnded: Boolean = false
)