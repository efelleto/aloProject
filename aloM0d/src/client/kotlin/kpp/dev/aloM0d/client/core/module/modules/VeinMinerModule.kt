package kpp.dev.aloM0d.client.core.module.modules

import kpp.dev.aloM0d.client.core.config.FallbackConfig
import kpp.dev.aloM0d.client.core.event.ClientTickEvent
import kpp.dev.aloM0d.client.core.mining.MiningQueue
import kpp.dev.aloM0d.client.core.module.Module
import kpp.dev.aloM0d.client.core.module.ModuleCategory
import kpp.dev.aloM0d.client.core.world.OreVeinScanner
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.tags.TagKey
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

object VeinMinerModule : Module(
    id = "vein-miner",
    title = "Vein Miner",
    description = "Queues connected ores and logs after you mine the first block while sneaking.",
    category = ModuleCategory.WORLD
) {
    private var pendingVein: PendingVein? = null
    private var activeVeinTag: TagKey<Block>? = null
    private var highlightedBlocks = emptyList<BlockPos>()
    private var highlightedProgress = 0.0f
    private val mirroredBreakOverlays = mutableMapOf<Int, BlockPos>()

    val debugHighlights: List<DebugHighlight>
        get() {
            if (!DebugModule.active) return emptyList()

            return highlightedBlocks.map { pos ->
                DebugHighlight(pos, highlightedProgress)
            }
        }

    override fun onDisable() {
        pendingVein = null
        activeVeinTag = null
        highlightedBlocks = emptyList()
        highlightedProgress = 0.0f
        clearMirroredBreakOverlays(Minecraft.getInstance().level)
        MiningQueue.clear()
    }

    override fun onTick(event: ClientTickEvent) {
        activatePendingVein(event)
        clearCancelledPendingVein(event)
        captureVein(event)
        updateDebugProgress(event)
        updateMirroredBreakOverlays(event)

        val veinTag = activeVeinTag

        if (veinTag != null) {
            MiningQueue.tick(
                client = event.client,
                isValidTarget = { state ->
                    isMineableVeinBlock(state, veinTag) { candidate ->
                        event.client.player?.mainHandItem?.isCorrectToolForDrops(candidate) == true
                    }
                },
                isWithinTargetReach = ::isWithinVeinBreakReach,
                keepOutOfReachTargets = true
            )
        }

        if (MiningQueue.size == 0) {
            activeVeinTag = null

            if (pendingVein == null) {
                highlightedBlocks = emptyList()
                highlightedProgress = 0.0f
                clearMirroredBreakOverlays(event.client.level)
            }
        }
    }

    private fun captureVein(event: ClientTickEvent) {
        if (pendingVein != null || MiningQueue.size > 0) return

        val client = event.client
        val level = client.level ?: return
        val player = client.player ?: return
        val hitResult = client.hitResult as? BlockHitResult ?: return

        if (!client.options.keyAttack.isDown) return
        if (!player.isShiftKeyDown) return
        if (hitResult.type != HitResult.Type.BLOCK) return

        val root = hitResult.blockPos.immutable()
        val state = level.getBlockState(root)
        val veinTag = OreVeinScanner.veinTagFor(state) ?: return

        if (!player.mainHandItem.isCorrectToolForDrops(state)) return
        if (!isWithinRootReach(client, root)) return

        val veinBlocks = OreVeinScanner
            .scan(
                level = level,
                root = root,
                veinTag = veinTag,
                maxBlocks = MAX_BLOCKS,
                maxDistanceFromRoot = VEIN_SCAN_RADIUS
            )
            .filter { pos -> pos != root }
            .sortedBy { pos -> pos.distToCenterSqr(player.position()) }

        if (veinBlocks.isEmpty()) return

        pendingVein = PendingVein(root, veinTag, veinBlocks)
        highlightedBlocks = veinBlocks
        highlightedProgress = 0.0f
    }

    private fun activatePendingVein(event: ClientTickEvent) {
        val pending = pendingVein ?: return
        val level = event.client.level ?: return

        if (!level.getBlockState(pending.root).isAir) return

        if (!FallbackConfig.forceMultiplayerFallback && breakSingleplayerVein(event.client, pending)) {
            pendingVein = null
            activeVeinTag = null
            highlightedBlocks = emptyList()
            highlightedProgress = 0.0f
            clearMirroredBreakOverlays(event.client.level)
            return
        }

        MiningQueue.enqueue(pending.blocks)
        activeVeinTag = pending.veinTag
        pendingVein = null
        highlightedBlocks = pending.blocks
        highlightedProgress = 1.0f
        clearMirroredBreakOverlays(event.client.level)
    }

    private fun clearCancelledPendingVein(event: ClientTickEvent) {
        val pending = pendingVein ?: return
        val client = event.client
        val level = client.level ?: return clearPendingVein()
        val player = client.player ?: return clearPendingVein()
        val state = level.getBlockState(pending.root)

        if (!state.`is`(pending.veinTag)) return clearPendingVein()
        if (!client.options.keyAttack.isDown || !player.isShiftKeyDown) return clearPendingVein()
        if (!isWithinRootReach(client, pending.root)) return clearPendingVein()

        val hitResult = client.hitResult as? BlockHitResult ?: return clearPendingVein()

        if (hitResult.type != HitResult.Type.BLOCK || hitResult.blockPos != pending.root) {
            clearPendingVein()
        }
    }

    private fun clearPendingVein() {
        pendingVein = null
        highlightedBlocks = emptyList()
        highlightedProgress = 0.0f
        clearMirroredBreakOverlays(Minecraft.getInstance().level)
    }

    private fun updateDebugProgress(event: ClientTickEvent) {
        if (!DebugModule.active) return

        highlightedProgress = when {
            pendingVein != null -> {
                val stage = event.client.gameMode?.getDestroyStage() ?: -1

                if (stage < 0) {
                    0.0f
                } else {
                    ((stage + 1) / 10.0f).coerceIn(0.0f, 1.0f)
                }
            }

            MiningQueue.size > 0 -> 1.0f
            else -> 0.0f
        }
    }

    private fun updateMirroredBreakOverlays(event: ClientTickEvent) {
        val level = event.client.level ?: return clearMirroredBreakOverlays(null)
        val stage = event.client.gameMode?.getDestroyStage() ?: -1

        if (pendingVein == null || stage < 0) {
            clearMirroredBreakOverlays(level)
            return
        }

        val activeIds = mutableSetOf<Int>()

        highlightedBlocks.forEachIndexed { index, pos ->
            val id = DEBUG_BREAK_OVERLAY_ID_BASE - index
            val overlayPos = pos.immutable()

            activeIds.add(id)
            mirroredBreakOverlays[id] = overlayPos
            level.destroyBlockProgress(id, overlayPos, stage.coerceIn(0, 9))
        }

        val staleIds = mirroredBreakOverlays.keys - activeIds

        staleIds.forEach { id ->
            val pos = mirroredBreakOverlays.remove(id) ?: return@forEach
            level.destroyBlockProgress(id, pos, -1)
        }
    }

    private fun clearMirroredBreakOverlays(level: ClientLevel?) {
        if (mirroredBreakOverlays.isEmpty()) return

        val overlays = mirroredBreakOverlays.toMap()
        mirroredBreakOverlays.clear()

        level ?: return

        overlays.forEach { (id, pos) ->
            level.destroyBlockProgress(id, pos, -1)
        }
    }

    private fun breakSingleplayerVein(client: Minecraft, pending: PendingVein): Boolean {
        val clientLevel = client.level ?: return false
        val clientPlayer = client.player ?: return false
        val server = client.singleplayerServer ?: return false
        val serverLevel = server.getLevel(clientLevel.dimension()) ?: return false
        val serverPlayer = server.playerList.getPlayer(clientPlayer.uuid) ?: return false
        val blocks = pending.blocks.map(BlockPos::immutable)

        server.executeIfPossible {
            val playerPosition = serverPlayer.position()

            blocks.forEach { pos ->
                val state = serverLevel.getBlockState(pos)

                if (!isMineableVeinBlock(state, pending.veinTag, serverPlayer::hasCorrectToolForDrops)) return@forEach
                if (!isWithinVeinBreakReach(playerPosition, pos)) return@forEach
                if (!serverPlayer.mayInteract(serverLevel, pos)) return@forEach

                serverPlayer.gameMode.destroyBlock(pos)
            }
        }

        return true
    }

    private fun isWithinRootReach(client: Minecraft, pos: BlockPos): Boolean {
        return MiningQueue.isWithinReach(client, pos)
    }

    private fun isWithinVeinBreakReach(client: Minecraft, pos: BlockPos): Boolean {
        val player = client.player ?: return false

        return isWithinVeinBreakReach(player.position(), pos)
    }

    private fun isWithinVeinBreakReach(playerPosition: Vec3, pos: BlockPos): Boolean {
        return pos.closerToCenterThan(playerPosition, VEIN_BREAK_REACH)
    }

    private fun isMineableVeinBlock(
        state: BlockState,
        veinTag: TagKey<Block>,
        hasCorrectTool: (BlockState) -> Boolean
    ): Boolean {
        return !state.isAir && state.`is`(veinTag) && hasCorrectTool(state)
    }

    private data class PendingVein(
        val root: BlockPos,
        val veinTag: TagKey<Block>,
        val blocks: List<BlockPos>
    )

    data class DebugHighlight(
        val pos: BlockPos,
        val progress: Float
    )

    private const val MAX_BLOCKS = 64
    private const val DEBUG_BREAK_OVERLAY_ID_BASE = -1_000_000
    private const val VEIN_SCAN_RADIUS = 16.0
    private const val VEIN_BREAK_REACH = 8.0
}
