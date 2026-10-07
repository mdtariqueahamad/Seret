package com.witty.securevault.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

enum class AuthState {
    IDLE,
    AUTHENTICATING,
    SUCCESS,
    ERROR
}

@Composable
fun AnimatedLockLogo(
    authState: AuthState,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    val showResult = authState == AuthState.SUCCESS || authState == AuthState.ERROR

    // White/silver liquid glassy shimmer for dark mode;
    // dark charcoal/graphite shimmer for light mode
    val normalGradient = if (isDarkTheme) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF),  // pure white
                Color(0xFFD1D5DB),  // silver-200
                Color(0xFFFFFFFF),  // white highlight (shimmer peak)
                Color(0xFFB0B8C4)   // cool silver
            ),
            start = Offset(0f, 0f),
            end = Offset(100f, 100f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1F2937),  // charcoal-800
                Color(0xFF374151),  // gray-700
                Color(0xFF1F2937),  // charcoal repeat (shimmer)
                Color(0xFF4B5563)   // gray-600
            ),
            start = Offset(0f, 0f),
            end = Offset(100f, 100f)
        )
    }

    val successColor = Color(0xFF34C759)
    val errorColor = Color(0xFFFF3B30)

    val infiniteTransition = rememberInfiniteTransition(label = "authTransition")

    val dashOffset by infiniteTransition.animateFloat(
        initialValue = 200f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "dash"
    )

    val innerDashOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "innerDash"
    )

    val nodeScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "nodeScale"
    )

    val lockAlpha by animateFloatAsState(
        targetValue = if (showResult) 0f else 1f,
        animationSpec = tween(170, delayMillis = if (showResult) 0 else 350, easing = FastOutSlowInEasing),
        label = "lockAlpha"
    )
    // The original logo clears while its own inner-hex lines morph in place.
    val morphProgress by animateFloatAsState(
        targetValue = if (showResult) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "logoLineMorph"
    )

    Canvas(modifier = modifier.size(110.dp).padding(8.dp)) {
        val scaleX = size.width / 100f
        val scaleY = size.height / 100f

        // Thinner stroke — slim/patli wireframe
        val strokeWidthPx = 3f * scaleX

        if (lockAlpha > 0.01f) {

                // SHACKLE
                val shacklePath = Path().apply {
                    moveTo(32f * scaleX, 30f * scaleY)
                    lineTo(32f * scaleX, 20f * scaleY)
                    arcTo(
                        rect = Rect(32f * scaleX, 2f * scaleY, 68f * scaleX, 38f * scaleY),
                        startAngleDegrees = 180f, sweepAngleDegrees = 180f, forceMoveTo = false
                    )
                    lineTo(68f * scaleX, 30f * scaleY)
                }

                // OUTER HEX
                val outerHexPath = Path().apply {
                    moveTo(50f * scaleX, 14f * scaleY)
                    lineTo(81.17f * scaleX, 32f * scaleY)
                    lineTo(81.17f * scaleX, 68f * scaleY)
                    lineTo(50f * scaleX, 86f * scaleY)
                    lineTo(18.83f * scaleX, 68f * scaleY)
                    lineTo(18.83f * scaleX, 32f * scaleY)
                    close()
                }

                // INNER HEX
                val innerHexPath = Path().apply {
                    moveTo(50f * scaleX, 28f * scaleY)
                    lineTo(69.05f * scaleX, 39f * scaleY)
                    lineTo(69.05f * scaleX, 61f * scaleY)
                    lineTo(50f * scaleX, 72f * scaleY)
                    lineTo(30.95f * scaleX, 61f * scaleY)
                    lineTo(30.95f * scaleX, 39f * scaleY)
                    close()
                }

                val outerPathEffect = if (authState == AuthState.AUTHENTICATING) {
                    PathEffect.dashPathEffect(floatArrayOf(40f * scaleX, 40f * scaleX), dashOffset * scaleX)
                } else null

                val innerPathEffect = if (authState == AuthState.AUTHENTICATING) {
                    PathEffect.dashPathEffect(floatArrayOf(20f * scaleX, 20f * scaleX), innerDashOffset * scaleX)
                } else null

            drawPath(path = shacklePath, brush = normalGradient, alpha = lockAlpha, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(path = outerHexPath, brush = normalGradient, alpha = lockAlpha, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = outerPathEffect))
            drawPath(path = innerHexPath, brush = normalGradient, alpha = lockAlpha, style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = innerPathEffect))

            // Node circles — more subtle (2f radius instead of 3f)
            val nodes = listOf(Offset(50f, 14f), Offset(81.17f, 68f), Offset(18.83f, 32f), Offset(30.95f, 61f), Offset(69.05f, 39f))
            val activeNodeScale = if (authState == AuthState.AUTHENTICATING) nodeScale else 1f
            nodes.forEach { point ->
                drawCircle(
                    brush = normalGradient,
                    radius = 2f * scaleX * activeNodeScale,
                    center = Offset(point.x * scaleX, point.y * scaleY),
                    alpha = lockAlpha
                )
            }

                // KEYHOLE (matching SVG: M 50 43 A 3.5 3.5 0 1 0 47.4 48.3 L 45.5 57 H 54.5 L 52.6 48.3 A 3.5 3.5 0 1 0 50 43 Z)
                val keyholePath = PathParser().parsePathString("M 50 43 A 3.5 3.5 0 1 0 47.4 48.3 L 45.5 57 H 54.5 L 52.6 48.3 A 3.5 3.5 0 1 0 50 43 Z").toPath()
                val scaledKeyhole = Path().apply {
                    addPath(keyholePath)
                    transform(Matrix().apply { scale(scaleX, scaleY) })
                }

            drawPath(path = scaledKeyhole, brush = normalGradient, alpha = lockAlpha) // It's filled in SVG
        }

        // The confirmation still grows from the logo's own inner-hex segments. Its
        // perimeter is drawn from one circumference point while the mark morphs.
        if (morphProgress > 0.01f) {
            val resultColor = if (authState == AuthState.SUCCESS) successColor else errorColor
            val ringInset = 12f * scaleX
            val slimStroke = strokeWidthPx * 0.5f // slim border for tick/cross

            // --- Phase 1: Ring sweep (morphProgress 0..0.6) ---
            // Map morphProgress 0..0.6 → 0..1 for the ring, with easeInOut curve
            val ringRaw = (morphProgress / 0.6f).coerceIn(0f, 1f)
            // Apply easeInOut (smooth cubic) for fluid acceleration/deceleration
            val ringProgress = if (ringRaw < 0.5f) {
                2f * ringRaw * ringRaw
            } else {
                1f - (-2f * ringRaw + 2f).let { it * it } / 2f
            }

            drawArc(
                color = resultColor,
                startAngle = -90f,
                sweepAngle = 360f * ringProgress,
                useCenter = false,
                topLeft = Offset(ringInset, ringInset),
                size = Size(size.width - ringInset * 2f, size.height - ringInset * 2f),
                style = Stroke(width = slimStroke, cap = StrokeCap.Round)
            )

            // --- Phase 2: Tick/cross mark (morphProgress 0.6..1.0) ---
            if (morphProgress > 0.6f) {
                // Map morphProgress 0.6..1.0 → 0..1 for the mark drawing
                val markProgress = ((morphProgress - 0.6f) / 0.4f).coerceIn(0f, 1f)

                fun point(x: Float, y: Float) = Offset(x * scaleX, y * scaleY)
                fun morph(from: Offset, to: Offset) = Offset(
                    x = from.x + (to.x - from.x) * markProgress,
                    y = from.y + (to.y - from.y) * markProgress
                )
                fun line(fromX: Float, fromY: Float, toX: Float, toY: Float, endX: Float, endY: Float, endEndX: Float, endEndY: Float) {
                    drawLine(
                        color = resultColor,
                        start = morph(point(fromX, fromY), point(toX, toY)),
                        end = morph(point(endX, endY), point(endEndX, endEndY)),
                        strokeWidth = slimStroke,
                        cap = StrokeCap.Round
                    )
                }
                if (authState == AuthState.SUCCESS) {
                    line(30.95f, 61f, 32f, 52f, 50f, 72f, 45f, 65f)
                    line(50f, 72f, 45f, 65f, 69.05f, 39f, 70f, 36f)
                } else {
                    line(30.95f, 39f, 36f, 36f, 69.05f, 61f, 64f, 64f)
                    line(69.05f, 39f, 64f, 36f, 30.95f, 61f, 36f, 64f)
                }
            }
        }
    }
}
