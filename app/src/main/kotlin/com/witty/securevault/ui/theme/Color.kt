package com.witty.securevault.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Colors — Apple frosted glass with improved legibility
val LightBgBase = Color(0xFFF2F2F7)           // Apple's actual light bg (iOS system background)
val LightBgCard = Color(0xB3FFFFFF)            // 70% white – frosted glass with stronger readability
val LightBgInput = Color(0x80FFFFFF)           // 50% white – visible yet translucent
val LightGlassBorder = Color(0x30C7C7CC)       // warm neutral border, subtle
val LightTextPrimary = Color(0xFF000000)       // iOS standard pure black text
val LightAccentBlue = Color(0xFF000000)        // iOS system blue
val LightError = Color(0xFFFF3B30)             // iOS system red
val LightPlaceholder = Color(0xFF3C3C43).copy(alpha = 0.36f)
val LightSubtleGlow = Color(0x0D000000)        // faint shadow glow

// Dark Theme Colors — Premium aurora glass
val DarkBgBase = Color(0xFF000000)             // Apple's actual dark bg (pure black for OLED)
val DarkBgCard = Color(0x3D1C1C1E)             // 24% alpha dark gray – slightly more legible glass
val DarkBgInput = Color(0x4D2C2C2E)            // 30% alpha – readable input surface
val DarkGlassBorder = Color(0x28FFFFFF)        // 16% white border – refined, not neon
val DarkTextPrimary = Color(0xFFFFFFFF)        // Pure white
val DarkAccentBlue = Color(0xFFFFFFFF)         // iOS dark mode system blue
val DarkError = Color(0xFFFF453A)              // iOS dark mode system red
val DarkPlaceholder = Color(0xFFEBEBF5).copy(alpha = 0.36f)
val DarkSubtleGlow = Color(0x0AFFFFFF)         // faint blue ambient glow
