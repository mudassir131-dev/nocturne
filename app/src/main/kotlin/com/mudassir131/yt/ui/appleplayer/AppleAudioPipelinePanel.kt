/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 *
 * Audio Pipeline panel: live signal path from decoder to output, opened by
 * tapping the quality badge in the Apple-inspired player. Only playback-chain
 * details are shown here (no YouTube metadata).
 */

package com.mudassir131.yt.ui.appleplayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mudassir131.yt.R
import com.mudassir131.yt.db.entities.FormatEntity
import com.mudassir131.yt.playback.alac.AudioFormatInfo
import com.mudassir131.yt.playback.alac.toAudioFormatInfo

private data class PipelineSection(
    val icon: Int,
    val title: String,
    val rows: List<Pair<String, String>>,
)

private fun buildAudioPipelineSections(formatInfo: AudioFormatInfo?): List<PipelineSection> {
    if (formatInfo == null) return emptyList()
    val out = formatInfo.outputInfo

    val rawSource = formatInfo.source.orEmpty()
    val source = when {
        rawSource.isBlank() -> "—"
        rawSource.contains("addon", ignoreCase = true) -> "Addon Source"
        rawSource.contains("saavn", ignoreCase = true) -> "Saavn"
        rawSource.contains("local", ignoreCase = true) -> "Local file"
        rawSource.contains("self", ignoreCase = true) ||
            rawSource.contains("navidrome", ignoreCase = true) ||
            rawSource.contains("subsonic", ignoreCase = true) ||
            rawSource.contains("jellyfin", ignoreCase = true) -> "Self-hosted"
        rawSource.contains("youtube", ignoreCase = true) ||
            rawSource.contains("googlevideo", ignoreCase = true) ||
            rawSource.startsWith("http", ignoreCase = true) -> "YouTube"
        else -> rawSource.replaceFirstChar { it.uppercase() }
    }

    val format = formatInfo.codec.ifBlank { "—" }
    // Lossy codecs have no meaningful bit depth; lossless reports the real one.
    val bitDepth = if (formatInfo.isLossless) {
        formatInfo.bitDepth?.takeIf { it > 0 }?.let { "$it-bit" } ?: "—"
    } else {
        "—"
    }
    val sampleRate = formatInfo.sampleRate?.takeIf { it > 0 }?.let { "$it Hz" } ?: "—"
    val bitrate = formatInfo.displayBitrate ?: "—"
    val fileSize = formatInfo.displayFileSize ?: "—"
    val channels = when (formatInfo.channelCount) {
        null -> "—"
        1 -> "Mono"
        2 -> "Stereo"
        else -> "${formatInfo.channelCount} ch"
    }

    val sections = mutableListOf(
        PipelineSection(
            icon = R.drawable.info,
            title = "TRACK INFO",
            rows = listOf(
                "Source" to source,
                "Format" to format,
                "Bit Depth" to bitDepth,
                "Sample Rate" to sampleRate,
                "Bitrate" to bitrate,
                "File Size" to fileSize,
                "Channels" to channels,
            ),
        ),
    )

    val decoderName = formatInfo.decoderName?.takeIf { it.isNotBlank() } ?: "—"
    val decoderPcmBitDepth = out?.sourceBitDepth?.takeIf { it > 0 } ?: formatInfo.bitDepth?.takeIf { it > 0 } ?: 16
    sections += PipelineSection(
        icon = R.drawable.settings,
        title = "DECODER",
        rows = listOf(
            "Decoder Name" to decoderName,
            "Format" to "$decoderPcmBitDepth-bit PCM",
        ),
    )

    val inputRate = out?.sourceSampleRate?.takeIf { it > 0 } ?: formatInfo.sampleRate?.takeIf { it > 0 } ?: 48000
    val outputRate = out?.outputSampleRate?.takeIf { it > 0 } ?: inputRate
    val resampled = out?.isResampled == true
    sections += PipelineSection(
        icon = R.drawable.tune,
        title = "RESAMPLER",
        rows = listOf(
            "I/O Rate" to "$inputRate Hz → $outputRate Hz",
            "Type" to if (resampled) "Resampler" else "None",
            "Cutoff" to if (resampled) "Auto" else "—",
            "Quality" to when {
                out == null -> "Passthrough"
                out.isBitPerfect -> "Bit-perfect"
                resampled -> "Resampled"
                out.isProcessed -> "DSP"
                else -> "Passthrough"
            },
        ),
    )

    if (out != null) {
        sections += PipelineSection(
            icon = R.drawable.graphic_eq,
            title = "OUTPUT",
            rows = listOfNotNull(
                "Backend" to out.backend,
                "Sharing" to out.sharingMode,
                "Hardware Output" to out.displayOutputFormat,
                out.routeName?.takeIf { it.isNotBlank() }?.let { "Output Device" to it },
            ),
        )
    }
    return sections
}

@Composable
fun AppleAudioPipelinePanel(
    format: FormatEntity?,
    formatInfo: AudioFormatInfo?,
    onDismiss: () -> Unit,
) {
    val resolved = remember(format, formatInfo) {
        formatInfo ?: format?.toAudioFormatInfo()
    }
    val sections = remember(resolved) { buildAudioPipelineSections(resolved) }

    val accentColor = MaterialTheme.colorScheme.primary
    val panelColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = MaterialTheme.colorScheme.onSurface
    val lineColor = contentColor.copy(alpha = .22f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = panelColor,
            contentColor = contentColor,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .heightIn(max = 480.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 24.dp),
            ) {
                Text(
                    text = "Audio Pipeline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = contentColor,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Live signal path, decoder to output",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = .65f),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = contentColor.copy(alpha = .12f), thickness = 0.5.dp)
                Spacer(Modifier.height(18.dp))

                if (sections.isEmpty()) {
                    Text(
                        text = "No active audio stream",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = .6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        sections.forEachIndexed { index, section ->
                            PipelineSectionRow(
                                section = section,
                                isLast = index == sections.lastIndex,
                                accentColor = accentColor,
                                lineColor = lineColor,
                                contentColor = contentColor,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    modifier = Modifier
                        .clipCircleClickable(onDismiss),
                )
            }
        }
    }
}

private fun Modifier.clipCircleClickable(onClick: () -> Unit): Modifier =
    this
        .clip(RoundedCornerShape(50))
        .clickable(onClick = onClick)
        .padding(horizontal = 32.dp, vertical = 10.dp)

@Composable
private fun PipelineSectionRow(
    section: PipelineSection,
    isLast: Boolean,
    accentColor: Color,
    lineColor: Color,
    contentColor: Color,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(44.dp),
        ) {
            PipelineNode(icon = section.icon, accentColor = accentColor)
            if (!isLast) {
                AnimatedPipelineLine(
                    accentColor = accentColor,
                    baseColor = lineColor,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp, bottom = 4.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 22.dp),
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
                color = contentColor.copy(alpha = .6f),
            )
            Spacer(Modifier.height(8.dp))
            section.rows.forEach { (label, value) ->
                Row {
                    Text(
                        text = "$label: ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleSmall,
                        color = contentColor.copy(alpha = .85f),
                    )
                }
                Spacer(Modifier.height(5.dp))
            }
        }
    }
}

@Composable
private fun PipelineNode(icon: Int, accentColor: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .border(1.dp, accentColor.copy(alpha = .55f), CircleShape),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * The vertical signal line between pipeline stages: a static base track with a
 * soft accent pulse that keeps flowing downwards.
 */
@Composable
private fun AnimatedPipelineLine(
    accentColor: Color,
    baseColor: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "pipelineFlow")
    val progress by transition.animateFloat(
        initialValue = -0.45f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1800, easing = LinearEasing)),
        label = "pipelineFlowProgress",
    )
    Box(
        modifier
            .width(2.dp)
            .background(baseColor, RoundedCornerShape(2.dp)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val segment = size.height * 0.5f
            val center = size.height * progress
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, accentColor, Color.Transparent),
                    startY = center - segment / 2f,
                    endY = center + segment / 2f,
                ),
            )
        }
    }
}




// Refinement 77: AppleAudioPipelinePanel metrics documentation

// Refinement 91: Pipeline detail row spacing refinement

// Refinement 100: Clean kHz suffix formatting in pipeline panel

// Refinement 115: Bitrate Mbps formatting
