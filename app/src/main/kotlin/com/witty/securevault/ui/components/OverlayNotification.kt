package com.witty.securevault.ui.components

import android.media.MediaPlayer
import androidx.compose.ui.platform.LocalContext
import com.witty.securevault.R
import androidx.compose.ui.draw.blur
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

enum class NotificationType {
    SUCCESS, DELETE, EDIT, ERROR
}

data class NotificationState(
    val message: String = "",
    val type: NotificationType = NotificationType.SUCCESS,
    val isVisible: Boolean = false
)

@Composable
fun OverlayNotificationHost(
    state: NotificationState,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    if (state.isVisible) {
        var animateIn by remember { mutableStateOf(false) }
        var animateOut by remember { mutableStateOf(false) }

        LaunchedEffect(state) {
            animateIn = true
            animateOut = false
            try {
                when (state.type) {
                    NotificationType.ERROR -> {
                        val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 100)
                        toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
                        delay(200)
                        toneGen.release()
                    }
                    NotificationType.SUCCESS -> {
                        val player = MediaPlayer.create(context, R.raw.apple_pay_ding)
                        player.setOnCompletionListener { it.release() }
                        player.start()
                    }
                    else -> {
                        val player = MediaPlayer.create(context, R.raw.ios_sent)
                        player.setOnCompletionListener { it.release() }
                        player.start()
                    }
                }
            } catch (e: Exception) {}
        }

        // Determine final animation targets based on animateOut state
        val showContent = animateIn && !animateOut

        val scale by animateFloatAsState(
            targetValue = when {
                animateOut -> 0.8f
                animateIn -> 1f
                else -> 0.7f
            },
            animationSpec = if (animateOut) {
                tween(250, easing = FastOutSlowInEasing)
            } else {
                spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessVeryLow)
            },
            label = "scale"
        )

        val contentAlpha by animateFloatAsState(
            targetValue = if (showContent) 1f else 0f,
            animationSpec = if (animateOut) {
                tween(250, easing = FastOutSlowInEasing)
            } else {
                tween(200)
            },
            label = "alpha"
        )

        Popup(
            alignment = Alignment.TopCenter,
            properties = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 80.dp)
                    .scale(scale)
                    .alpha(contentAlpha)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = if (isDarkTheme) Color(0x60000000) else Color(0x30000000),
                        spotColor = if (isDarkTheme) Color(0x60000000) else Color(0x30000000)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    
                    .background(if (isDarkTheme) Color(0x991C1C1E) else Color(0xAAFFFFFF))
                    .border(0.5.dp, Color.White.copy(alpha = if (isDarkTheme) 0.15f else 0.4f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(14.dp)
                ) {
                    AnimatedPremiumIcon(type = state.type, isDarkTheme = isDarkTheme)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.message,
                        color = if (isDarkTheme) Color.White else Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedPremiumIcon(type: NotificationType, isDarkTheme: Boolean) {
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        startAnim = true
    }

    val iconScale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessVeryLow),
        label = "iconScale"
    )

    val iconAlpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(300),
        label = "iconAlpha"
    )

    val color = if (isDarkTheme) Color.White else Color(0xFF1C1C1E)

    Canvas(
        modifier = Modifier
            .size(48.dp)
            .scale(iconScale)
            .alpha(iconAlpha)
    ) {
        val w = size.width
        val h = size.height
        val thin = 1.3.dp.toPx()
        val stroke = Stroke(width = thin, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (type) {
            NotificationType.SUCCESS -> {
                // Slim document-into-folder icon
                // Folder body (open top, rounded bottom)
                val folderPath = Path().apply {
                    // Folder base
                    moveTo(w * 0.12f, h * 0.38f)
                    lineTo(w * 0.12f, h * 0.82f)
                    lineTo(w * 0.72f, h * 0.82f)
                    lineTo(w * 0.72f, h * 0.38f)
                    // Folder tab
                    lineTo(w * 0.42f, h * 0.38f)
                    lineTo(w * 0.36f, h * 0.30f)
                    lineTo(w * 0.12f, h * 0.30f)
                    close()
                }
                drawPath(path = folderPath, color = color, style = stroke)

                // Document sliding in from top-right (offset slightly)
                val docPath = Path().apply {
                    moveTo(w * 0.50f, h * 0.14f)
                    lineTo(w * 0.50f, h * 0.62f)
                    lineTo(w * 0.88f, h * 0.62f)
                    lineTo(w * 0.88f, h * 0.26f)
                    // Dog-ear fold
                    lineTo(w * 0.76f, h * 0.14f)
                    close()
                }
                drawPath(path = docPath, color = color, style = stroke)

                // Dog-ear fold line
                drawLine(
                    color = color,
                    start = Offset(w * 0.76f, h * 0.14f),
                    end = Offset(w * 0.76f, h * 0.26f),
                    strokeWidth = thin,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.76f, h * 0.26f),
                    end = Offset(w * 0.88f, h * 0.26f),
                    strokeWidth = thin,
                    cap = StrokeCap.Round
                )

                // Tiny lines on document (text)
                drawLine(color = color.copy(alpha = 0.5f), start = Offset(w * 0.56f, h * 0.36f), end = Offset(w * 0.80f, h * 0.36f), strokeWidth = thin * 0.7f, cap = StrokeCap.Round)
                drawLine(color = color.copy(alpha = 0.5f), start = Offset(w * 0.56f, h * 0.44f), end = Offset(w * 0.74f, h * 0.44f), strokeWidth = thin * 0.7f, cap = StrokeCap.Round)
                drawLine(color = color.copy(alpha = 0.5f), start = Offset(w * 0.56f, h * 0.52f), end = Offset(w * 0.70f, h * 0.52f), strokeWidth = thin * 0.7f, cap = StrokeCap.Round)
            }

            NotificationType.EDIT -> {
                // Slim pencil icon
                val tipX = w * 0.20f
                val tipY = h * 0.80f
                val topLX = w * 0.62f
                val topLY = h * 0.16f
                val topRX = w * 0.78f
                val topRY = h * 0.32f
                val botLX = w * 0.36f
                val botLY = h * 0.64f

                // Pencil body (parallelogram)
                val pencilPath = Path().apply {
                    moveTo(topLX, topLY)
                    lineTo(topRX, topRY)
                    lineTo(botLX, botLY)
                    lineTo(tipX, tipY)
                    close()
                }
                drawPath(path = pencilPath, color = color, style = stroke)

                // Eraser cap line
                drawLine(color = color, start = Offset(w * 0.66f, h * 0.20f), end = Offset(w * 0.82f, h * 0.36f), strokeWidth = thin, cap = StrokeCap.Round)

                // Tip triangle
                drawLine(color = color, start = Offset(tipX, tipY), end = Offset(w * 0.16f, h * 0.88f), strokeWidth = thin, cap = StrokeCap.Round)

                // Writing line underneath
                drawLine(color = color.copy(alpha = 0.4f), start = Offset(w * 0.22f, h * 0.90f), end = Offset(w * 0.80f, h * 0.90f), strokeWidth = thin * 0.7f, cap = StrokeCap.Round)
            }

            NotificationType.DELETE -> {
                // Slim trash can outline
                // Lid
                drawLine(color = color, start = Offset(w * 0.18f, h * 0.24f), end = Offset(w * 0.82f, h * 0.24f), strokeWidth = thin, cap = StrokeCap.Round)

                // Handle on lid
                drawLine(color = color, start = Offset(w * 0.38f, h * 0.24f), end = Offset(w * 0.38f, h * 0.16f), strokeWidth = thin, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.38f, h * 0.16f), end = Offset(w * 0.62f, h * 0.16f), strokeWidth = thin, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.62f, h * 0.16f), end = Offset(w * 0.62f, h * 0.24f), strokeWidth = thin, cap = StrokeCap.Round)

                // Body (tapered)
                val bodyPath = Path().apply {
                    moveTo(w * 0.22f, h * 0.24f)
                    lineTo(w * 0.28f, h * 0.86f)
                    lineTo(w * 0.72f, h * 0.86f)
                    lineTo(w * 0.78f, h * 0.24f)
                }
                drawPath(path = bodyPath, color = color, style = stroke)

                // Vertical lines inside (3 ribs)
                drawLine(color = color.copy(alpha = 0.5f), start = Offset(w * 0.40f, h * 0.34f), end = Offset(w * 0.40f, h * 0.76f), strokeWidth = thin * 0.7f, cap = StrokeCap.Round)
                drawLine(color = color.copy(alpha = 0.5f), start = Offset(w * 0.50f, h * 0.34f), end = Offset(w * 0.50f, h * 0.76f), strokeWidth = thin * 0.7f, cap = StrokeCap.Round)
                drawLine(color = color.copy(alpha = 0.5f), start = Offset(w * 0.60f, h * 0.34f), end = Offset(w * 0.60f, h * 0.76f), strokeWidth = thin * 0.7f, cap = StrokeCap.Round)
            }

            NotificationType.ERROR -> {
                // Slim X mark
                drawLine(color = color, start = Offset(w * 0.25f, h * 0.25f), end = Offset(w * 0.75f, h * 0.75f), strokeWidth = thin, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.75f, h * 0.25f), end = Offset(w * 0.25f, h * 0.75f), strokeWidth = thin, cap = StrokeCap.Round)
            }
        }
    }
}
