package kpp.dev.aloM0d.client.core.event

import net.minecraft.client.Minecraft

data class ClientTickEvent(val client: Minecraft) : Event
