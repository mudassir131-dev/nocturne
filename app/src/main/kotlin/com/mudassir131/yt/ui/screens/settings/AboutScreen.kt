/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */
package com.mudassir131.yt.ui.screens.settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mudassir131.yt.BuildConfig
import com.mudassir131.yt.LocalPlayerAwareWindowInsets
import com.mudassir131.yt.R
import com.mudassir131.yt.ui.component.IconButton
import com.mudassir131.yt.ui.utils.PreferencePosition
import com.mudassir131.yt.ui.utils.backToMain
import com.mudassir131.yt.ui.utils.getPreferenceShape

private data class ContributorInfo(
    val name: String,
    val avatarUrl: String,
    val githubUrl: String,
)

private val NocturneContributors = listOf(
    ContributorInfo(
        name = "koiverse (ArchiveTune)",
        avatarUrl = "https://github.com/koiverse.png",
        githubUrl = "https://github.com/koiverse",
    ),
    ContributorInfo(
        name = "MO AGAMY (Metrolist)",
        avatarUrl = "https://github.com/mostafaalagamy.png",
        githubUrl = "https://github.com/mostafaalagamy",
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val collapsibleScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(collapsibleScrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.about),
                        fontWeight = FontWeight.Bold,
                    )
                },
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
                scrollBehavior = collapsibleScrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
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
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AboutHeader()
            }

            item {
                DeveloperCard(
                    onGitHub = { uriHandler.openUri("https://github.com/mudassir131-dev") },
                    onWebsite = { uriHandler.openUri("https://portfolioooooss.vercel.app") },
                    onInstagram = { uriHandler.openUri("https://instagram.com/mud4sssir7") },
                    onSupport = { launchUpiPayment(context, "touseefparay7-1@okicici", "Mudassir") },
                )
            }

            item {
                AboutSectionTitle("CONTRIBUTORS")
            }
            item {
                ContributorsRow(
                    contributors = NocturneContributors,
                    onContributorClick = { url -> uriHandler.openUri(url) },
                )
            }

            item {
                AboutSectionTitle("SOURCE CODE & WEBSITE")
            }
            item {
                Column {
                    AboutLinkCard(
                        iconRes = R.drawable.github,
                        title = "Source Code",
                        subtitle = "mudassir131-dev/nocturne",
                        onClick = { uriHandler.openUri("https://github.com/mudassir131-dev/nocturne") },
                        position = PreferencePosition.FIRST,
                    )
                    AboutLinkCard(
                        iconRes = R.drawable.website,
                        title = "Website",
                        subtitle = "nocturne-music.vercel.app",
                        onClick = { uriHandler.openUri("https://nocturne-music.vercel.app") },
                        position = PreferencePosition.LAST,
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun AboutHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "NOCTURNE",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AboutCapsuleBadge(text = "RELEASE")
            AboutCapsuleBadge(
                text = BuildConfig.VERSION_NAME.ifBlank { "2.22.36" },
            )
        }
    }
}

@Composable
private fun AboutCapsuleBadge(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun DeveloperCard(
    onGitHub: () -> Unit,
    onWebsite: () -> Unit,
    onInstagram: () -> Unit,
    onSupport: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AsyncImage(
                model = "https://github.com/mudassir131-dev.png",
                placeholder = painterResource(R.drawable.developer_mudassir),
                error = painterResource(R.drawable.developer_mudassir),
                fallback = painterResource(R.drawable.developer_mudassir),
                contentDescription = "Mudassir",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Mudassir",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "App developer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }

            // Compact circular CTA buttons for GitHub, Portfolio Website, and Instagram
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleCtaButton(
                    iconRes = R.drawable.github,
                    contentDescription = "GitHub",
                    onClick = onGitHub,
                )
                CircleCtaButton(
                    iconRes = R.drawable.website,
                    contentDescription = "Website",
                    onClick = onWebsite,
                )
                CircleCtaButton(
                    iconRes = R.drawable.instagram,
                    contentDescription = "@mud4sssir7 Instagram",
                    onClick = onInstagram,
                )
            }

            Spacer(Modifier.height(2.dp))

            Button(
                onClick = onSupport,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.favorite),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Support the developer", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CircleCtaButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(46.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ContributorsRow(
    contributors: List<ContributorInfo>,
    onContributorClick: (String) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(contributors) { contributor ->
            Surface(
                onClick = { onContributorClick(contributor.githubUrl) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(54.dp),
            ) {
                AsyncImage(
                    model = contributor.avatarUrl,
                    placeholder = painterResource(R.drawable.github),
                    error = painterResource(R.drawable.github),
                    fallback = painterResource(R.drawable.github),
                    contentDescription = contributor.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                )
            }
        }
    }
}

@Composable
private fun AboutSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}

@Composable
private fun AboutLinkCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    position: PreferencePosition = PreferencePosition.SINGLE,
) {
    val topPadding = if (position == PreferencePosition.FIRST || position == PreferencePosition.SINGLE) 4.dp else 0.5.dp
    val bottomPadding = if (position == PreferencePosition.LAST || position == PreferencePosition.SINGLE) 4.dp else 0.5.dp

    Surface(
        onClick = onClick,
        shape = getPreferenceShape(position, 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = bottomPadding),
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