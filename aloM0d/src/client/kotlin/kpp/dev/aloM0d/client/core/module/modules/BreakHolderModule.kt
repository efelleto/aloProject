package kpp.dev.aloM0d.client.core.module.modules

import kpp.dev.aloM0d.client.core.event.ClientTickEvent
import kpp.dev.aloM0d.client.core.mining.MiningQueue
import kpp.dev.aloM0d.client.core.module.Module
import kpp.dev.aloM0d.client.core.module.ModuleCategory
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

object BreakHolderModule : Module(
    id = "break-holder",
    title = "Break Holder",
    description = "Keeps mining the block under your crosshair without holding attack.",
    category = ModuleCategory.MISC
) {
    private var activeTarget: BlockPos? = null

    override fun onDisable() {
        stopDestroyBlock()
    }

    override fun onTick(event: ClientTickEvent) {
        val client = event.client
        val level = client.level ?: return stopDestroyBlock()
        val player = client.player ?: return stopDestroyBlock()
        val gameMode = client.gameMode ?: return stopDestroyBlock()

        val hitResult = client.hitResult as? BlockHitResult ?: return stopDestroyBlock()
        if (hitResult.type != HitResult.Type.BLOCK) return stopDestroyBlock()

        val pos = hitResult.blockPos.immutable()
        val state = level.getBlockState(pos)

        if (state.isAir) {
            activeTarget = null
            return
        }

        if (!MiningQueue.isWithinReach(client, pos)) return stopDestroyBlock()

        if (AutoToolModule.enabled) {
            AutoToolModule.selectBestToolForHit(client)
        }

        if (activeTarget != pos) {
            stopDestroyBlock()

            if (gameMode.startDestroyBlock(pos, hitResult.direction)) {
                activeTarget = pos
                player.swing(InteractionHand.MAIN_HAND)
            }

            return
        }

        if (gameMode.continueDestroyBlock(pos, hitResult.direction)) {
            player.swing(InteractionHand.MAIN_HAND)
        }

        if (level.getBlockState(pos).isAir) {
            activeTarget = null
        }
    }

    private fun stopDestroyBlock() {
        activeTarget = null
        Minecraft.getInstance().gameMode?.stopDestroyBlock()
    }
}
