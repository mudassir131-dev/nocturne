/*
 * Nocturne - by Mudassir

 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mudassir131.yt.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun UpdateDialog(
    currentVersion: String,
    latestVersion: String,
    releaseDate: String,
    releaseNotes: String,
    downloadUrl: String,
    onDismissRequest: () -> Unit,
    onLater: () -> Unit,
) {
    val context = LocalContext.current
    val downloadManager = remember {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }
    var downloadId by rememberSaveable { mutableStateOf<Long?>(null) }
    var downloadProgress by rememberSaveable { mutableFloatStateOf(0f) }
    var downloadComplete by rememberSaveable { mutableStateOf(false) }
    var downloadFailed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(downloadId) {
        val id = downloadId ?: return@LaunchedEffect
        while (!downloadComplete && !downloadFailed) {
            downloadManager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
                if (!cursor.moveToFirst()) {
                    downloadFailed = true
                } else {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    if (total > 0) {
                        downloadProgress = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    }
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloadProgress = 1f
                            downloadComplete = true
                        }
                        DownloadManager.STATUS_FAILED -> downloadFailed = true
                    }
                }
            }
            if (!downloadComplete && !downloadFailed) delay(350)
        }
    }

    if (downloadId != null) {
        UpdateDownloadProgressDialog(
            latestVersion = latestVersion,
            progress = downloadProgress,
            isComplete = downloadComplete,
            isFailed = downloadFailed,
            downloadId = downloadId!!,
            downloadManager = downloadManager,
            onClose = onDismissRequest,
        )
        return
    }

    
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF0F0F0F),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Circular Badge Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFF1C1C1E), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.update),
                        contentDescription = "Update Available",
                        tint = Color(0xFFD2C795),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "New Update Available!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle / Version
                Text(
                    text = "Version ${latestVersion.trim().removePrefix("v").removePrefix("V")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD2C795)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Nested Content Card ("What's New:")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141414), shape = RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "What's New:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val notes = releaseNotes.ifBlank { com.mudassir131.yt.utils.Updater.GenericReleaseNotes }
                        MarkdownText(
                            markdown = notes,
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onLater) {
                        Text(
                            text = "Later",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Button(
                        onClick = {
                            try {
                                val fileName = "Nocturne-${latestVersion.trim().replace(' ', '-')}.apk"
                                val request = DownloadManager.Request(Uri.parse(downloadUrl))
                                    .setTitle("Nocturne $latestVersion")
                                    .setDescription("Downloading app update")
                                    .setMimeType("application/vnd.android.package-archive")
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                downloadId = downloadManager.enqueue(request)
                                Log.d("NocturneUpdater", "In-app download enqueued: $downloadUrl")
                            } catch (e: Exception) {
                                Log.e("NocturneUpdater", "Unable to start in-app download", e)
                                downloadFailed = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD2C795),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.download),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download Now",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateDownloadProgressDialog(
    latestVersion: String,
    progress: Float,
    isComplete: Boolean,
    isFailed: Boolean,
    downloadId: Long,
    downloadManager: DownloadManager,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F0F), RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(64.dp).background(Color(0xFF1C1C1E), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (isComplete) R.drawable.check else R.drawable.download),
                        contentDescription = null,
                        tint = Color(0xFFD2C795),
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = when {
                        isFailed -> "Download failed"
                        isComplete -> "Update ready to install"
                        else -> "Downloading Nocturne"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "Version ${latestVersion.trim().removePrefix("v").removePrefix("V")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD2C795),
                )
                Spacer(Modifier.height(28.dp))
                WavyDownloadProgress(progress)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = when {
                        isFailed -> "Please close this screen and try the download again."
                        isComplete -> "100%  Download complete"
                        else -> "${(progress * 100).toInt()}%  Keep Nocturne open while downloading"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                )
                Spacer(Modifier.height(28.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                        Text("Close")
                    }
                    if (isComplete) {
                        Button(
                            onClick = {
                                val uri = downloadManager.getUriForDownloadedFile(downloadId)
                                if (uri != null) {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/vnd.android.package-archive")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD2C795),
                                contentColor = Color.Black,
                            ),
                        ) {
                            Text("Install Update", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WavyDownloadProgress(progress: Float) {
    val primary = Color(0xFFD2C795)
    val track = Color.White.copy(alpha = 0.18f)
    Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
        val amplitude = 3.dp.toPx()
        val period = 18.dp.toPx()
        val centerY = size.height / 2f
        val path = Path()
        var x = 0f
        while (x <= size.width + 2f) {
            val y = centerY + sin((x / period) * 2f * PI.toFloat()) * amplitude
            if (x == 0f) path.moveTo(x, y) else path.lineTo(x, y)
            x += 2f
        }
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        drawPath(path, track, style = stroke)
        clipRect(right = size.width * progress.coerceIn(0f, 1f)) {
            drawPath(path, primary, style = stroke)
        }
    }
}

@Composable
fun WelcomeUpdateDialog(
    versionName: String,
    releaseNotes: String,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF0F0F0F),
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFF1C1C1E), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.update),
                        contentDescription = "Welcome to Nocturne",
                        tint = Color(0xFFD2C795),
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Welcome to Nocturne $versionName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Thank you for updating Nocturne.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8E8E93)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141414), shape = RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "What's New:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val notes = releaseNotes.ifBlank { com.mudassir131.yt.utils.Updater.GenericReleaseNotes }
                        MarkdownText(
                            markdown = notes,
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD2C795),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = "Awesome",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
