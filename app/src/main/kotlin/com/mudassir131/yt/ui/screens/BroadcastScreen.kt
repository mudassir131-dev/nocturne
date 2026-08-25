/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mudassir131.yt.LocalPlayerAwareWindowInsets
import com.mudassir131.yt.R
import com.mudassir131.yt.models.BroadcastMessage
import com.mudassir131.yt.models.BroadcastTag
import com.mudassir131.yt.ui.component.GoogleLoadingIndicator
import com.mudassir131.yt.ui.component.InstagramVerifiedBadge
import com.mudassir131.yt.utils.BroadcastManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AvailableEmojis = listOf("❤️", "🔥", "🚀", "🎉", "👍", "👏", "💯", "🎵", "⚡", "🌟")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(navController: NavController) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    val messages by BroadcastManager.messages.collectAsState()
    val isDeveloperMode by BroadcastManager.isDeveloperMode.collectAsState()
    val isLoading by BroadcastManager.isLoading.collectAsState()

    val listState = rememberLazyListState()

    var showDeveloperAuthDialog by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var showQuickReactionDialogForMsg by remember { mutableStateOf<BroadcastMessage?>(null) }
    var selectedFullscreenImage by remember { mutableStateOf<String?>(null) }

    // Developer composer state
    var composerTitle by remember { mutableStateOf("") }
    var composerContent by remember { mutableStateOf("") }
    var composerMediaUriOrUrl by remember { mutableStateOf("") }
    var composerActionText by remember { mutableStateOf("") }
    var composerActionUrl by remember { mutableStateOf("") }
    var composerTag by remember { mutableStateOf(BroadcastTag.ANNOUNCEMENT) }

    // Media picker launcher for Local Images and animated GIFs
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedUri = copyUriToInternalStorage(context, uri)
                composerMediaUriOrUrl = savedUri
                Toast.makeText(context, "Media attached! 📸", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Scroll to bottom on initial load / new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        BroadcastManager.syncRemoteAnnouncements()
    }

    var devTapCount by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var lastTapTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val now = System.currentTimeMillis()
                                    if (now - lastTapTime > 2500L) {
                                        devTapCount = 0
                                    }
                                    lastTapTime = now
                                    devTapCount++
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                    if (devTapCount in 3..6) {
                                        val remaining = 7 - devTapCount
                                        Toast.makeText(context, "$remaining taps to Developer Access", Toast.LENGTH_SHORT).show()
                                    } else if (devTapCount >= 7) {
                                        devTapCount = 0
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showDeveloperAuthDialog = true
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(R.drawable.broadcast),
                                    contentDescription = "Nocturne Broadcast",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "Announcements",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                InstagramVerifiedBadge(size = 15.dp)
                            }
                            Text(
                                text = "Official Nocturne Broadcast",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                BroadcastManager.syncRemoteAnnouncements()
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.cached),
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .imePadding() // Ensures whole container & composer moves up with soft keyboard
        ) {
            // Stream messages (Google Messages style)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            GoogleLoadingIndicator(size = 48.dp)
                        } else {
                            Text(
                                text = "No announcements yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            BroadcastMessageItem(
                                message = msg,
                                isDeveloperMode = isDeveloperMode,
                                onReactionClick = { emoji ->
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    BroadcastManager.toggleReaction(msg.id, emoji)
                                },
                                onAddReactionClick = {
                                    showQuickReactionDialogForMsg = msg
                                },
                                onImageClick = { url ->
                                    selectedFullscreenImage = url
                                },
                                onActionClick = { url ->
                                    if (url.startsWith("nocturne://settings/update")) {
                                        navController.navigate("settings/update")
                                    } else if (url.startsWith("http")) {
                                        uriHandler.openUri(url)
                                    } else {
                                        navController.navigate(url)
                                    }
                                },
                                onDeleteClick = {
                                    BroadcastManager.deleteAnnouncement(msg.id)
                                },
                                onCopyClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Announcement", msg.content))
                                    Toast.makeText(context, "Copied announcement to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Google Messages Bottom Composer Bar (Only visible when authenticated as Developer)
            AnimatedVisibility(
                visible = isDeveloperMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                DeveloperComposerBar(
                    title = composerTitle,
                    onTitleChange = { composerTitle = it },
                    content = composerContent,
                    onContentChange = { composerContent = it },
                    selectedTag = composerTag,
                    onTagSelect = { composerTag = it },
                    attachedMedia = composerMediaUriOrUrl,
                    onRemoveMedia = { composerMediaUriOrUrl = "" },
                    onPickLocalMedia = { mediaPickerLauncher.launch("image/*") },
                    onCustomAttachmentClick = { showAttachmentDialog = true },
                    onSend = {
                        if (composerContent.isNotBlank() || composerMediaUriOrUrl.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            BroadcastManager.postAnnouncement(
                                title = composerTitle,
                                content = composerContent,
                                imageUrl = composerMediaUriOrUrl,
                                tag = composerTag,
                                actionText = composerActionText,
                                actionUrl = composerActionUrl
                            )
                            composerTitle = ""
                            composerContent = ""
                            composerMediaUriOrUrl = ""
                            composerActionText = ""
                            composerActionUrl = ""
                        }
                    }
                )
            }
        }
    }

    // Developer Login Modal
    if (showDeveloperAuthDialog) {
        DeveloperAuthDialog(
            isCurrentlyDev = isDeveloperMode,
            onDismiss = { showDeveloperAuthDialog = false },
            onLogin = { passkey ->
                val ok = BroadcastManager.verifyAndLoginDeveloper(passkey)
                if (ok) {
                    Toast.makeText(context, "Developer Mode Unlocked! 🚀", Toast.LENGTH_SHORT).show()
                    showDeveloperAuthDialog = false
                } else {
                    Toast.makeText(context, "Invalid Developer Passkey", Toast.LENGTH_SHORT).show()
                }
            },
            onLogout = {
                BroadcastManager.logoutDeveloper()
                showDeveloperAuthDialog = false
                Toast.makeText(context, "Developer Mode Locked", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Attachment Configuration Dialog (for Custom URLs / CTA link)
    if (showAttachmentDialog) {
        AttachmentDialog(
            mediaUrl = composerMediaUriOrUrl,
            onMediaUrlChange = { composerMediaUriOrUrl = it },
            onPickLocal = { mediaPickerLauncher.launch("image/*") },
            actionText = composerActionText,
            onActionTextChange = { composerActionText = it },
            actionUrl = composerActionUrl,
            onActionUrlChange = { composerActionUrl = it },
            onDismiss = { showAttachmentDialog = false }
        )
    }

    // Quick Reaction Picker Dialog
    showQuickReactionDialogForMsg?.let { targetMsg ->
        QuickReactionDialog(
            onEmojiSelect = { emoji ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                BroadcastManager.toggleReaction(targetMsg.id, emoji)
                showQuickReactionDialogForMsg = null
            },
            onDismiss = { showQuickReactionDialogForMsg = null }
        )
    }

    // Fullscreen Image Preview
    selectedFullscreenImage?.let { imgUrl ->
        FullscreenImageDialog(
            imageUrl = imgUrl,
            onDismiss = { selectedFullscreenImage = null }
        )
    }
}

/**
 * Google Messages Style Chat Bubble for Broadcast Messages
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BroadcastMessageItem(
    message: BroadcastMessage,
    isDeveloperMode: Boolean,
    onReactionClick: (String) -> Unit,
    onAddReactionClick: () -> Unit,
    onImageClick: (String) -> Unit,
    onActionClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onCopyClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        // Date / Time Separator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
            ) {
                Text(
                    text = formatMessageTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Message Bubble Container (Google Messages styling)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            // Developer Avatar
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.developer_mudassir),
                        contentDescription = "Mudassir",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Bubble Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 20.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 20.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = onCopyClick
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        // Header: Author & Instagram Verified Badge & Tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = message.authorName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                // Dedicated Instagram Verified Badge
                                InstagramVerifiedBadge(size = 15.dp)
                            }

                            // Tag Badge (e.g. 🚀 Update, ✨ Feature)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when (message.tag) {
                                    BroadcastTag.UPDATE -> MaterialTheme.colorScheme.primaryContainer
                                    BroadcastTag.FEATURE -> MaterialTheme.colorScheme.secondaryContainer
                                    BroadcastTag.HOTFIX -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            ) {
                                Text(
                                    text = "${message.tag.emoji} ${message.tag.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (message.title.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = message.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (message.content.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Attached Media / Image / GIF (Supports local files, Uri, and network URLs)
                        val mediaUrl = message.imageUrl ?: message.gifUrl
                        if (!mediaUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                                    .clickable { onImageClick(mediaUrl) }
                            ) {
                                AsyncImage(
                                    model = mediaUrl,
                                    contentDescription = "Announcement Media",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        // CTA Button Link
                        if (!message.actionUrl.isNullOrBlank() && !message.actionText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onActionClick(message.actionUrl) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(
                                    text = message.actionText,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        // Footer (Time + Double Check icon)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTimeOnly(message.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                painter = painterResource(R.drawable.check),
                                contentDescription = "Delivered",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Reactions Bar (Single active reaction per user)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    message.reactions.forEach { (emoji, count) ->
                        if (count > 0) {
                            val isReacted = message.userReactions.contains(emoji)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isReacted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onReactionClick(emoji) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = emoji, fontSize = 13.sp)
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isReacted) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isReacted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Add reaction button (+)
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable { onAddReactionClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = "Add reaction",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (isDeveloperMode) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clickable { onDeleteClick() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Google Messages Style Developer Bottom Composer Capsule (Moves up with Keyboard)
 */
@Composable
private fun DeveloperComposerBar(
    title: String,
    onTitleChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    selectedTag: BroadcastTag,
    onTagSelect: (BroadcastTag) -> Unit,
    attachedMedia: String,
    onRemoveMedia: () -> Unit,
    onPickLocalMedia: () -> Unit,
    onCustomAttachmentClick: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Status bar indicating developer mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34A853))
                    )
                    Text(
                        text = "Developer Console (Mudassir)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Tag selector chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.clickable {
                        val nextTag = when (selectedTag) {
                            BroadcastTag.ANNOUNCEMENT -> BroadcastTag.UPDATE
                            BroadcastTag.UPDATE -> BroadcastTag.FEATURE
                            BroadcastTag.FEATURE -> BroadcastTag.HOTFIX
                            BroadcastTag.HOTFIX -> BroadcastTag.GENERAL
                            BroadcastTag.GENERAL -> BroadcastTag.ANNOUNCEMENT
                        }
                        onTagSelect(nextTag)
                    }
                ) {
                    Text(
                        text = "${selectedTag.emoji} ${selectedTag.label}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Attached Media Thumbnail Preview (if attached from storage)
            if (attachedMedia.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(44.dp)
                        ) {
                            AsyncImage(
                                model = attachedMedia,
                                contentDescription = "Attached media",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Text(
                            text = "Media attached (Image/GIF)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onRemoveMedia,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "Remove media",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Google Messages Style Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Gallery / Local Media Picker Button
                IconButton(
                    onClick = onPickLocalMedia,
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (attachedMedia.isNotBlank()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                            CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.image),
                        contentDescription = "Pick Image / GIF from device storage",
                        tint = if (attachedMedia.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // CTA Link Button
                IconButton(
                    onClick = onCustomAttachmentClick,
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.link),
                        contentDescription = "Attach CTA Link / URL",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Message Text Field
                OutlinedTextField(
                    value = content,
                    onValueChange = onContentChange,
                    placeholder = { Text("Broadcast announcement…") },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp, max = 120.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                // Send Button (Google Messages pill style)
                IconButton(
                    onClick = onSend,
                    enabled = content.isNotBlank() || attachedMedia.isNotBlank(),
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (content.isNotBlank() || attachedMedia.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                            CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = "Send",
                        tint = if (content.isNotBlank() || attachedMedia.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Developer Passkey Auth Dialog
 */
@Composable
private fun DeveloperAuthDialog(
    isCurrentlyDev: Boolean,
    onDismiss: () -> Unit,
    onLogin: (String) -> Unit,
    onLogout: () -> Unit,
) {
    var passkey by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(if (isCurrentlyDev) R.drawable.lock_open else R.drawable.lock),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Developer Authentication")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isCurrentlyDev) {
                    Text(
                        text = "You are authenticated in Developer Mode (Mudassir). You have full access to broadcast announcements and media.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Enter the Developer Master Passkey to unlock posting tools:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = passkey,
                        onValueChange = { passkey = it },
                        placeholder = { Text("Enter Passkey") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    painter = painterResource(if (showPassword) R.drawable.hide_image else R.drawable.image),
                                    contentDescription = "Toggle visibility"
                                )
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (isCurrentlyDev) {
                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Lock & Logout")
                }
            } else {
                Button(
                    onClick = { onLogin(passkey) },
                    enabled = passkey.isNotBlank()
                ) {
                    Text("Unlock")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Custom Attachment & CTA Dialog
 */
@Composable
private fun AttachmentDialog(
    mediaUrl: String,
    onMediaUrlChange: (String) -> Unit,
    onPickLocal: () -> Unit,
    actionText: String,
    onActionTextChange: (String) -> Unit,
    actionUrl: String,
    onActionUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attach Media & CTA Button") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onPickLocal()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.image),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Choose from Gallery (Image / GIF)")
                }

                OutlinedTextField(
                    value = mediaUrl,
                    onValueChange = onMediaUrlChange,
                    placeholder = { Text("Or direct URL (https://...)") },
                    label = { Text("Media URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = actionText,
                    onValueChange = onActionTextChange,
                    placeholder = { Text("e.g. Check for Update") },
                    label = { Text("CTA Button Text (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = actionUrl,
                    onValueChange = onActionUrlChange,
                    placeholder = { Text("e.g. nocturne://settings/update") },
                    label = { Text("CTA Button URL / Route") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

/**
 * Quick Reaction Emoji Dialog
 */
@Composable
private fun QuickReactionDialog(
    onEmojiSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("React to Announcement") },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvailableEmojis.forEach { emoji ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { onEmojiSelect(emoji) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 22.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Fullscreen Image Preview
 */
@Composable
private fun FullscreenImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Fullscreen preview",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Copies a content Uri into local app files storage so it persists and survives reboots
 */
private suspend fun copyUriToInternalStorage(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext uri.toString()
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val extension = when {
            mimeType.contains("gif") -> "gif"
            mimeType.contains("png") -> "png"
            mimeType.contains("webp") -> "webp"
            else -> "jpg"
        }
        val fileName = "broadcast_media_${System.currentTimeMillis()}.$extension"
        val destinationFile = File(context.filesDir, fileName)
        destinationFile.outputStream().use { output ->
            inputStream.copyTo(output)
        }
        destinationFile.toURI().toString()
    } catch (e: Exception) {
        uri.toString()
    }
}

private fun formatMessageTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val oneDay = 24 * 60 * 60 * 1000L
    return when {
        diff < oneDay -> "Today"
        diff < 2 * oneDay -> "Yesterday"
        else -> {
            val date = Date(timestamp)
            val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            formatter.format(date)
        }
    }
}

private fun formatTimeOnly(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(date)
}
