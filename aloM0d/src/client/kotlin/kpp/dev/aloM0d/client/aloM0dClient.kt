package kpp.dev.aloM0d.client

import com.mojang.blaze3d.platform.InputConstants
import kpp.dev.aloM0d.client.core.event.ClientTickEvent
import kpp.dev.aloM0d.client.core.event.EventBus
import kpp.dev.aloM0d.client.core.module.ModuleManager
import kpp.dev.aloM0d.client.core.render.DebugHighlightRenderer
import kpp.dev.aloM0d.client.gui.ClickGuiScreen
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.KeyMapping
import org.lwjgl.glfw.GLFW

class aloM0dClient : ClientModInitializer {
    private lateinit var openGuiKey: KeyMapping

    override fun onInitializeClient() {
        ModuleManager.init()
        DebugHighlightRenderer.init()

        openGuiKey = KeyMappingHelper.registerKeyMapping(
            KeyMapping(
                "key.aloM0d.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyMapping.Category.MISC
            )
        )

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            EventBus.post(ClientTickEvent(client))

            while (openGuiKey.consumeClick()) {
                if (client.screen !is ClickGuiScreen) {
                    client.setScreen(ClickGuiScreen())
                }
            }
        }
    }
}
