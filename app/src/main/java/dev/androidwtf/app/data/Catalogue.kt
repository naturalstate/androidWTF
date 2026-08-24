package dev.androidwtf.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The catalogue, as produced by `wtf catalogue --json` and bundled into the APK
 * at build time.
 *
 * Bundling rather than fetching means browsing, filtering and pack-building all
 * work before Termux is installed or configured, and with no network. The engine
 * is only needed to actually install something.
 */
@Serializable
data class Tool(
    val id: String,
    val name: String,
    val desc: String,
    val tier: Int,
    val bundle: String,
    val provider: String,
    val flags: List<String> = emptyList(),
    val license: String = "free",
    val notes: String = "",
    val upstream: String = "",
) {
    val essential get() = "essential" in flags
    /** Whether the engine can install this unattended, or it needs a human tap. */
    val scriptable get() = provider in setOf("pkg", "pip", "go", "git")
}

@Serializable
data class Bundle(val name: String, val description: String, val count: Int)

@Serializable
data class Profile(
    val name: String,
    val description: String,
    val requiresTier: Int,
    val order: Int = 99,
)

@Serializable
data class Catalogue(
    val version: String = "",
    val bundles: List<Bundle> = emptyList(),
    val profiles: List<Profile> = emptyList(),
    val tools: List<Tool> = emptyList(),
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun load(ctx: Context): Catalogue =
            ctx.assets.open("catalogue.json").bufferedReader().use {
                json.decodeFromString(it.readText())
            }
    }
}

/** Tier metadata, kept beside the catalogue so the UI never hardcodes it. */
data class TierInfo(val n: Int, val label: String, val needs: String)

val TIERS = listOf(
    TierInfo(0, "Stock", "Any Android 7+ device"),
    TierInfo(1, "Shizuku", "ADB pairing over wireless debugging"),
    TierInfo(2, "Root", "Magisk or KernelSU"),
    TierInfo(3, "NetHunter", "NetHunter kernel"),
)
