package kpp.dev.aloM0d.client.core.module.modules

import kpp.dev.aloM0d.client.core.event.ClientTickEvent
import kpp.dev.aloM0d.client.core.module.Module
import kpp.dev.aloM0d.client.core.module.ModuleCategory

object TickCounterModule : Module(
    id = "tick-counter",
    title = "Tick Counter",
    description = "Counts client ticks while enabled.",
    category = ModuleCategory.MISC
) {
    var ticks: Long = 0
        private set

    override fun onEnable() {
        ticks = 0
    }

    override fun onTick(event: ClientTickEvent) {
        ticks++
    }
}
