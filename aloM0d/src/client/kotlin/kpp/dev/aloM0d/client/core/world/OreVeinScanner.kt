package kpp.dev.aloM0d.client.core.world

import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import java.util.ArrayDeque

object OreVeinScanner {
    private val oreTags = listOf(
        BlockTags.COAL_ORES,
        BlockTags.COPPER_ORES,
        BlockTags.IRON_ORES,
        BlockTags.GOLD_ORES,
        BlockTags.REDSTONE_ORES,
        BlockTags.LAPIS_ORES,
        BlockTags.DIAMOND_ORES,
        BlockTags.EMERALD_ORES
    )

    private val woodTags = listOf(
        BlockTags.OAK_LOGS,
        BlockTags.SPRUCE_LOGS,
        BlockTags.BIRCH_LOGS,
        BlockTags.JUNGLE_LOGS,
        BlockTags.ACACIA_LOGS,
        BlockTags.DARK_OAK_LOGS,
        BlockTags.PALE_OAK_LOGS,
        BlockTags.MANGROVE_LOGS,
        BlockTags.CHERRY_LOGS,
        BlockTags.CRIMSON_STEMS,
        BlockTags.WARPED_STEMS,
        BlockTags.LOGS
    )

    fun veinTagFor(state: BlockState): TagKey<Block>? {
        return veinTags.firstOrNull { tag -> state.`is`(tag) }
    }

    fun scan(
        level: Level,
        root: BlockPos,
        veinTag: TagKey<Block>,
        maxBlocks: Int,
        maxDistanceFromRoot: Double? = null
    ): List<BlockPos> {
        val visited = mutableSetOf<BlockPos>()
        val found = mutableListOf<BlockPos>()
        val queue = ArrayDeque<BlockPos>()
        val rootCenter = root.center

        queue.add(root.immutable())
        visited.add(root.immutable())

        while (queue.isNotEmpty() && found.size < maxBlocks) {
            val pos = queue.removeFirst()
            val state = level.getBlockState(pos)

            if (!state.`is`(veinTag)) continue

            found.add(pos)

            adjacentOffsets.forEach { offset ->
                val next = pos.offset(offset.x, offset.y, offset.z).immutable()

                if (visited.add(next) && isWithinScanRadius(next, rootCenter, maxDistanceFromRoot)) {
                    queue.add(next)
                }
            }
        }

        return found
    }

    private fun isWithinScanRadius(pos: BlockPos, rootCenter: Vec3, maxDistanceFromRoot: Double?): Boolean {
        return maxDistanceFromRoot == null || pos.closerToCenterThan(rootCenter, maxDistanceFromRoot)
    }

    private val veinTags = oreTags + woodTags

    private data class BlockOffset(val x: Int, val y: Int, val z: Int)

    private val adjacentOffsets = buildList {
        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    if (x != 0 || y != 0 || z != 0) {
                        add(BlockOffset(x, y, z))
                    }
                }
            }
        }
    }
}
