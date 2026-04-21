package kpp.dev.aloM0d.client.core.mining

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.level.block.state.BlockState
import java.util.ArrayDeque

object MiningQueue {
    private data class Target(val pos: BlockPos, val direction: Direction)

    private val pending = ArrayDeque<BlockPos>()
    private var current: Target? = null

    val size: Int
        get() = pending.size + if (current == null) 0 else 1

    fun enqueue(positions: List<BlockPos>) {
        clear()
        positions.distinct().forEach { pos -> pending.add(pos.immutable()) }
    }

    fun clear() {
        pending.clear()
        current = null
    }

    fun tick(
        client: Minecraft,
        isValidTarget: (BlockState) -> Boolean,
        isWithinTargetReach: (Minecraft, BlockPos) -> Boolean = ::isWithinReach,
        keepOutOfReachTargets: Boolean = false
    ) {
        val level = client.level ?: return clear()
        val player = client.player ?: return clear()
        val gameMode = client.gameMode ?: return clear()

        val active = current

        if (active != null) {
            val state = level.getBlockState(active.pos)

            if (!state.isQueueTarget(isValidTarget)) {
                current = null
                return
            }

            if (!isWithinTargetReach(client, active.pos)) {
                current = null

                if (keepOutOfReachTargets) {
                    pending.add(active.pos.immutable())
                }

                return
            }

            if (gameMode.continueDestroyBlock(active.pos, active.direction)) {
                player.swing(InteractionHand.MAIN_HAND)
            }

            if (level.getBlockState(active.pos).isAir) {
                current = null
            }

            return
        }

        if (gameMode.isDestroying || client.options.keyAttack.isDown) return

        var attempts = pending.size

        while (attempts > 0 && pending.isNotEmpty()) {
            attempts--

            val pos = pending.removeFirst()
            val state = level.getBlockState(pos)

            if (!state.isQueueTarget(isValidTarget)) continue

            if (!isWithinTargetReach(client, pos)) {
                if (keepOutOfReachTargets) {
                    pending.add(pos)
                }

                continue
            }

            val target = Target(pos, directionFromPlayer(client, pos))

            if (gameMode.startDestroyBlock(target.pos, target.direction)) {
                current = target
                player.swing(InteractionHand.MAIN_HAND)
            } else if (keepOutOfReachTargets) {
                pending.add(pos)
            }

            return
        }
    }

    fun isWithinReach(client: Minecraft, pos: BlockPos): Boolean {
        val player = client.player ?: return false
        val range = player.blockInteractionRange() + RANGE_EPSILON

        return pos.closerToCenterThan(player.position(), range)
    }

    private fun BlockState.isQueueTarget(isValidTarget: (BlockState) -> Boolean): Boolean {
        return !isAir && isValidTarget(this)
    }

    private fun directionFromPlayer(client: Minecraft, pos: BlockPos): Direction {
        val player = client.player ?: return Direction.UP
        val delta = player.position().subtract(pos.center)

        return Direction.getApproximateNearest(delta)
    }

    private const val RANGE_EPSILON = 0.25
}
