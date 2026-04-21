package kpp.dev.aloM0d.client.core.module

import kpp.dev.aloM0d.client.core.event.ClientTickEvent

abstract class Module(
    val id: String,
    val title: String,
    val description: String,
    val category: ModuleCategory
) {
    var enabled: Boolean = false
        private set

    fun toggle() {
        setEnabled(!enabled)
    }

    fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) return

        this.enabled = enabled

        if (enabled) {
            onEnable()
        } else {
            onDisable()
        }
    }

    open fun onEnable() {
    }

    open fun onDisable() {
    }

    open fun onTick(event: ClientTickEvent) {
    }
}
