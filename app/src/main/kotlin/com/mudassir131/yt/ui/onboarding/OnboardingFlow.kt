package com.mudassir131.yt.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mudassir131.yt.R
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class OnboardingProfile(
    val name: String,
    val gender: String,
    val dateOfBirthMillis: Long,
)

private enum class OnboardingStep { WELCOME, DETAILS }

private val OnboardingEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
fun OnboardingFlow(
    darkTheme: Boolean,
    onComplete: suspend (OnboardingProfile) -> Unit,
) {
    var step by remember { mutableStateOf(OnboardingStep.WELCOME) }
    BackHandler(enabled = step == OnboardingStep.DETAILS) { step = OnboardingStep.WELCOME }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            val forward = targetState == OnboardingStep.DETAILS
            val enter = slideIntoContainer(
                towards = if (forward) AnimatedContentTransitionScope.SlideDirection.Left
                else AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(440, easing = OnboardingEasing),
            ) + fadeIn(tween(300, delayMillis = 70))
            val exit = slideOutOfContainer(
                towards = if (forward) AnimatedContentTransitionScope.SlideDirection.Left
                else AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(440, easing = OnboardingEasing),
            ) + fadeOut(tween(220))
            enter togetherWith exit
        },
        label = "onboarding-step",
        modifier = Modifier.fillMaxSize(),
    ) { current ->
        when (current) {
            OnboardingStep.WELCOME -> WelcomeScreen(
                darkTheme = darkTheme,
                onGetStarted = { step = OnboardingStep.DETAILS },
            )
            OnboardingStep.DETAILS -> DetailsScreen(
                darkTheme = darkTheme,
                onBack = { step = OnboardingStep.WELCOME },
                onComplete = onComplete,
            )
        }
    }
}

@Composable
private fun OnboardingBackground(darkTheme: Boolean, content: @Composable () -> Unit) {
    val background = if (darkTheme) Color(0xFF090B16) else Color(0xFFFAF9FC)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF8456E8).copy(alpha = if (darkTheme) 0.16f else 0.08f),
                            Color.Transparent,
                        ),
                        radius = 950f,
                    )
                )
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) { content() }
    }
}

@Composable
private fun WelcomeScreen(darkTheme: Boolean, onGetStarted: () -> Unit) {
    OnboardingBackground(darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.25f))
            Image(
                painter = painterResource(R.drawable.onboarding_character),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2.6f),
            )
            Text(
                text = "Nocturne",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Music Player",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(30.dp))
            Text(
                text = "Your Music.\nYour Moment.",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Listen, organise, and enjoy your favourite tracks — anytime, anywhere.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.weight(0.45f))
            OnboardingButton(
                text = "Get Started",
                onClick = onGetStarted,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsScreen(
    darkTheme: Boolean,
    onBack: () -> Unit,
    onComplete: suspend (OnboardingProfile) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var dateOfBirth by remember { mutableStateOf<Long?>(null) }
    var attempted by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMMM yyyy") }
    val formattedDate = dateOfBirth?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(formatter)
    }

    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = dateOfBirth,
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis < System.currentTimeMillis()
                override fun isSelectableYear(year: Int): Boolean = year <= LocalDate.now().year
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateOfBirth = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) { DatePicker(state = datePickerState) }
    }

    OnboardingBackground(darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Image(
                painter = painterResource(R.drawable.onboarding_character),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 30.dp),
            )
            Text(
                text = "Enter Your Details",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(
                text = "Tell us a bit about yourself to\npersonalize your experience.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 28.dp),
            )

            Text("Your Name", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Enter your name") },
                leadingIcon = { Icon(painterResource(R.drawable.person), null) },
                singleLine = true,
                isError = attempted && name.isBlank(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            Text("Gender", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf("Male", "Female", "Other").forEach { option ->
                    GenderChoice(
                        label = option,
                        selected = gender == option,
                        onClick = { gender = option },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (attempted && gender == null) {
                Text(
                    "Select a gender",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Text("Date of Birth", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = CardDefaults.outlinedCardBorder(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painterResource(R.drawable.calendar_today),
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = formattedDate ?: "Select your date of birth",
                        color = if (formattedDate == null) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .weight(1f),
                    )
                    Icon(
                        painterResource(R.drawable.expand_more),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (attempted && dateOfBirth == null) {
                Text(
                    "Select a valid date",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Spacer(Modifier.height(30.dp))
            OnboardingButton(
                text = if (saving) "Saving…" else "Continue",
                enabled = !saving,
                onClick = {
                    attempted = true
                    val dob = dateOfBirth
                    val selectedGender = gender
                    if (name.isNotBlank() && selectedGender != null && dob != null) {
                        saving = true
                        scope.launch {
                            onComplete(OnboardingProfile(name.trim(), selectedGender, dob))
                            saving = false
                        }
                    }
                },
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun GenderChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120), label = "gender-scale")
    Card(
        modifier = modifier
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = CardDefaults.outlinedCardBorder(selected),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painterResource(R.drawable.person),
                null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun OnboardingButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(110), label = "cta-scale")
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
