/*
 * Nocturne - by Mudassir

 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mudassir131.yt.R
import kotlin.math.roundToInt

@Composable
fun PreferenceEntry(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    content: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    isEnabled: Boolean = true,
    position: com.mudassir131.yt.ui.utils.PreferencePosition = com.mudassir131.yt.ui.utils.LocalPreferencePosition.current,
) {
    val isInsideGroup = com.mudassir131.yt.ui.utils.LocalInsidePreferenceGroup.current
    val effectiveShape = com.mudassir131.yt.ui.utils.LocalPreferenceShape.current ?: com.mudassir131.yt.ui.utils.getPreferenceShape(position)

    if (isInsideGroup) {
        Surface(
            onClick = onClick ?: {},
            enabled = isEnabled && onClick != null,
            shape = effectiveShape,
            color = Color.Transparent,
            modifier = modifier
                .fillMaxWidth()
                .alpha(if (isEnabled) 1f else 0.5f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (icon != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) { icon() }
                    }
                    Spacer(Modifier.width(14.dp))
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f),
                ) {
                    ProvideTextStyle(
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    ) {
                        title()
                    }
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    content?.invoke()
                }

                if (trailingContent != null) {
                    Spacer(Modifier.width(12.dp))
                    trailingContent()
                }
            }
        }
    } else {
        val topPadding = if (position == com.mudassir131.yt.ui.utils.PreferencePosition.FIRST || position == com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE) 4.dp else 0.5.dp
        val bottomPadding = if (position == com.mudassir131.yt.ui.utils.PreferencePosition.LAST || position == com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE) 4.dp else 0.5.dp

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = topPadding, bottom = bottomPadding)
                .alpha(if (isEnabled) 1f else 0.5f),
            shape = effectiveShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = isEnabled && onClick != null,
                        onClick = onClick ?: {},
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (icon != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) { icon() }
                    }
                    Spacer(Modifier.width(14.dp))
                }

                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f),
                ) {
                    ProvideTextStyle(
                        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    ) {
                        title()
                    }
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    content?.invoke()
                }

                if (trailingContent != null) {
                    Spacer(Modifier.width(12.dp))
                    trailingContent()
                }
            }
        }
    }
}

@Composable
fun <T> ListPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    selectedValue: T,
    values: List<T>,
    valueText: @Composable (T) -> String,
    onValueSelected: (T) -> Unit,
    isEnabled: Boolean = true,
    position: com.mudassir131.yt.ui.utils.PreferencePosition = com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE,
) {
    var showDialog by rememberSaveable {
        mutableStateOf(false)
    }
    if (showDialog) {
        ListDialog(
            onDismiss = { showDialog = false },
        ) {
            items(values) { value ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDialog = false
                            onValueSelected(value)
                        }.padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    RadioButton(
                        selected = value == selectedValue,
                        onClick = null,
                    )

                    Text(
                        text = valueText(value),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = valueText(selectedValue),
        icon = icon,
        onClick = { showDialog = true },
        isEnabled = isEnabled,
        position = position,
    )
}

@Composable
inline fun <reified T : Enum<T>> EnumListPreference(
    modifier: Modifier = Modifier,
    noinline title: @Composable () -> Unit,
    noinline icon: (@Composable () -> Unit)?,
    selectedValue: T,
    noinline valueText: @Composable (T) -> String,
    noinline onValueSelected: (T) -> Unit,
    isEnabled: Boolean = true,
    position: com.mudassir131.yt.ui.utils.PreferencePosition = com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE,
) {
    ListPreference(
        modifier = modifier,
        title = title,
        icon = icon,
        selectedValue = selectedValue,
        values = enumValues<T>().toList(),
        valueText = valueText,
        onValueSelected = onValueSelected,
        isEnabled = isEnabled,
        position = position,
    )
}

@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isEnabled: Boolean = true,
    position: com.mudassir131.yt.ui.utils.PreferencePosition = com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE,
) {
    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = isEnabled,
                thumbContent = {
                    Icon(
                        painter = painterResource(
                            id = if (checked) R.drawable.check else R.drawable.close
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            )
        },
        onClick = { onCheckedChange(!checked) },
        isEnabled = isEnabled,
        position = position,
    )
}

@Composable
fun EditTextPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    isInputValid: (String) -> Boolean = { it.isNotEmpty() },
    isEnabled: Boolean = true,
    position: com.mudassir131.yt.ui.utils.PreferencePosition = com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE,
) {
    var showDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDialog) {
        TextFieldDialog(
            initialTextFieldValue =
            TextFieldValue(
                text = value,
                selection = TextRange(value.length),
            ),
            singleLine = singleLine,
            isInputValid = isInputValid,
            onDone = onValueChange,
            onDismiss = { showDialog = false },
        )
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = value,
        icon = icon,
        onClick = { showDialog = true },
        isEnabled = isEnabled,
        position = position,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    value: Float,
    onValueChange: (Float) -> Unit,
    isEnabled: Boolean = true,
    position: com.mudassir131.yt.ui.utils.PreferencePosition = com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    var sliderValue by remember {
        mutableFloatStateOf(value)
    }

    if (showDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.history_duration),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            },
            onDismiss = { showDialog = false },
            onConfirm = {
                showDialog = false
                onValueChange.invoke(sliderValue)
            },
            onCancel = {
                sliderValue = value
                showDialog = false
            },
            onReset = {
                sliderValue = 30f // Default value or any reset value you prefer
            },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.seconds,
                            sliderValue.roundToInt(),
                            sliderValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(Modifier.height(16.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        valueRange = 15f..60f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = value.roundToInt().toString(),
        icon = icon,
        onClick = { showDialog = true },
        isEnabled = isEnabled,
        position = position,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrossfadeSliderPreference(
    modifier: Modifier = Modifier,
    value: Int,
    onValueChange: (Int) -> Unit,
    isEnabled: Boolean = true,
    position: com.mudassir131.yt.ui.utils.PreferencePosition = com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    var sliderValue by remember {
        mutableFloatStateOf(value.toFloat())
    }

    if (showDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.audio_crossfade_dialog_title),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            },
            onDismiss = { showDialog = false },
            onConfirm = {
                val rounded = sliderValue.roundToInt().coerceIn(0, 10)
                sliderValue = rounded.toFloat()
                showDialog = false
                onValueChange.invoke(rounded)
            },
            onCancel = {
                sliderValue = value.toFloat()
                showDialog = false
            },
            onReset = {
                sliderValue = 0f
            },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val rounded = sliderValue.roundToInt().coerceIn(0, 10)
                    Text(
                        text =
                        if (rounded == 0) {
                            stringResource(R.string.dark_theme_off)
                        } else {
                            pluralStringResource(
                                R.plurals.seconds,
                                rounded,
                                rounded
                            )
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.audio_crossfade_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )

                    Spacer(Modifier.height(16.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it.coerceIn(0f, 10f) },
                        valueRange = 0f..10f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    val descriptionText =
        if (value <= 0) {
            stringResource(R.string.dark_theme_off)
        } else {
            pluralStringResource(R.plurals.seconds, value, value)
        }

    PreferenceEntry(
        modifier = modifier,
        title = { Text(stringResource(R.string.audio_crossfade_title)) },
        description = descriptionText,
        icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
        onClick = { if (isEnabled) showDialog = true },
        isEnabled = isEnabled,
        position = position,
    )
}

@Composable
fun NumberPickerPreference(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    icon: (@Composable () -> Unit)? = null,
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int = 0,
    maxValue: Int = 10,
    valueText: (Int) -> String = { it.toString() },
    isEnabled: Boolean = true,
    position: com.mudassir131.yt.ui.utils.PreferencePosition = com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE,
) {
    var showDialog by remember {
        mutableStateOf(false)
    }

    var sliderValue by remember {
        mutableFloatStateOf(value.toFloat())
    }

    if (showDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    title()
                }
            },
            onDismiss = { showDialog = false },
            onConfirm = {
                val rounded = sliderValue.roundToInt().coerceIn(minValue, maxValue)
                sliderValue = rounded.toFloat()
                showDialog = false
                onValueChange.invoke(rounded)
            },
            onCancel = {
                sliderValue = value.toFloat()
                showDialog = false
            },
            onReset = {
                sliderValue = minValue.toFloat()
            },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val rounded = sliderValue.roundToInt().coerceIn(minValue, maxValue)
                    Text(
                        text = valueText(rounded),
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    Spacer(Modifier.height(16.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it.coerceIn(minValue.toFloat(), maxValue.toFloat()) },
                        valueRange = minValue.toFloat()..maxValue.toFloat(),
                        steps = maxValue - minValue - 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    PreferenceEntry(
        modifier = modifier,
        title = title,
        description = valueText(value),
        icon = icon,
        onClick = { if (isEnabled) showDialog = true },
        isEnabled = isEnabled,
        position = position,
    )
}

@Composable
fun PreferenceGroupTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
fun PreferenceGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    items: List<@Composable () -> Unit>,
) {
    if (items.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (title != null) {
            PreferenceGroupTitle(title = title)
        }
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
            )
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, item ->
                    val isFirst = index == 0
                    val isLast = index == items.size - 1
                    val pos = when {
                        isFirst && isLast -> com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE
                        isFirst -> com.mudassir131.yt.ui.utils.PreferencePosition.FIRST
                        isLast -> com.mudassir131.yt.ui.utils.PreferencePosition.LAST
                        else -> com.mudassir131.yt.ui.utils.PreferencePosition.MIDDLE
                    }
                    val shape = com.mudassir131.yt.ui.utils.getPreferenceShape(pos, 24.dp)

                    androidx.compose.runtime.CompositionLocalProvider(
                        com.mudassir131.yt.ui.utils.LocalPreferencePosition provides pos,
                        com.mudassir131.yt.ui.utils.LocalPreferenceShape provides shape,
                        com.mudassir131.yt.ui.utils.LocalInsidePreferenceGroup provides true,
                    ) {
                        item()
                    }

                    if (index < items.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        if (title != null) {
            PreferenceGroupTitle(title = title)
        }
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
            )
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.runtime.CompositionLocalProvider(
                    com.mudassir131.yt.ui.utils.LocalInsidePreferenceGroup provides true,
                ) {
                    content()
                }
            }
        }
    }
}
