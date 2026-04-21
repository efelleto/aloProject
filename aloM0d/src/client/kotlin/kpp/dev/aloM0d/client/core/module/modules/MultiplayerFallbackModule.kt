package kpp.dev.aloM0d.client.core.module.modules

import kpp.dev.aloM0d.client.core.config.FallbackConfig
import kpp.dev.aloM0d.client.core.module.Module
import kpp.dev.aloM0d.client.core.module.ModuleCategory

object MultiplayerFallbackModule : Module(
    id = "multiplayer-fallback",
    title = "MP Fallback",
    description = "Forces multiplayer-safe fallback logic instead of local singleplayer shortcuts.",
    category = ModuleCategory.MISC
) {
    override fun onEnable() {
        FallbackConfig.forceMultiplayerFallback = true
    }

    override fun onDisable() {
        FallbackConfig.forceMultiplayerFallback = false
    }
}
