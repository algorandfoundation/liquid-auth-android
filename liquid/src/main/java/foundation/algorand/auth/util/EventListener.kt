package foundation.algorand.auth.util

fun interface EventListener<E: Event> {
    fun onEvent(event: E)
}
