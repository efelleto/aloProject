package kpp.dev.aloM0d.client.core.module.modules

import kpp.dev.aloM0d.client.core.event.ClientTickEvent
import kpp.dev.aloM0d.client.core.inventory.ToolSelector
import kpp.dev.aloM0d.client.core.mining.MiningQueue
import kpp.dev.aloM0d.client.core.module.Module
import kpp.dev.aloM0d.client.core.module.ModuleCategory
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

object AutoToolModule : Module(
    id = "auto-tool",
    title = "Auto Tool",
    description = "Selects the best inventory tool before mining a block.",
    category = ModuleCategory.MISC
) {
    override fun onTick(event: ClientTickEvent) {
        val client = event.client
        if (!client.options.keyAttack.isDown) return
        selectBestToolForHit(client = client)
    }

    fun selectBestToolForHit(client: Minecraft): Boolean {
        val level = client.level ?: return false
        val hitResult = client.hitResult as? BlockHitResult ?: return false
        if (hitResult.type != HitResult.Type.BLOCK) return false
        if (!MiningQueue.isWithinReach(client, hitResult.blockPos)) return false

        val state = level.getBlockState(hitResult.blockPos)
        if (state.isAir) return false

        return ToolSelector.selectBestTool(client, state)
    }
}
