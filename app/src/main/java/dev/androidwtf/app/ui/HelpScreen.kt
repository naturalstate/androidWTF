package dev.androidwtf.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Help, written for someone who has never used a terminal.
 *
 * Everything here is something that actually cost time on real hardware: the
 * missing Ctrl key, the commented-out config line, the Go binary that installed
 * fine and then could not be run.
 */
@Composable
fun HelpScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 96.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Help", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "Written assuming you have never used a terminal. Tap a section to open it.",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
        Spacer(Modifier.height(18.dp))

        // ------------------------------------------------------------ keyboard
        Expandable(
            "You need a keyboard with Ctrl",
            "Android keyboards have no Ctrl, Esc, Tab or arrows",
            startExpanded = true,
        ) {
            P("The stock Android keyboard has letters, numbers and punctuation. " +
                "A terminal needs more than that: Ctrl to send control signals, Tab " +
                "to autocomplete, arrows to move around, Esc to back out. Without " +
                "them a lot of the terminal simply cannot be driven.")

            H("Option 1 — the volume keys (nothing to install)")
            P("Termux maps the volume buttons to the missing keys. This works out " +
                "of the box and is the fastest way to get unstuck.")
            KeyRow("Vol Down + C", "Ctrl+C — stop whatever is running")
            KeyRow("Vol Down + any", "Ctrl + that letter")
            KeyRow("Vol Up + E", "Esc")
            KeyRow("Vol Up + T", "Tab")
            KeyRow("Vol Up + WASD", "arrow up, left, down, right")
            KeyRow("Vol Up + K / J", "page up / page down")
            KeyRow("Vol Up + 1…9", "F1…F9")
            KeyRow("Vol Up + U", "underscore _")
            KeyRow("Vol Up + L", "pipe |")
            Spacer(Modifier.height(6.dp))

            H("Option 2 — Termux's extra keys row")
            P("Termux can show a strip of ESC / CTRL / ALT / TAB / arrows above the " +
                "keyboard. Swipe up on the keyboard area to toggle it. It is the " +
                "easiest permanent fix and costs nothing.")

            H("Option 3 — Hacker's Keyboard")
            P("A full PC-style keyboard with real Ctrl, Alt, Esc, Tab, arrows and " +
                "function keys. It is in the catalogue under Core, from F-Droid. " +
                "Best in landscape — it is cramped in portrait. After installing you " +
                "must enable it in Android Settings → System → Languages & input, " +
                "then switch to it with the keyboard icon.")
        }

        // ------------------------------------------------------------- terminal
        Expandable("Terminal survival", "The handful of keys that get you out of trouble") {
            P("If you learn nothing else, learn Ctrl+C.")

            H("Stopping things")
            KeyRow("Ctrl+C", "Stop the running command. Not copy — in a terminal " +
                "Ctrl+C means cancel, and it has meant that far longer than it has " +
                "meant copy.")
            KeyRow("Ctrl+D", "End of input. On an empty line it closes the shell.")
            KeyRow("q", "Quits most things that are showing you text and waiting.")
            Spacer(Modifier.height(6.dp))

            H("Moving faster")
            KeyRow("Tab", "Autocomplete. Type part of a name and press it.")
            KeyRow("Up arrow", "Previous command. Saves retyping long ones.")
            KeyRow("Ctrl+L", "Clear the screen.")
            KeyRow("Ctrl+A", "Jump to the start of the line.")
            KeyRow("Ctrl+E", "Jump to the end of the line.")
            KeyRow("Ctrl+U", "Delete the whole line and start again.")
            Spacer(Modifier.height(6.dp))

            H("If something looks frozen")
            P("It is usually still working — installs and downloads can take minutes " +
                "with no output. Give it time before pressing anything. If you are " +
                "sure it is stuck, Ctrl+C.")
        }

        // ---------------------------------------------------------------- nano
        Expandable("Editing a file with nano", "What ^O means, and how to actually save") {
            P("nano is the simplest terminal text editor. Open a file by naming it:")
            Code("nano ~/.termux/termux.properties")

            H("The symbols at the bottom")
            P("nano lists its commands along the bottom of the screen as things like " +
                "^O and ^X. The caret ^ means Ctrl. So ^O is Ctrl and O together, " +
                "not three separate presses. An M- prefix means Alt.")

            H("The only four you need")
            KeyRow("Ctrl+O", "Save. It then asks for the filename — just press Enter " +
                "to keep the same one. nano calls this WriteOut, which is why it is O.")
            KeyRow("Enter", "Confirms that filename prompt.")
            KeyRow("Ctrl+X", "Exit. If you have unsaved changes it asks Y or N first.")
            KeyRow("Ctrl+K", "Cut the current line.")
            Spacer(Modifier.height(6.dp))

            H("Saving and quitting, start to finish")
            P("Make your edit, then: Ctrl+O, Enter, Ctrl+X. That sequence saves and " +
                "leaves. If you get lost, Ctrl+X and answer N to leave without saving " +
                "anything — nothing is harmed.")

            H("Moving around")
            P("Arrow keys move the cursor. There is no mouse and no tapping to " +
                "position the cursor. This is the main reason you want a keyboard " +
                "with arrows or the volume-key mappings above.")
        }

        // -------------------------------------------------------- config syntax
        Expandable(
            "Config file syntax",
            "Why a setting can be present and still switched off",
            accent = Tier2,
        ) {
            P("Config files are plain text, one setting per line, usually written as " +
                "a name, an equals sign and a value:")
            Code("allow-external-apps = true")

            H("The # trap")
            P("A line starting with # is a comment. The program ignores it completely. " +
                "Comments exist so a file can document itself and ship with every " +
                "option listed but switched off.")
            P("Termux does exactly that. Open termux.properties on a fresh install and " +
                "you will see allow-external-apps sitting right there — commented out. " +
                "It looks configured. It is not doing anything.")
            Code("# allow-external-apps = true   <- ignored\nallow-external-apps = true     <- active")
            P("To switch a commented line on, delete the # and any space before the " +
                "name. To switch a line off, put a # back at the start.")

            H("Check what is actually live")
            P("This hides every comment and shows only real settings:")
            Code("grep -v '^#' ~/.termux/termux.properties")
            P("If your setting does not appear there, it is not active, whatever the " +
                "file appears to say.")

            H("Termux needs telling")
            P("Termux reads this file at startup. After editing, run this or the " +
                "change will not take effect until you fully restart the app:")
            Code("termux-reload-settings")

            H("Skip the editor entirely")
            P("Appending a line is safer than editing one, because it works no matter " +
                "what the file already contains — a live line further down wins:")
            Code("echo 'allow-external-apps = true' >> ~/.termux/termux.properties")
            P("Note the double >>. A single > would erase the file and replace it. " +
                "That difference matters everywhere in the terminal.")
        }

        // ------------------------------------------------------ where it lands
        Expandable(
            "Where tools get installed",
            "Four package managers, four different directories",
            accent = Tier1,
        ) {
            P("androidWTF installs things four different ways depending on how the " +
                "tool is published. They do not all land in the same place, and only " +
                "some end up runnable by name.")

            H("pkg — Termux's own packages")
            Code("pkg install nmap        ->  \$PREFIX/bin/nmap")
            P("Lands in Termux's bin directory, which is already on your PATH. Type " +
                "nmap and it runs. Nothing else to do.")

            H("pip — Python packages")
            Code("pip install sqlmap      ->  \$PREFIX/bin/sqlmap")
            P("Also lands on PATH. Works the same way.")

            H("go — Go modules")
            Code("go install ...ffuf@latest  ->  ~/go/bin/ffuf")
            P("This is the one that catches everyone. Go does not use Termux's bin " +
                "directory — it has its own, ~/go/bin, and that is NOT on your PATH " +
                "by default. The install succeeds, and then typing ffuf says command " +
                "not found, which looks exactly like a failed install.")
            P("androidWTF adds ~/go/bin to your PATH when it runs a Go install, but " +
                "only new terminal sessions pick that up. Close the session and open " +
                "a fresh one, or run it directly:")
            Code("~/go/bin/ffuf -h")

            H("git — cloned source")
            Code("git clone ...  ->  ~/wtf/src/<name>/")
            P("Not a program at all — a folder of source code. There is no command to " +
                "type. Go into the folder and read its README to find out how to run " +
                "it. Many need a build step first.")

            H("What PATH actually is")
            P("PATH is the list of folders the shell searches when you type a command. " +
                "If a program is not in one of them you must give its full location. " +
                "See your list, one folder per line:")
            Code("echo \$PATH | tr ':' '\\n'")
            P("Find out where a command lives, or whether the shell can see it at all:")
            Code("command -v ffuf")
            P("No output means it is not on PATH. Add a folder permanently:")
            Code("echo 'export PATH=\"\$HOME/go/bin:\$PATH\"' >> ~/.bashrc")
            P("Then open a new session. Editing .bashrc does not affect the session " +
                "you are already in.")
        }

        // ----------------------------------------------------------- app to termux
        Expandable("The app's buttons do nothing", "Almost always one setting", accent = Danger) {
            P("Termux ignores commands from other apps until you explicitly allow it, " +
                "and when it refuses it says nothing useful. Work through these in order.")

            H("1. Is the setting actually live?")
            Code("grep -v '^#' ~/.termux/termux.properties | grep external")
            P("No output means it is missing or commented out. Fix it with:")
            Code("echo 'allow-external-apps = true' >> ~/.termux/termux.properties && termux-reload-settings")

            H("2. Is the engine installed?")
            Code("command -v wtf")
            P("No output means the bootstrap has not run, or did not finish. Re-run it " +
                "— it is safe to run repeatedly and updates in place. The Setup tab has " +
                "it with a copy button.")

            H("3. Can Termux come to the front?")
            P("Android Settings → Apps → Termux → Display over other apps. Without " +
                "this the command genuinely runs, but the terminal never appears, " +
                "which is indistinguishable from nothing happening.")

            H("4. Did this app get its permission?")
            P("The Home screen offers a Grant button when the permission is missing. " +
                "If you dismissed it, Android Settings → Apps → androidWTF → Permissions.")
        }

        // ----------------------------------------------------------- error messages
        Expandable("Error messages, translated", "What they actually mean") {
            H("command not found")
            P("The shell searched every folder on PATH and found nothing by that name. " +
                "Either it is not installed, or it is installed somewhere not on PATH " +
                "— see the Go section above, which is the usual culprit.")

            H("Metadata integrity can't be verified. Repository is disabled now.")
            P("Reads like a security problem. It is not. Termux moved its package " +
                "servers, and old installs still point at the old address, which now " +
                "redirects. apt refuses to follow a redirect for a signed repository " +
                "and reports it as a signature failure. Fix:")
            Code("termux-change-repo")
            P("Pick a mirror, then run pkg update. If it still fails, the Termux app " +
                "itself is too old to repair from inside — reinstall it from F-Droid. " +
                "A Play Store install can never be fixed; those repositories are gone " +
                "for good.")

            H("Permission denied")
            P("Either the file is not marked executable — chmod +x thefile — or the " +
                "action genuinely needs root, which most phones do not have.")

            H("No such file or directory (naming a file you can see)")
            P("Confusing but specific: the file exists, but the interpreter named on " +
                "its first line does not. Scripts start with a line like " +
                "#!/usr/bin/env bash, and Android has no /usr directory at all. Run it " +
                "by naming the interpreter yourself:")
            Code("bash thescript.sh")
        }

        // --------------------------------------------------------------- tiers
        Expandable("Tiers, and what root changes", "Why some tools are greyed out") {
            P("Android locks things down by default, and a lot of security tooling " +
                "needs privileges a normal app cannot have. Rather than let you " +
                "install things that will never work, every tool is labelled with what " +
                "it needs.")
            KeyRow("T0 Stock", "Any phone. Nothing unlocked, nothing voided.")
            KeyRow("T1 Shizuku", "ADB-level access without root. Reversible, free, " +
                "and the best value step you can take.")
            KeyRow("T2 Root", "Magisk or KernelSU. Raw sockets, real packet capture, " +
                "Frida. Also breaks banking apps.")
            KeyRow("T3 NetHunter", "A custom kernel. Monitor mode and packet " +
                "injection. Can brick a phone.")
            Spacer(Modifier.height(6.dp))
            P("Run doctor on the Setup tab and the app will detect which tier this " +
                "phone is on, then mark anything above it.")
        }

        // ---------------------------------------------------------------- kinds
        Expandable("CLI tools vs phone apps", "Why some things have no install button") {
            P("The catalogue holds two different kinds of thing.")
            H("CLI")
            P("Terminal programs that run inside Termux. androidWTF installs these " +
                "for you — select as many as you like and press Install.")
            H("APP")
            P("Ordinary Android apps. No app can install another app silently; Android " +
                "shows its own confirmation for every APK and there is no way around " +
                "it. These have no checkbox, and opening one tells you where to get it.")
            H("GUIDE")
            P("Not an install at all — a documented workflow or a reference, like " +
                "using Android's built-in Bluetooth logging.")
        }

        Spacer(Modifier.height(24.dp))
    }
}
