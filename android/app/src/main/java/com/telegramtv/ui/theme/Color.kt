package com.telegramtv.ui.theme

import androidx.compose.ui.graphics.Color

// Primary colors (legacy compat)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ── Modern TV Color System ──────────────────────────────────────────
// Matches the web app's Tailwind palette (web/tailwind.config.js) —
// primary = purple scale, dark = near-black slate scale — so the TV app
// reads as the same product instead of a differently-branded blue/cyan skin.

// Backgrounds (web: dark-950 / dark-900 / dark-800)
val TVBackground = Color(0xFF020617)
val TVSurface = Color(0xFF0F172A)
val TVSurfaceVariant = Color(0xFF1E293B)

// Primary accent — purple (web: primary-500 / primary-700 / primary-300)
val TVPrimary = Color(0xFFA855F7)
val TVPrimaryVariant = Color(0xFF7C3AED)
val TVPrimaryLight = Color(0xFFD8B4FE)

// Secondary accent — deeper purple (web: primary-600 / primary-800)
val TVSecondary = Color(0xFF9333EA)
val TVSecondaryVariant = Color(0xFF6B21A8)

// Cards (web: dark-800 / a lighter dark-800 for focus/hover)
val TVCardBackground = Color(0xFF1E293B)
val TVCardFocused = Color(0xFF334155)
val TVCardSelected = Color(0xFF475569)

// Text
val TVTextPrimary = Color(0xFFF1F5F9)
val TVTextSecondary = Color(0xFF94A3B8)
val TVTextDisabled = Color(0xFF64748B)

// Progress
val TVProgressBar = Color(0xFFA855F7)
val TVProgressBackground = Color(0xFF334155)

// Overlays
val TVOverlayDark = Color(0xCC000000)
val TVOverlayLight = Color(0x22FFFFFF)

// Status
val TVError = Color(0xFFFF5252)
val TVSuccess = Color(0xFF69F0AE)
val TVWarning = Color(0xFFFFD740)

// Gradients (web: primary-400 -> primary-600)
val TVGradientStart = Color(0xFFC084FC)
val TVGradientEnd = Color(0xFF9333EA)

// Focus glow
val TVAccentGlow = Color(0x44A855F7)
val TVFocusRing = Color(0xFFD8B4FE)

// Download states
val TVDownloadProgress = Color(0xFFA855F7)
val TVDownloadComplete = Color(0xFF69F0AE)

// ── Mobile Modern Color System ──────────────────────────────────────
// Same web palette as the TV system above, kept as separate constants
// since the mobile theme has its own surface/background shades.

// Backgrounds (web: dark-950 / dark-900)
val MobileBackground = Color(0xFF020617)
val MobileSurface = Color(0xFF0F172A)
val MobileSurfaceTransparent = Color(0xCC0F172A)

// Accents (web: primary-500 / primary-600 / primary-400)
val MobilePrimary = Color(0xFFA855F7)
val MobileSecondary = Color(0xFF9333EA)
val MobileAccent = Color(0xFFC084FC)

// Gradients (web: primary-500 -> dark-800)
val MobileGradientStart = Color(0xFFA855F7)
val MobileGradientEnd = Color(0xFF1E293B)
val MobileHeaderGradientStart = Color(0xFF1E293B)
val MobileHeaderGradientEnd = Color(0xFF020617)

// Text
val MobileTextPrimary = Color(0xFFFFFFFF)
val MobileTextSecondary = Color(0xFFB3B3B3)

// Glassmorphism
val GlassCorrectionColor = Color(0xFF1E1E1E) // Fallback or tint
