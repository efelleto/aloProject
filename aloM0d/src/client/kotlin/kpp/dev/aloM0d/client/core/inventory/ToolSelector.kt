package kpp.dev.aloM0d.client.core.inventory

import net.minecraft.client.Minecraft
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState

object ToolSelector {
    fun selectBestTool(client: Minecraft, state: BlockState): Boolean {
        val player = client.player ?: return false
        if (player.containerMenu != player.inventoryMenu) return false

        val gameMode = client.gameMode ?: return false
        val inventory = player.inventory
        val selectedSlot = inventory.selectedSlot
        val selectedStack = inventory.selectedItem
        val selectedSpeed = selectedStack.getDestroySpeed(state)
        if (!selectedStack.isEmpty && selectedStack.isCorrectToolForDrops(state) && selectedSpeed > BASE_MINING_SPEED) {
            return false
        }

        val requiresCorrectTool = state.requiresCorrectToolForDrops()
        val best = (0 until MAIN_INVENTORY_SIZE)
            .asSequence()
            .mapNotNull { slot -> candidateFor(inventory, slot, selectedSlot, state, requiresCorrectTool) }
            .maxWithOrNull(toolComparator())
            ?: return false

        if (best.inventorySlot == selectedSlot) return false
        if (!requiresCorrectTool && best.speed <= selectedSpeed) return false

        if (Inventory.isHotbarSlot(best.inventorySlot)) {
            inventory.selectedSlot = best.inventorySlot
            return true
        }

        val sourceSlotId = screenSlotId(best.inventorySlot) ?: return false
        gameMode.handleContainerInput(
            player.inventoryMenu.containerId,
            sourceSlotId,
            selectedSlot,
            ContainerInput.SWAP,
            player
        )

        return true
    }

    private fun candidateFor(
        inventory: Inventory,
        inventorySlot: Int,
        selectedSlot: Int,
        state: BlockState,
        requiresCorrectTool: Boolean
    ): ToolCandidate? {
        val stack = inventory.getItem(inventorySlot)
        if (stack.isEmpty) return null

        val correctForDrops = stack.isCorrectToolForDrops(state)
        if (requiresCorrectTool && !correctForDrops) return null

        return ToolCandidate(
            inventorySlot = inventorySlot,
            stack = stack,
            speed = stack.getDestroySpeed(state),
            correctForDrops = correctForDrops,
            isCurrentSlot = inventorySlot == selectedSlot,
            isHotbarSlot = Inventory.isHotbarSlot(inventorySlot)
        )
    }

    private fun toolComparator(): Comparator<ToolCandidate> {
        return compareBy<ToolCandidate> { it.speed }
            .thenBy { it.correctForDrops }
            .thenBy { it.isCurrentSlot }
            .thenBy { it.isHotbarSlot }
            .thenByDescending { it.inventorySlot }
    }

    private fun screenSlotId(inventorySlot: Int): Int? {
        return when (inventorySlot) {
            in HOTBAR_SLOT_RANGE -> HOTBAR_SCREEN_SLOT_OFFSET + inventorySlot
            in MAIN_SLOT_RANGE -> inventorySlot
            else -> null
        }
    }

    private data class ToolCandidate(
        val inventorySlot: Int,
        val stack: ItemStack,
        val speed: Float,
        val correctForDrops: Boolean,
        val isCurrentSlot: Boolean,
        val isHotbarSlot: Boolean
    )

    private val HOTBAR_SLOT_RANGE = 0 until Inventory.getSelectionSize()
    private val MAIN_SLOT_RANGE = Inventory.getSelectionSize() until MAIN_INVENTORY_SIZE
    private const val MAIN_INVENTORY_SIZE = 36
    private const val HOTBAR_SCREEN_SLOT_OFFSET = 36
    private const val BASE_MINING_SPEED = 1.0F
}
