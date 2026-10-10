package de.ricci.garminsleep

import android.content.Context

/**
 * Preview-only entitlement policy. Never trust an editable local "premium=true" flag.
 * Replace hasPremium with verified Play Billing purchase entitlement before release.
 */
object WallpaperPremiumGate {
    const val FREE_STYLE = "aurora_dream"
    const val PREMIUM_PRICE_LABEL = "9,99 €"
    private const val DEV_PREFS = "sleepsync_developer_preview"
    private const val DEV_KEY = "live_worlds_enabled"

    /** Temporary internal testing access, NOT a purchase entitlement. Remove for public release. */
    fun isDeveloperPreview(context: Context): Boolean =
        context.getSharedPreferences(DEV_PREFS, Context.MODE_PRIVATE).getBoolean(DEV_KEY, false)

    fun setDeveloperPreview(context: Context, enabled: Boolean) {
        context.getSharedPreferences(DEV_PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(DEV_KEY, enabled).apply()
    }

    fun hasPremium(context: Context): Boolean = isDeveloperPreview(context) // No billing yet.
    fun isAllowed(context: Context, style: String): Boolean =
        style == FREE_STYLE || hasPremium(context)
    fun safeStyle(context: Context, style: String): String =
        if (isAllowed(context, style)) style else FREE_STYLE
}
