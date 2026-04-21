package kpp.dev.aloM0d.client.core.module.modules

import kpp.dev.aloM0d.client.core.module.Module
import kpp.dev.aloM0d.client.core.module.ModuleCategory

object DebugModule : Module(
    id = "debug",
    title = "Debug",
    description = "Enables global debug visuals for development.",
    category = ModuleCategory.MISC
) {
    val active: Boolean
        get() = enabled
}
