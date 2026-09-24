package com.airobot.framework.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Robot Fixed Colors — Theme-independent, always the same
// ============================================================

/** Robot head shell — sky-200 */
val RobotHeadColor = Color(0xFFBAE6FD)

/** Robot inner face — sky-100 (Legacy) */
val RobotFaceColorLegacy = Color(0xFFE0F2FE)

/** Aether Robot Shell (Transitional Layer) */
val AetherShellStart = Color(0xFFFFEDD5) // orange-100
val AetherShellVia = Color(0xFFFFF7ED)   // orange-50
val AetherShellEnd = Color(0xFFFFF1F2)   // rose-50

/** Aether Robot Body (Inner Core) */
val AetherBodyStart = Color(0xFFFFFFFF)
val AetherBodyEnd = Color(0xFFFFF7ED)   // orange-50
val AetherBodyVia = Color(0xFFFFFBEB)   // amber-50
val AetherEarInner = Color(0xFFFFF1F2)  // rose-50
val AetherShadowColor = Color(0xFFFB923C).copy(alpha = 0.25f) 
val AetherAuraCyan = Color(0xFF22D3EE).copy(alpha = 0.15f)
val AetherAuraRose = Color(0xFFF472B6).copy(alpha = 0.15f)
val AetherHandBg = Color(0xFFFFFFFF)
val AetherHandBorder = Color(0xFFFFF1F2) // rose-50
val AetherMouthReady = Color(0xFF334155).copy(alpha = 0.6f)
val AetherMouthTalking = Color(0xFFF9A8D4) // pink-300
val AetherBlushColor = Color(0xFFF472B6)

/** Robot head border — sky-400/60% */
val RobotHeadBorder = Color(0xFF38BDF8).copy(alpha = 0.6f)

/** Robot ear border — sky-400/50% */
val RobotEarBorder = Color(0xFF38BDF8).copy(alpha = 0.5f)

/** Robot face inner border — sky-300/50% */
val RobotFaceBorder = Color(0xFF7DD3FC).copy(alpha = 0.5f)

/** Robot blush — rose-300 */
val RobotBlush = Color(0xFFFDA4AF)

/** Robot eye default — slate-800 */
val RobotEyeDefault = Color(0xFF1E293B)

/** Robot eye active — orange-500 */
val RobotEyeActive = Color(0xFFF97316)

/** Robot eye focus — emerald-500 */
val RobotEyeFocus = Color(0xFF10B981)

/** Robot antenna default — orange-500 */
val RobotAntennaDefault = Color(0xFFF97316)

/** Robot antenna focus — emerald-400 */
val RobotAntennaFocus = Color(0xFF34D399)

/** Robot antenna stalk gradient — slate-300 to slate-400 */
val RobotAntennaStemLight = Color(0xFFCBD5E1)
val RobotAntennaStemDark = Color(0xFF94A3B8)

/** Robot neck — slate-600 */
val RobotNeckColor = Color(0xFF475569)

/** Robot collar — sky-200 (same as head) */
val RobotCollarColor = RobotHeadColor

// ============================================================
// Dark Theme Palette — slate-900 based
// ============================================================
val DarkBackground = Color(0xFF0F172A)            // slate-900
val DarkBackgroundEnd = Color(0xFF020617)          // slate-950
val DarkBackgroundShapes = Color(0xFF1E293B)       // slate-800/40%
val DarkTextPrimary = Color(0xFFE2E8F0)            // slate-200
val DarkTextSecondary = Color(0xFFCBD5E1)          // slate-300
val DarkTextMuted = Color(0xFF64748B)              // slate-500
val DarkAccent = Color(0xFF818CF8)                 // indigo-400
val DarkAccentBg = Color(0xFF6366F1)               // indigo-500
val DarkCardBg = Color(0xFF1E293B)                 // slate-800/90%
val DarkCardBorder = Color(0xFF334155)             // slate-700
val DarkBubbleBg = Color(0xFF1E293B)               // slate-800/40%
val DarkBubbleBorder = Color(0xFF334155)           // slate-700/50%
val DarkSurfaceOverlay = Color(0xFFFFFFFF)         // white — used at low alpha
val DarkRobotAuraStart = Color(0xFF0EA5E9)         // sky-500/40%
val DarkRobotAuraEnd = Color(0xFF3B82F6)           // blue-500/40%
val DarkTimerBg = Color(0xFF1E293B)                // slate-800
val DarkTimerRingBg = Color(0xFF334155)            // slate-700
val DarkTimerRingActive = Color(0xFF818CF8)        // indigo-400
val DarkTimerRingPaused = Color(0xFF475569)        // slate-600

// ============================================================
// Light Theme Palette — slate-200 based
// ============================================================
val LightBackground = Color(0xFFF3F7FA)            // 极浅蓝灰色，匹配 Image 3
val LightBackgroundEnd = Color(0xFFE6EEF5)         // 浅蓝灰
val LightBackgroundShapes = Color(0xFFCBD5E1)      // slate-300/40%
val LightTextPrimary = Color(0xFF334155)            // slate-700
val LightTextSecondary = Color(0xFF475569)          // slate-600
val LightTextMuted = Color(0xFF94A3B8)              // slate-400
val LightAccent = Color(0xFFF97316)                 // orange-500
val LightAccentBg = Color(0xFFF97316)               // orange-500
val LightCardBg = Color(0xFFFFFFFF)                 // white/90%
val LightCardBorder = Color(0xFFE2E8F0)             // slate-200
val LightBubbleBg = Color(0xFFFFFFFF)               // white/40%
val LightBubbleBorder = Color(0xFFFFFFFF)           // white/50%
val LightSurfaceOverlay = Color(0xFF000000)         // black — used at low alpha
val LightRobotAuraStart = Color(0xFFBAE6FD)         // 浅蓝色 (sky-200), 与机器人本体接近，更融合
val LightRobotAuraEnd = Color(0xFFE0F2FE)           // 极浅蓝 (sky-100)
val LightTimerBg = Color(0xFFFFFFFF)                // white
val LightTimerRingBg = Color(0xFFF1F5F9)            // slate-100
val LightTimerRingActive = Color(0xFFFB923C)        // orange-400
val LightTimerRingPaused = Color(0xFFCBD5E1)        // slate-300

// ============================================================
// Semantic Colors — shared across themes
// ============================================================
val StatusRed = Color(0xFFEF4444)                   // red-500
val StatusAmber = Color(0xFFFBBF24)                 // amber-400
val StatusEmerald = Color(0xFF34D399)               // emerald-400
val StatusCyan = Color(0xFF22D3EE)                  // cyan-400

// ============================================================
// Functional Module Accent Colors
// ============================================================

// Clock module
val ClockHandHour = Color(0xFF94A3B8)               // slate-400
val ClockHandMinute = Color(0xFF64748B)             // slate-500
val ClockHandSecond = Color(0xFFF97316)             // orange-500 (accent)
val ClockCenterPin = Color(0xFFEA580C)              // orange-600
val ClockMarkerColor = Color(0xFF334155)            // slate-700
val ClockDigitalColor = Color(0xFFF97316)           // orange-500 (80% alpha)

// Schedule module
val ScheduleDateBg = Color(0xFFF97316)              // orange-500 (date card)
val ScheduleDateBgLight = Color(0xFFFB923C)         // orange-400
val ScheduleChipBg = Color(0xFF334155)              // slate-700 (dark)
val ScheduleChipBgLight = Color(0xFFF1F5F9)         // slate-100 (light)

// Podcast module
val PodcastFeaturedStart = Color(0xFF818CF8)         // indigo-400
val PodcastFeaturedEnd = Color(0xFFA78BFA)           // violet-400
val PodcastFeaturedBg = Color(0xFF6366F1)            // indigo-500

// SkeuomorphicDial layers
val DialTrackDark = Color(0xFF1E293B)               // slate-800/40%
val DialTrackLight = Color(0xFFFFFFFF)              // white/40%
val DialBaseDark = Color(0xFF1E293B)                // slate-800
val DialBaseLight = Color(0xFFE2E8F0)               // slate-200
val DialKnobDark = Color(0xFF334155)                // slate-700
val DialKnobLight = Color(0xFFF1F5F9)               // slate-100
val DialKnobCenterDark = Color(0xFF475569)          // slate-600
val DialKnobCenterLight = Color(0xFFF8FAFC)         // slate-50
val DialIndicatorOrange = Color(0xFFF97316)         // orange-500 (main category)
val DialIndicatorCyan = Color(0xFF22D3EE)           // cyan-400 (sub-category)

// Aether Prompt Strip
val AetherPromptBgDark = Color(0xFF312E81)          // indigo-900/20%
val AetherPromptBorderDark = Color(0xFF6366F1)      // indigo-500/30%
val AetherPromptBgLight = Color(0xFFEEF2FF)         // indigo-50/80%
val AetherPromptBorderLight = Color(0xFFC7D2FE)     // indigo-200

// Legacy aliases (kept for backward compatibility during migration)
val RobotBackgroundDark = DarkBackground
val RobotPrimaryCyan = Color(0xFF22D3EE)
val RobotSecondaryIndigo = Color(0xFF6366F1)
val RobotTextPrimary = Color(0xFFFFFFFF)
val RobotTextSecondary = Color(0xFF94A3B8)
val RobotSurface = Color(0xFF1E293B)
val ConnectionRed = Color(0xFFE53935)
val ConnectedGreen = Color(0xFF00FF04)

// ============================================================
// Overlay Chassis & Skeuomorphic Surface Colors
// ============================================================
val OverlayChassisButtonDark = Color(0xFF1E293B)
val OverlayChassisButtonLight = Color(0xFFF1F5F9)
val OverlayDialFaceDark = Color(0xFF090D16)
val OverlayDialFaceLight = Color(0xFFFFFFFF)

val SkeuoMetalDarkStart = Color(0xFF64748B)
val SkeuoMetalDarkEnd = Color(0xFF334155)
val SkeuoMetalLightStart = Color(0xFFE2E8F0)
val SkeuoMetalLightEnd = Color(0xFF94A3B8)

val LogbookPaperDark = Color(0xFF1E1E1E)
val LogbookPaperLight = Color(0xFFFDFBF7)
val LogbookBorderDark = Color(0xFF2D2D2D)
val LogbookBorderLight = Color(0xFF8B5A2B)

