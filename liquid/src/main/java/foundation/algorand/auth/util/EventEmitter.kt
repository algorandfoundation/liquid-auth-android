package foundation.algorand.auth.util

import kotlin.reflect.KClass

open class EventEmitter {
    val listeners = mutableMapOf<KClass<out Event>, MutableList<EventListener<out Event>>>()

    fun <E : Event> emit(event: E) {
        listeners[event::class]?.filterIsInstance<EventListener<E>>()?.forEach { it.onEvent(event) }
    }

    inline fun <reified E : Event> on(listener: EventListener<E>) {
        val eventListeners = listeners.getOrPut(E::class) {
            mutableListOf()
        }
        eventListeners.add(listener)
    }
}
