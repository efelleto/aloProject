package kpp.dev.aloM0d.client.core.event

import kotlin.reflect.KClass

object EventBus {
    private val listeners = mutableMapOf<KClass<out Event>, MutableList<(Event) -> Unit>>()

    fun <T : Event> subscribe(type: KClass<T>, listener: (T) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val typedListener: (Event) -> Unit = { event -> listener(event as T) }
        listeners.getOrPut(type) { mutableListOf() }.add(typedListener)
    }

    fun <T : Event> post(event: T) {
        listeners[event::class]?.forEach { listener -> listener(event) }
    }
}
