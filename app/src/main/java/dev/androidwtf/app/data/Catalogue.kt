package dev.androidwtf.app.data

import android.content.Context
import kotlinx.serialization.SerialName
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
    @SerialName("package") val package_: String = "",
    val flags: List<String> = emptyList(),
    val license: String = "free",
    val notes: String = "",
    val upstream: String = "",
) {
    val essential get() = "essential" in flags

    /** Whether the engine can install this unattended, or it needs a human tap. */
    val scriptable get() = provider in setOf("pkg", "pip", "go", "git")

    /**
     * A terminal tool or a phone app.
     *
     * Browsing the catalogue gave no way to tell these apart, so a command-line
     * scanner and an APK looked like the same kind of thing — and tapping
     * Install on an APK entry appeared to do nothing.
     */
    val kind: Kind
        get() = when (provider) {
            "pkg", "pip", "go", "git" -> Kind.Cli
            "obtainium", "fdroid", "github", "play", "nethunter", "androidwtf" -> Kind.App
            else -> Kind.Guide
        }

    /** Where it lands, because finding it afterwards is half the battle. */
    val installsTo: String?
        get() = when (provider) {
            "pkg", "pip" -> "$PREFIX/bin/"
            "go" -> "~/go/bin/"
            "git" -> "~/wtf/src/${package_.trimEnd('/').substringAfterLast('/').removeSuffix(".git")}/"
            else -> null
        }

    /** How you actually run it once installed. */
    val howToRun: String?
        get() = when (provider) {
            "pkg", "pip" -> "$name  (already on PATH)"
            "go" -> "$name  (needs ~/go/bin on PATH — the installer adds it)"
            "git" -> "cd ~/wtf/src/${package_.trimEnd('/').substringAfterLast('/')} then read its README"
            else -> null
        }

    companion object { const val PREFIX = "\$PREFIX" }
}

enum class Kind { Cli, App, Guide }

val Kind.label get() = when (this) {
    Kind.Cli -> "CLI"
    Kind.App -> "APP"
    Kind.Guide -> "GUIDE"
}

val Kind.blurb get() = when (this) {
    Kind.Cli -> "Terminal tool. Runs inside Termux."
    Kind.App -> "Phone app. Installed from a store, one tap each."
    Kind.Guide -> "Not an install — a documented workflow or reference."
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
