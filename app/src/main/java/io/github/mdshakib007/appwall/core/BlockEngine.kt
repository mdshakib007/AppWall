package io.github.mdshakib007.appwall.core

import io.github.mdshakib007.appwall.data.BlockRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Keeps an always-fresh [BlockState] in memory. Services read [state] synchronously.
 * Recomputes when items or focus change and once a minute (schedule boundaries).
 */
class BlockEngine(repo: BlockRepository, scope: CoroutineScope) {

    @Volatile var state: BlockState = BlockState.EMPTY
        private set

    private val _stateFlow = MutableStateFlow(BlockState.EMPTY)
    val stateFlow: StateFlow<BlockState> = _stateFlow

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            val now = System.currentTimeMillis()
            delay(60_000 - now % 60_000 + 50) // align to the next minute boundary
        }
    }

    init {
        scope.launch {
            combine(repo.items, repo.focus, ticker) { items, focus, _ ->
                BlockState(items, focus, System.currentTimeMillis())
            }.collect {
                state = it
                _stateFlow.value = it
            }
        }
    }
}
