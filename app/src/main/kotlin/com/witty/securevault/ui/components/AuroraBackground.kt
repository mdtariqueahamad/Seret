package com.witty.securevault.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.unit.dp
import com.witty.securevault.R
import com.witty.securevault.ui.theme.DarkBgBase
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun AuroraBackground(isDarkTheme: Boolean, modifier: Modifier = Modifier) {
    if (!isDarkTheme) {
        // Reverted Light Theme: Leafy image
        Image(
            painter = painterResource(id = R.drawable.leaf_shadow),
            contentDescription = "Ambient Background",
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxSize().blur(8.dp)
        )
        return
    }

    // Each dark-mode drift has a new random destination and duration: no repeated loop.
    var driftTarget by remember { mutableStateOf(Offset(0.52f, 0.35f)) }
    var driftDuration by remember { mutableIntStateOf(9000) }
    var waveOrigin by remember { mutableStateOf<Offset?>(null) }
    var waveNonce by remember { mutableIntStateOf(0) }
    val waveProgress = remember { Animatable(0f) }
    val driftX by animateFloatAsState(
        targetValue = driftTarget.x,
        animationSpec = tween(driftDuration, easing = FastOutSlowInEasing),
        label = "auroraDriftX"
    )
    val driftY by animateFloatAsState(
        targetValue = driftTarget.y,
        animationSpec = tween(driftDuration, easing = FastOutSlowInEasing),
        label = "auroraDriftY"
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(4200, 8500))
            driftDuration = Random.nextInt(7000, 12000)
            driftTarget = Offset(Random.nextFloat(), Random.nextFloat())
        }
    }

    LaunchedEffect(waveNonce) {
        if (waveNonce > 0) {
            waveProgress.snapTo(0f)
            waveProgress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            waveOrigin = null
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .blur(20.dp)
            .pointerInput(waveNonce) {
                detectTapGestures { tapOffset ->
                    waveOrigin = tapOffset
                    waveNonce++
                }
            }
    ) {
        val w = size.width
        val h = size.height

        // Dark deep background – use themed color
        drawRect(color = DarkBgBase)

        // Blob 1: Muted teal – subtle ambient wash
        val cx1 = w * driftX
        val cy1 = h * driftY
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF).copy(alpha = 0.18f), Color.Transparent),
                center = Offset(cx1, cy1),
                radius = w * 0.8f
            ),
            radius = w * 0.8f,
            center = Offset(cx1, cy1)
        )

        // Blob 2: Deep indigo – sophisticated depth
        val cx2 = w * (1f - driftY * 0.75f)
        val cy2 = h * (0.25f + driftX * 0.6f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE0E0E0).copy(alpha = 0.20f), Color.Transparent),
                center = Offset(cx2, cy2),
                radius = w * 0.9f
            ),
            radius = w * 0.9f,
            center = Offset(cx2, cy2)
        )

        // Blob 3: Muted plum – warm counterpoint
        val cx3 = w * (0.2f + driftY * 0.65f)
        val cy3 = h * (0.8f - driftX * 0.5f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFCCCCCC).copy(alpha = 0.14f), Color.Transparent),
                center = Offset(cx3, cy3),
                radius = w * 0.7f
            ),
            radius = w * 0.7f,
            center = Offset(cx3, cy3)
        )

        // Blob 4: Warm slate-rose – barely-there warmth for depth
        val cx4 = w * (0.7f - driftX * 0.4f)
        val cy4 = h * (0.3f + driftY * 0.55f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF5F5F5).copy(alpha = 0.10f), Color.Transparent),
                center = Offset(cx4, cy4),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(cx4, cy4)
        )

        // Touch wave – silver/white ripple
        waveOrigin?.let { origin ->
            val maxRadius = maxOf(w, h) * waveProgress.value * 1.5f
            drawCircle(
                color = Color(0xFFE8E8ED).copy(alpha = (1f - waveProgress.value) * 0.28f),
                radius = maxRadius,
                center = origin,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
