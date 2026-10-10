package de.ricci.garminsleep

import android.content.Context

/**
 * Preview-only entitlement policy. Never trust an editable local "premium=true" flag.
 * Replace hasPremium with verified Play Billing purchase entitlement before release.
 */
object WallpaperPremiumGate {
    const val FREE_STYLE = "aurora_dream"
    const val PREMIUM_PRICE_LABEL = "9,99 €"
    fun hasPremium(context: Context): Boolean = false // No checkout or entitlement yet.
    fun isAllowed(context: Context, style: String): Boolean =
        style == FREE_STYLE || hasPremium(context)
    fun safeStyle(context: Context, style: String): String =
        if (isAllowed(context, style)) style else FREE_STYLE
}
