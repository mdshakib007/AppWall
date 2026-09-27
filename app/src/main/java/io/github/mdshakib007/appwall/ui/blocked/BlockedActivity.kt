package io.github.mdshakib007.appwall.ui.blocked

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.mdshakib007.appwall.Graph
import io.github.mdshakib007.appwall.MainActivity
import io.github.mdshakib007.appwall.data.db.BlockItem
import io.github.mdshakib007.appwall.data.db.BlockType
import io.github.mdshakib007.appwall.ui.theme.AppWallTheme
import io.github.mdshakib007.appwall.ui.theme.ThemeMode

/** Full-screen interstitial shown on top of a blocked app / website. */
class BlockedActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ITEM_ID = "item_id"
        private const val EXTRA_PROTECTED = "protected"

        fun show(context: Context, item: BlockItem) {
            context.startActivity(intent(context).putExtra(EXTRA_ITEM_ID, item.id))
        }

        fun showProtected(context: Context) {
            context.startActivity(intent(context).putExtra(EXTRA_PROTECTED, true))
        }

        private fun intent(context: Context) = Intent(context, BlockedActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Graph.init(this)
        enableEdgeToEdge()
        render(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render(intent)
    }

    private fun render(intent: Intent) {
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        val protected = intent.getBooleanExtra(EXTRA_PROTECTED, false)
        setContent {
            val mode by Graph.prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            AppWallTheme(mode) {
                BackHandler { goHome() }
                Screen(itemId, protected)
            }
        }
    }

    @Composable
    private fun Screen(itemId: Long, protected: Boolean) {
        val state by Graph.engine.stateFlow.collectAsState()
        val item = state.items.firstOrNull { it.id == itemId }
        BlockedScreen(
            item = item,
            protectedMode = protected,
            focus = state.focus,
            onGoHome = ::goHome,
            onOpenAppWall = {
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            },
        )
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
        overridePendingTransition(0, 0)
    }
}
