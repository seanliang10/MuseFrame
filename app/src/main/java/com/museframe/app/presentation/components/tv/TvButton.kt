package com.museframe.app.presentation.components.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    colors: TvButtonColors = TvButtonDefaults.colors(),
    requestInitialFocus: Boolean = false,
    focusRequester: FocusRequester? = null
) {
    val defaultFocusRequester = remember { FocusRequester() }
    val actualFocusRequester = focusRequester ?: defaultFocusRequester
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            // Small delay to ensure the component is properly laid out
            kotlinx.coroutines.delay(100)
            try {
                actualFocusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore if focus can't be requested yet
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.disabledBackgroundColor
            isFocused -> colors.focusedBackgroundColor
            else -> colors.backgroundColor
        },
        label = "backgroundColor"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.disabledContentColor
            isFocused -> colors.focusedContentColor
            else -> colors.contentColor
        },
        label = "contentColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Gray.copy(alpha = 0.3f)
            isFocused -> colors.focusedBorderColor
            else -> colors.borderColor
        },
        label = "borderColor"
    )

    val borderWidth by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isFocused -> 3f
            else -> 2f
        },
        label = "borderWidth"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .focusRequester(actualFocusRequester)
            .focusProperties {
                // This helps prevent focus from getting lost
            }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)) {
                    if (enabled) {
                        onClick()
                        true
                    } else false
                } else false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = BorderStroke(
            width = borderWidth.dp,
            color = borderColor
        ),
        shadowElevation = if (isFocused) 8.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 28.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = if (isFocused) 17.sp else 16.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null,
    enabled: Boolean = true,
    colors: TvButtonColors = TvButtonDefaults.iconColors(),
    requestInitialFocus: Boolean = false
) {
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) {
            // Small delay to ensure the component is properly laid out
            kotlinx.coroutines.delay(100)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore if focus can't be requested yet
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.15f else 1f,
        label = "scale"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Gray.copy(alpha = 0.1f)
            isFocused -> colors.focusedBackgroundColor.copy(alpha = 0.3f)
            else -> Color.White.copy(alpha = 0.05f)
        },
        label = "backgroundColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Gray.copy(alpha = 0.3f)
            isFocused -> Color.White.copy(alpha = 0.9f)
            else -> Color.White.copy(alpha = 0.4f)
        },
        label = "borderColor"
    )

    val borderWidth by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isFocused -> 3f
            else -> 2f
        },
        label = "borderWidth"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.disabledContentColor
            isFocused -> colors.focusedContentColor
            else -> colors.contentColor
        },
        label = "contentColor"
    )

    Box(
        modifier = modifier
            .size(52.dp)
            .scale(scale)
            .focusRequester(focusRequester)
            .background(backgroundColor, RoundedCornerShape(26.dp))
            .border(
                width = borderWidth.dp,
                color = borderColor,
                shape = RoundedCornerShape(26.dp)
            )
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter)) {
                    if (enabled) {
                        onClick()
                        true
                    } else false
                } else false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(26.dp)
        )
    }
}

data class TvButtonColors(
    val backgroundColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val focusedBackgroundColor: Color,
    val focusedContentColor: Color,
    val focusedBorderColor: Color,
    val disabledBackgroundColor: Color,
    val disabledContentColor: Color
)

object TvButtonDefaults {
    @Composable
    fun colors(
        backgroundColor: Color = Color.White.copy(alpha = 0.08f),
        contentColor: Color = Color.White.copy(alpha = 0.9f),
        borderColor: Color = Color.White.copy(alpha = 0.5f),
        focusedBackgroundColor: Color = Color.White.copy(alpha = 0.15f),
        focusedContentColor: Color = Color.White,
        focusedBorderColor: Color = Color.White.copy(alpha = 0.95f),
        disabledBackgroundColor: Color = Color.Gray.copy(alpha = 0.1f),
        disabledContentColor: Color = Color.Gray.copy(alpha = 0.5f)
    ) = TvButtonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        borderColor = borderColor,
        focusedBackgroundColor = focusedBackgroundColor,
        focusedContentColor = focusedContentColor,
        focusedBorderColor = focusedBorderColor,
        disabledBackgroundColor = disabledBackgroundColor,
        disabledContentColor = disabledContentColor
    )

    @Composable
    fun primaryColors() = colors(
        backgroundColor = Color.White.copy(alpha = 0.12f),
        borderColor = Color.White.copy(alpha = 0.6f),
        focusedBackgroundColor = Color.White.copy(alpha = 0.95f),
        focusedContentColor = Color.Black,
        focusedBorderColor = Color.White
    )

    @Composable
    fun iconColors() = colors(
        backgroundColor = Color.Transparent,
        borderColor = Color.Transparent,
        focusedBorderColor = Color.Transparent
    )
}