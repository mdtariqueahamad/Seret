package com.witty.securevault

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.witty.securevault.backend.PythonBridge
import com.witty.securevault.ui.screens.DashboardScreen
import com.witty.securevault.ui.screens.LoginScreen
import com.witty.securevault.ui.theme.SecureVaultTheme
import com.witty.securevault.viewmodel.AppState
import com.witty.securevault.viewmodel.VaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var initError: String? = null
        try {
            com.witty.securevault.ui.theme.ThemeManager.init(applicationContext)
            // Initialize Native Python Engine
            PythonBridge.init(applicationContext)
        } catch (e: Exception) {
            initError = e.message
            Log.e("SERET", "Failed to init python: ${e.message}", e)
        }

        setContent {
            SecureVaultTheme {
                if (initError != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Fatal Error: Python Engine Failed to start.\n$initError", color = Color.Red)
                    }
                    return@SecureVaultTheme
                }

                val viewModel: VaultViewModel = viewModel()
                
                // Check if this is a first-time launch (empty vault) — only once
                LaunchedEffect(Unit) {
                    viewModel.loadInitialState()
                }
                
                val currentScreen = viewModel.appState.collectAsState().value
                var displayedScreen by remember { mutableStateOf(currentScreen) }

                LaunchedEffect(currentScreen) {
                    if ((displayedScreen == AppState.LOGIN || displayedScreen == AppState.SETUP) && currentScreen == AppState.DASHBOARD) {
                        // Delay swapping the screen so the success animation can play in LoginScreen
                        kotlinx.coroutines.delay(1100)
                    }
                    displayedScreen = currentScreen
                }
                
                AnimatedContent(
                    targetState = displayedScreen,
                    transitionSpec = {
                        val duration = 1000
                        if (targetState == AppState.DASHBOARD) {
                            fadeIn(tween(duration)) togetherWith fadeOut(tween(duration / 2))
                        } else {
                            fadeIn(tween(duration / 2)) togetherWith fadeOut(tween(duration))
                        }
                    },
                    label = "CircularRevealTransition"
                ) { screen ->
                    // To do a true circular reveal from the center, we animate a float progress
                    val transition = updateTransition(targetState = screen == displayedScreen, label = "circle")
                    val progress by transition.animateFloat(
                        transitionSpec = { tween(1000, easing = FastOutSlowInEasing) },
                        label = "progress"
                    ) { if (it) 1f else 0f }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircularRevealShape(progress))
                    ) {
                        when (screen) {
                            AppState.LOGIN -> LoginScreen(viewModel)
                            AppState.SETUP -> LoginScreen(viewModel)
                            AppState.DASHBOARD -> DashboardScreen(viewModel)
                            else -> LoginScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

class CircularRevealShape(private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val maxRadius = kotlin.math.hypot(size.width / 2.0, size.height / 2.0).toFloat()
        val currentRadius = maxRadius * progress
        return Outline.Generic(androidx.compose.ui.graphics.Path().apply {
            addOval(Rect(
                center = Offset(size.width / 2f, size.height / 2f),
                radius = currentRadius
            ))
        })
    }
}
