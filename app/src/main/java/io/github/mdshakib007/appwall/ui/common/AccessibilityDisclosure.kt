package io.github.mdshakib007.appwall.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Google Play's Accessibility policy requires a prominent disclosure, with explicit consent, before an app sends
 * the user to enable an accessibility service. Shown every time we open that settings page.
 */
@Composable
fun AccessibilityDisclosureDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AppWall uses the Accessibility API") },
        text = {
            Text(
                "AppWall uses Android's Accessibility API to detect which app is in the foreground, so it can close a " +
                    "blocked app and show the Blocked screen, and to read the address bar of web browsers to count blocked " +
                    "attempts and measure time per website for your own statistics.\n\n" +
                    "It does not read, store or transmit what you type, your messages, passwords or page content. " +
                    "Nothing leaves your device; AppWall has no server and no account.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = { onDismiss(); context.startActivity(Permissions.accessibilityIntent()) }) { Text("Agree and continue") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}
