/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */
package com.mudassir131.yt.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mudassir131.yt.BuildConfig
import com.mudassir131.yt.LocalPlayerAwareWindowInsets
import com.mudassir131.yt.R
import com.mudassir131.yt.ui.component.IconButton
import com.mudassir131.yt.ui.utils.backToMain
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val installDate = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(packageInfo.firstInstallTime))
    } catch (_: Exception) {
        "Unknown"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { AboutAppCard() }

            item {
                DeveloperCard(
                    onGitHub = { uriHandler.openUri("https://github.com/mudassir131-dev") },
                    onWebsite = { uriHandler.openUri("https://portfolioooooss.vercel.app") },
                    onTelegram = { uriHandler.openUri("https://t.me/NocturneOfficial7") },
                    onSupport = { launchUpiPayment(context, "touseefparay7-1@okicici", "Mudassir") },
                )
            }

            item { AboutSectionTitle("Contributors") }
            item {
                Column {
                    ContributorCard(
                        imageUrl = "https://avatars.githubusercontent.com/u/107134739?v=4",
                        title = "Archivetune — by koiverse",
                        subtitle = "Base framework",
                        onClick = { uriHandler.openUri("https://github.com/koiverse/ArchiveTune") },
                        position = com.mudassir131.yt.ui.utils.PreferencePosition.FIRST,
                    )
                    ContributorCard(
                        imageUrl = "https://avatars.githubusercontent.com/u/80542861?v=4",
                        title = "MO AGAMY",
                        subtitle = "Metrolist developer",
                        onClick = { uriHandler.openUri("https://github.com/mostafaalagamy") },
                        position = com.mudassir131.yt.ui.utils.PreferencePosition.LAST,
                    )
                }
            }

            item { AboutSectionTitle("Community & Info") }
            item {
                Column {
                    AboutLinkCard(
                        iconRes = R.drawable.github,
                        title = "GitHub Repository",
                        subtitle = "View source code",
                        onClick = { uriHandler.openUri("https://github.com/mudassir131-dev/nocturne") },
                        position = com.mudassir131.yt.ui.utils.PreferencePosition.FIRST,
                    )
                    AboutLinkCard(
                        iconRes = R.drawable.telegram,
                        title = "Telegram Server",
                        subtitle = "Chat with the community and report bugs",
                        onClick = { uriHandler.openUri("https://t.me/NocturneOfficial7") },
                        position = com.mudassir131.yt.ui.utils.PreferencePosition.MIDDLE,
                    )
                    AboutLinkCard(
                        iconRes = R.drawable.website,
                        title = "Nocturne Website",
                        subtitle = "Visit the official website",
                        onClick = { uriHandler.openUri("https://nocturne-music.vercel.app") },
                        position = com.mudassir131.yt.ui.utils.PreferencePosition.LAST,
                    )
                }
            }

            item { AboutSectionTitle("App Info") }
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        AppInfoRow("Installed", installDate)
                        AppInfoRow("Version code", BuildConfig.VERSION_CODE.toString())
                        AppInfoRow(
                            title = "License",
                            value = "GNU GPL v3.0",
                            onClick = { uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.html") },
                        )
                    }
                }
            }

            item { SolidarityFooter() }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun AboutAppCard() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(76.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(
                            if (isDark) R.drawable.ic_nocturne_logo_dark_trans
                            else R.drawable.ic_nocturne_logo_light_trans,
                        ),
                        contentDescription = "Nocturne",
                        modifier = Modifier.size(54.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Nocturne",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AboutBadge(BuildConfig.VERSION_NAME)
                    AboutBadge(if (BuildConfig.DEBUG) "DEBUG" else "UNIVERSAL")
                }
            }
        }
    }
}

@Composable
private fun AboutBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun DeveloperCard(
    onGitHub: () -> Unit,
    onWebsite: () -> Unit,
    onTelegram: () -> Unit,
    onSupport: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.developer_mudassir),
                    contentDescription = "Mudassir",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(28.dp)),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Mudassir",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "App developer",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AboutIconAction(R.drawable.website, "Website", onWebsite, Modifier.weight(1f))
                AboutIconAction(R.drawable.github, "GitHub", onGitHub, Modifier.weight(1f))
                AboutIconAction(R.drawable.telegram, "Telegram", onTelegram, Modifier.weight(1f))
            }

            Button(
                onClick = onSupport,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.favorite),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text("Support the developer", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AboutIconAction(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun AboutSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 10.dp, start = 4.dp),
    )
}

@Composable
private fun ContributorCard(
    imageUrl: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    position: com.mudassir131.yt.ui.utils.PreferencePosition = com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE,
) {
    val topPadding = if (position == com.mudassir131.yt.ui.utils.PreferencePosition.FIRST || position == com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE) 4.dp else 0.5.dp
    val bottomPadding = if (position == com.mudassir131.yt.ui.utils.PreferencePosition.LAST || position == com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE) 4.dp else 0.5.dp

    Surface(
        onClick = onClick,
        shape = com.mudassir131.yt.ui.utils.getPreferenceShape(position, 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(top = topPadding, bottom = bottomPadding),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                painter = painterResource(R.drawable.github),
                contentDescription = "GitHub",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun AboutLinkCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    position: com.mudassir131.yt.ui.utils.PreferencePosition = com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE,
) {
    val topPadding = if (position == com.mudassir131.yt.ui.utils.PreferencePosition.FIRST || position == com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE) 4.dp else 0.5.dp
    val bottomPadding = if (position == com.mudassir131.yt.ui.utils.PreferencePosition.LAST || position == com.mudassir131.yt.ui.utils.PreferencePosition.SINGLE) 4.dp else 0.5.dp

    Surface(
        onClick = onClick,
        shape = com.mudassir131.yt.ui.utils.getPreferenceShape(position, 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(top = topPadding, bottom = bottomPadding),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppInfoRow(
    title: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null, onClick = onClick ?: {})
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun SolidarityFooter() {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "This Project stands with",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                FlagLabel(R.drawable.ic_flag_palestine, "Palestine")
                Text(
                    text = "and",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
                FlagLabel(R.drawable.ic_flag_kashmir, "Kashmir")
            }
        }
    }
}

@Composable
private fun FlagLabel(flagRes: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(flagRes),
            contentDescription = "$label flag",
            modifier = Modifier
                .size(width = 36.dp, height = 24.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.width(7.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

fun launchUpiPayment(context: android.content.Context, upiId: String, payeeName: String) {
    val note = "Support for Nocturne"
    val uriString =
        "upi://pay?pa=$upiId&pn=${android.net.Uri.encode(payeeName)}&tn=${android.net.Uri.encode(note)}&cu=INR"
    val intent = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse(uriString),
    )
    val chooser = android.content.Intent.createChooser(intent, "Pay with...")
    try {
        context.startActivity(chooser)
    } catch (_: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(
            context,
            "No UPI app found on this device.",
            android.widget.Toast.LENGTH_SHORT,
        ).show()
    }
}