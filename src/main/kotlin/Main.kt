import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime
import kotlin.time.Duration.Companion.nanoseconds

const val ELEMENT_COUNT = 10000
const val NODE_COUNT = 7 * ELEMENT_COUNT

class DummyNode(var attr: Int)

class DummyApplier : AbstractApplier<DummyNode>(DummyNode(0)) {
    var inserted = java.util.concurrent.Semaphore(-NODE_COUNT + 1)
    override fun onClear() {
    }

    override fun insertBottomUp(index: Int, instance: DummyNode) {
        inserted.release()
    }

    override fun insertTopDown(index: Int, instance: DummyNode) {
    }

    override fun move(from: Int, to: Int, count: Int) {
    }

    override fun remove(index: Int, count: Int) {
    }
}

@Composable
fun Dummy(attr: Int = 0, content: @Composable () -> Unit) {
    ComposeNode<DummyNode, DummyApplier>(
        factory = { DummyNode(0) },
        update = {
            set(attr) { this.attr = it }
        },
        content = content,
    )
}

@Composable
fun Row(e: Int) {
    Dummy {
        Dummy {
            Dummy(e) { }
            Dummy { }
        }
        Dummy(e) {
            Dummy {
                Dummy { }
            }
        }
    }
}

fun main(args: Array<String>): Unit = repeat(10) {
    run("Flat") {
        val data = remember { mutableStateListOf<Int>() }
        for (e in data) {
            key(e) {
                Row(e)
            }
        }
        LaunchedEffect(Unit) {
            data.addAll((1..ELEMENT_COUNT).toList())
        }
    }
    run("Nested") {
        val data = remember { mutableStateListOf<Int>() }
        for (chunk in data.chunked(100)) {
            for (e in chunk) {
                key(e) {
                    Row(e)
                }
            }
        }
        LaunchedEffect(Unit) {
            data.addAll((1..ELEMENT_COUNT).toList())
        }
    }
}

private fun run(info: String, content: @Composable () -> Unit) = measureNanoTime {
    GlobalSnapshotManager.ensureStarted()

    val clock = BroadcastFrameClock { }

    val coroutineContext = Dispatchers.Default + clock
    val recomposer = Recomposer(coroutineContext)

    val applier = DummyApplier()
    val composition = ControlledComposition(applier = applier, parent = recomposer)

    composition.setContent {
        content()
    }

    CoroutineScope(coroutineContext).launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }


    while (!applier.inserted.tryAcquire(10, TimeUnit.MILLISECONDS)) {
        clock.sendFrame(System.nanoTime())
    }

    recomposer.cancel()
    recomposer.close()
}.let { println("$info: ${it.nanoseconds.toIsoString()}") }