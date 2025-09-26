package com.museframe.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Lifecycle-aware effect that automatically cancels when the lifecycle is destroyed
 */
@Composable
fun LifecycleEffect(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onStart: () -> Unit = {},
    onResume: () -> Unit = {},
    onPause: () -> Unit = {},
    onStop: () -> Unit = {},
    onDestroy: () -> Unit = {}
) {
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> onStart()
                Lifecycle.Event.ON_RESUME -> onResume()
                Lifecycle.Event.ON_PAUSE -> onPause()
                Lifecycle.Event.ON_STOP -> onStop()
                Lifecycle.Event.ON_DESTROY -> onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Extension function for Flows to avoid duplicate emissions
 */
fun <T> Flow<T>.distinctState(): Flow<T> = distinctUntilChanged()

/**
 * Debounced click handler to prevent rapid clicks
 */
class DebouncedClickHandler(
    private val delay: Long = 500L,
    private val scope: CoroutineScope,
    private val onClick: () -> Unit
) {
    private var clickJob: Job? = null

    fun handleClick() {
        clickJob?.cancel()
        clickJob = scope.launch {
            onClick()
            delay(delay)
        }
    }
}

/**
 * Memory-efficient image loading configuration
 */
fun ImageRequest.Builder.memoryOptimized(key: String? = null): ImageRequest.Builder {
    return this.apply {
        if (key != null) {
            memoryCacheKey(key)
            diskCacheKey(key)
        }
        allowHardware(true) // Use hardware bitmaps when possible
        allowRgb565(true)   // Use RGB_565 for images without alpha
    }
}

/**
 * Extension to safely handle nullable collections
 */
fun <T> List<T>?.orEmpty(): List<T> = this ?: emptyList()

/**
 * Batch operations helper for reducing recompositions
 */
class BatchProcessor<T>(
    private val batchSize: Int = 10,
    private val delayMs: Long = 100L,
    private val onBatch: suspend (List<T>) -> Unit
) {
    private val items = mutableListOf<T>()
    private var processJob: Job? = null

    fun add(item: T, scope: CoroutineScope) {
        items.add(item)

        if (items.size >= batchSize) {
            processBatch(scope)
        } else {
            scheduleProcess(scope)
        }
    }

    private fun scheduleProcess(scope: CoroutineScope) {
        processJob?.cancel()
        processJob = scope.launch {
            delay(delayMs)
            processBatch(scope)
        }
    }

    private fun processBatch(scope: CoroutineScope) {
        if (items.isEmpty()) return

        val batch = items.toList()
        items.clear()

        scope.launch {
            onBatch(batch)
        }
    }
}