package dev.androidwtf.app.termux

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * What came back from a RUN_COMMAND execution.
 *
 * Without this the app is fire-and-forget, which means a silently refused
 * command and a successful one look identical — a button that flashes and does
 * nothing. That was the entire first-run experience on real hardware.
 */
data class RunResult(
    val label: String,
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int? = null,
    val errCode: Int? = null,
    val errMsg: String = "",
    val pending: Boolean = false,
) {
    // Termux sets a non-zero `err` in cases the command itself succeeded, so err
    // alone is not a failure signal. Trust the exit code, and only treat err as
    // fatal when nothing came back at all — which is the genuine "refused before
    // running" case.
    val ok get() = !pending &&
        (exitCode == null || exitCode == 0) &&
        (errCode == null || stdout.isNotBlank() || stderr.isNotBlank())

    /** Best-effort human explanation, including the failures Termux reports opaquely. */
    val diagnosis: String
        get() = when {
            pending -> "Sent to Termux. Waiting for a reply…"
            errMsg.contains("allow-external-apps", true) ||
                errMsg.contains("not allowed", true) ->
                "Termux refused the command. Set allow-external-apps = true in " +
                    "~/.termux/termux.properties, then run termux-reload-settings."
            // Termux refusing before it runs anything is almost always this one
            // setting. It reports the refusal opaquely, so name the likely cause
            // rather than passing the opacity along.
            errCode != null && stdout.isEmpty() && stderr.isEmpty() ->
                "Termux refused to run the command.\n\nThis is almost always " +
                    "allow-external-apps. Termux ignores commands from other apps " +
                    "until you turn it on, and reports nothing useful when it does." +
                    (if (errMsg.isNotBlank()) "\n\nTermux said: $errMsg" else "")
            exitCode == 127 ->
                "Command not found. The engine may not be installed — run the bootstrap " +
                    "in Termux, then try again."
            exitCode != null && exitCode != 0 -> "Exited with code $exitCode."
            else -> "Completed."
        }
}

/** Single-slot store for the most recent result, observed by the UI. */
object TermuxResults {
    var last by mutableStateOf<RunResult?>(null)
        private set

    /** Populated from `wtf doctor --json` so the app can show the tier itself. */
    var deviceTier by mutableStateOf<Int?>(null)
        private set
    var deviceSummary by mutableStateOf<String?>(null)
        private set

    fun starting(label: String) { last = RunResult(label, pending = true) }
    fun clear() { last = null }

    fun accept(label: String, b: Bundle?) {
        val r = b?.getBundle("result")
        val out = r?.getString("stdout").orEmpty()
        val result = RunResult(
            label = label,
            stdout = out,
            stderr = r?.getString("stderr").orEmpty(),
            exitCode = r?.get("exitCode") as? Int,
            errCode = (r?.get("err") as? Int)?.takeIf { it != 0 },
            errMsg = r?.getString("errmsg").orEmpty(),
        )
        last = result
        if (label == DOCTOR_LABEL && out.contains("\"tier\"")) parseDoctor(out)
    }

    /** Pull the tier out of `wtf doctor --json` without a JSON dependency here. */
    private fun parseDoctor(out: String) {
        Regex("\"tier\"\\s*:\\s*(\\d+)").find(out)?.groupValues?.get(1)?.toIntOrNull()
            ?.let { deviceTier = it }
        val brand = Regex("\"brand\"\\s*:\\s*\"([^\"]*)\"").find(out)?.groupValues?.get(1)
        val model = Regex("\"model\"\\s*:\\s*\"([^\"]*)\"").find(out)?.groupValues?.get(1)
        val sdk = Regex("\"sdk\"\\s*:\\s*(\\d+)").find(out)?.groupValues?.get(1)
        val repo = Regex("\"repo\"\\s*:\\s*\"([^\"]*)\"").find(out)?.groupValues?.get(1)
        deviceSummary = listOfNotNull(
            listOfNotNull(brand, model).joinToString(" ").ifBlank { null },
            sdk?.let { "API $it" },
            repo?.takeIf { it == "stale" }?.let { "repo stale" },
        ).joinToString(" · ").ifBlank { null }
    }

    const val DOCTOR_LABEL = "doctor"
}

/** Receives the PendingIntent Termux fires when a background command finishes. */
class TermuxResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        TermuxResults.accept(intent.getStringExtra(EXTRA_LABEL) ?: "command", intent.extras)
    }

    companion object { const val EXTRA_LABEL = "dev.androidwtf.LABEL" }
}
