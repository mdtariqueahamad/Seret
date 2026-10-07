package com.witty.securevault.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witty.securevault.ui.components.AnimatedLockLogo
import com.witty.securevault.ui.components.AuthState
import com.witty.securevault.ui.theme.DarkBgInput
import com.witty.securevault.ui.theme.DarkGlassBorder
import com.witty.securevault.ui.theme.DarkPlaceholder
import com.witty.securevault.ui.theme.LightBgInput
import com.witty.securevault.ui.theme.LightGlassBorder
import com.witty.securevault.ui.theme.LightPlaceholder
import com.witty.securevault.ui.theme.OrangeAvenueFont
import com.witty.securevault.ui.theme.ThemeManager
import com.witty.securevault.ui.theme.ThemeMode
import com.witty.securevault.viewmodel.AppState
import com.witty.securevault.viewmodel.VaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: VaultViewModel) {
    val errorMessage = viewModel.errorMessage.collectAsState().value
    val isError = errorMessage.isNotBlank()
    val appState = viewModel.appState.collectAsState().value
    val isFirstTime = appState == AppState.SETUP

    var masterPassword by remember { mutableStateOf("") }
    var masterPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    val themeMode by ThemeManager.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    var isAuthenticating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val authState = when {
        appState == AppState.DASHBOARD -> AuthState.SUCCESS
        isError -> AuthState.ERROR
        isAuthenticating -> AuthState.AUTHENTICATING
        else -> AuthState.IDLE
    }

    // Apple-style entrance animation
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(150)
        contentVisible = true
    }

    val cardAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "cardAlpha"
    )

    LaunchedEffect(isError) {
        if (isError) {
            delay(1500)
            viewModel.resetError()
        }
    }

    // — Liquid glassy brand gradient (white shimmer, NOT blue) —
    val brandGlassGradient = Brush.linearGradient(
        colors = if (isDarkTheme) {
            listOf(
                Color(0xFFFFFFFF),
                Color(0x80FFFFFF),
                Color(0xFFFFFFFF)
            )
        } else {
            listOf(
                Color(0xFF000000),
                Color(0x80000000),
                Color(0xFF000000)
            )
        },
        start = Offset(0f, 0f),
        end = Offset(200f, 60f)
    )

    // Subtitle color — very subtle
    val subtitleColor = if (isDarkTheme) {
        Color(0x80FFFFFF)
    } else {
        Color(0x669E9E9E)
    }

    // Glass card colors
    val cardBackground = if (isDarkTheme) {
        Color(0x1AFFFFFF)
    } else {
        Color(0x80FFFFFF)
    }
    val cardBorder = if (isDarkTheme) DarkGlassBorder else LightGlassBorder

    // Input styling
    val inputBg = if (isDarkTheme) DarkBgInput else LightBgInput
    val placeholderColor = if (isDarkTheme) DarkPlaceholder else LightPlaceholder
    val textColor = MaterialTheme.colorScheme.onBackground

    // Apple Standard: Liquid Glassy Button Background
    val buttonGradient = Brush.horizontalGradient(
        colors = if (isDarkTheme) {
            listOf(Color(0x40FFFFFF), Color(0x1AFFFFFF))
        } else {
            listOf(Color(0x1A000000), Color(0x05000000))
        }
    )

    val pillShape = RoundedCornerShape(100.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Aurora animated background
        com.witty.securevault.ui.components.AuroraBackground(
            isDarkTheme = isDarkTheme,
            modifier = Modifier.fillMaxSize()
        )

        // Glassmorphic Card — Apple-like proportions
        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(
                animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
            ) + slideInVertically(
                initialOffsetY = { it / 12 },
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessVeryLow
                )
            ),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessVeryLow)) + slideOutVertically(
                targetOffsetY = { it / 12 },
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessVeryLow
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(cardBackground)
                    .border(
                        width = 0.5.dp,
                        color = cardBorder,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 28.dp, vertical = 36.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // ─── Logo ───
                    AnimatedLockLogo(
                        authState = authState,
                        modifier = Modifier.padding(bottom = 16.dp),
                        isDarkTheme = isDarkTheme
                    )

                    // ─── Brand: SERET — liquid white glass ───
                    Text(
                        text = "SERET",
                        style = TextStyle(
                            brush = brandGlassGradient,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = OrangeAvenueFont,
                            fontFeatureSettings = "liga, dlig, calt",
                            letterSpacing = 3.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // ─── Subtitle ───
                    Text(
                        text = "Your secure vault",
                        style = TextStyle(
                            color = subtitleColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 1.2.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // ─── Master Password Input ───
                    OutlinedTextField(
                        value = masterPassword,
                        onValueChange = {
                            masterPassword = it
                            if (isError) viewModel.resetError()
                        },
                        visualTransformation = if (masterPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { masterPasswordVisible = !masterPasswordVisible }) {
                                Icon(
                                    imageVector = if (masterPasswordVisible) {
                                        Icons.Outlined.Visibility
                                    } else {
                                        Icons.Outlined.VisibilityOff
                                    },
                                    contentDescription = "Toggle Visibility",
                                    tint = textColor.copy(alpha = 0.5f)
                                )
                            }
                        },
                        singleLine = true,
                        placeholder = {
                            Text(
                                "Enter Master Password",
                                color = placeholderColor,
                                fontSize = 15.sp
                            )
                        },
                        textStyle = TextStyle(fontSize = 15.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(pillShape),
                        isError = isError,
                        shape = pillShape
                    )

                    // ─── Confirm Password (first-time setup only) ───
                    AnimatedVisibility(
                        visible = isFirstTime,
                        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessVeryLow)) + slideInVertically(
                            initialOffsetY = { -it / 4 },
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessVeryLow
                            )
                        ),
                        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessVeryLow)) + slideOutVertically(
                            targetOffsetY = { -it / 4 },
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessVeryLow
                            )
                        )
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    if (isError) viewModel.resetError()
                                },
                                visualTransformation = if (confirmPasswordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        confirmPasswordVisible = !confirmPasswordVisible
                                    }) {
                                        Icon(
                                            imageVector = if (confirmPasswordVisible) {
                                                Icons.Outlined.Visibility
                                            } else {
                                                Icons.Outlined.VisibilityOff
                                            },
                                            contentDescription = "Toggle Visibility",
                                            tint = textColor.copy(alpha = 0.5f)
                                        )
                                    }
                                },
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        "Re-enter Master Password",
                                        color = placeholderColor,
                                        fontSize = 15.sp
                                    )
                                },
                                textStyle = TextStyle(fontSize = 15.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = cardBorder,
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedContainerColor = inputBg,
                                    unfocusedContainerColor = inputBg
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .clip(pillShape),
                                isError = isError,
                                shape = pillShape
                            )
                        }
                    }

                    // ─── Error Message ───
                    AnimatedVisibility(
                        visible = isError,
                        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessVeryLow)) + slideInVertically(
                            initialOffsetY = { -it / 2 },
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessVeryLow
                            )
                        ),
                        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessVeryLow))
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ─── Action Button — gradient pill ───
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isAuthenticating = true
                                delay(600)
                                if (isFirstTime) {
                                    viewModel.setupVault(masterPassword, confirmPassword)
                                } else {
                                    viewModel.attemptLogin(masterPassword)
                                }
                                isAuthenticating = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF000000)
                        ),
                        shape = pillShape,
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(brush = buttonGradient, shape = pillShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFirstTime) "Create Vault" else "Unlock",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
