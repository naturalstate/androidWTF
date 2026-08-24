package dev.androidwtf.app.termux

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Drives the `wtf` engine inside Termux via the RUN_COMMAND intent.
 *
 * The app deliberately contains no install logic of its own. It renders a
 * selection and asks the engine to act on it, so the GUI and the CLI cannot
 * disagree about what a given selection actually does.
 *
 * Three things must all be true for this to work, and each fails differently:
 *   1. Termux is installed                      -> TermuxState.NotInstalled
 *   2. this app holds RUN_COMMAND permission    -> TermuxState.NoPermission
 *   3. allow-external-apps=true is set in
 *      ~/.termux/termux.properties              -> cannot be detected, only inferred
 *
 * (3) is the awkward one: there is no API to query it. A command simply does
 * nothing. So the UI states it as a prerequisite up front rather than letting
 * the user discover it through silence.
 */
object Termux {

    const val PACKAGE = "com.termux"
    private const val SERVICE = "com.termux.app.RunCommandService"
    private const val ACTION = "com.termux.RUN_COMMAND"
    private const val PERMISSION = "com.termux.permission.RUN_COMMAND"

    private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    private const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"

    private const val PREFIX = "/data/data/com.termux/files/usr"
    private const val HOME = "/data/data/com.termux/files/home"

    /** Termux's bash. Always the command path — see runWtf. */
    private const val BASH = "$PREFIX/bin/bash"

    /** The engine, where install.sh clones it. */
    const val WTF = "$HOME/.wtf/repo/platforms/android/bootstrap/wtf"

    val bootstrapCommand =
        "curl -fsSL https://raw.githubusercontent.com/naturalstate/androidWTF/" +
            "main/platforms/android/bootstrap/install.sh | bash"

    fun isInstalled(ctx: Context): Boolean = try {
        ctx.packageManager.getPackageInfo(PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun state(ctx: Context): TermuxState = when {
        !isInstalled(ctx) -> TermuxState.NotInstalled
        !hasPermission(ctx) -> TermuxState.NoPermission
        else -> TermuxState.Ready
    }

    /**
     * Run `wtf <args>` in a visible Termux session.
     *
     * Foreground rather than background on purpose: installs are long, noisy and
     * occasionally need input, and a progress bar that hides apt's actual output
     * is worse than the terminal. The app decides *what* to install; Termux shows
     * the work.
     */
    fun runWtf(ctx: Context, args: List<String>) {
        val intent = Intent(ACTION).apply {
            setClassName(PACKAGE, SERVICE)
            // Execute bash and pass the script as its first argument, rather than
            // executing the script directly. The repo copy carries a portable
            // "#!/usr/bin/env bash" shebang and Android has no /usr, so exec'ing
            // it gives ENOENT about the *interpreter* — which surfaces as a
            // baffling "No such file or directory" naming the script that plainly
            // does exist. Going through bash sidesteps the shebang entirely and
            // works whether or not install.sh generated its $PREFIX/bin launcher.
            putExtra(EXTRA_COMMAND_PATH, BASH)
            putExtra(EXTRA_ARGUMENTS, (listOf(WTF) + args).toTypedArray())
            putExtra(EXTRA_WORKDIR, HOME)
            putExtra(EXTRA_BACKGROUND, false)
            putExtra(EXTRA_SESSION_ACTION, "0")
        }
        // RunCommandService is a foreground service; from Android O it must be
        // started as one or the system kills it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
    }

    fun install(ctx: Context, toolIds: List<String>, dryRun: Boolean = false) {
        val args = mutableListOf("install", "--tools", toolIds.joinToString(","))
        if (dryRun) args += "--dry-run"
        runWtf(ctx, args)
    }

    fun installProfile(ctx: Context, profile: String, dryRun: Boolean = false) {
        val args = mutableListOf("install", "--profile", profile)
        if (dryRun) args += "--dry-run"
        runWtf(ctx, args)
    }

    fun doctor(ctx: Context) = runWtf(ctx, listOf("doctor"))

    fun openTermux(ctx: Context) {
        ctx.packageManager.getLaunchIntentForPackage(PACKAGE)?.let { ctx.startActivity(it) }
    }

    fun openFDroidTermux(ctx: Context) {
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/packages/com.termux"))
        )
    }

    fun requiredPermission() = PERMISSION
}

enum class TermuxState { NotInstalled, NoPermission, Ready }
