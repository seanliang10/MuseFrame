package com.museframe.app.presentation.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.museframe.app.utils.NetworkUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class NetworkAwareViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _shouldNavigateToNoNetwork = MutableStateFlow(false)
    val shouldNavigateToNoNetwork: StateFlow<Boolean> = _shouldNavigateToNoNetwork

    protected fun checkNetworkAndExecute(
        onNetworkAvailable: suspend () -> Unit,
        onNetworkUnavailable: () -> Unit = { navigateToNoNetwork() }
    ) {
        viewModelScope.launch {
            if (NetworkUtils.isNetworkAvailable(getApplication())) {
                Timber.d("Network is available, executing network operation")
                onNetworkAvailable()
            } else {
                Timber.d("No network available, triggering navigation to no network screen")
                onNetworkUnavailable()
            }
        }
    }

    fun navigateToNoNetwork() {
        Timber.d("Setting navigation flag to navigate to NoNetworkScreen")
        _shouldNavigateToNoNetwork.value = true
    }

    fun clearNoNetworkNavigation() {
        _shouldNavigateToNoNetwork.value = false
    }

    protected fun hasNetwork(): Boolean {
        return NetworkUtils.isNetworkAvailable(getApplication())
    }
}