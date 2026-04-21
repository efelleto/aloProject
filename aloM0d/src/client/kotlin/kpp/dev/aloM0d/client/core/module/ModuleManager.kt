package kpp.dev.aloM0d.client.core.module

import kpp.dev.aloM0d.client.core.event.ClientTickEvent
import kpp.dev.aloM0d.client.core.event.EventBus
import kpp.dev.aloM0d.client.core.module.modules.AutoToolModule
import kpp.dev.aloM0d.client.core.module.modules.BreakHolderModule
import kpp.dev.aloM0d.client.core.module.modules.DebugModule
import kpp.dev.aloM0d.client.core.module.modules.MultiplayerFallbackModule
import kpp.dev.aloM0d.client.core.module.modules.TickCounterModule
import kpp.dev.aloM0d.client.core.module.modules.VeinMinerModule

object ModuleManager {
    private val registeredModules = mutableListOf<Module>()
    private var initialized = false

    val modules: List<Module>
        get() = registeredModules

    fun init() {
        if (initialized) return

        register(TickCounterModule)
        register(DebugModule)
        register(MultiplayerFallbackModule)
        register(AutoToolModule)
        register(BreakHolderModule)
        register(VeinMinerModule)

        EventBus.subscribe(ClientTickEvent::class) { event ->
            registeredModules
                .asSequence()
                .filter(Module::enabled)
                .forEach { module -> module.onTick(event) }
        }

        initialized = true
    }

    fun byCategory(category: ModuleCategory): List<Module> {
        return registeredModules.filter { module -> module.category == category }
    }

    private fun register(module: Module) {
        require(registeredModules.none { it.id == module.id }) {
            "Module with id '${module.id}' is already registered"
        }

        registeredModules.add(module)
    }
}
