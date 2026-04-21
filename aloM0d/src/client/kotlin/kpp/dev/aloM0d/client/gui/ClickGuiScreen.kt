package kpp.dev.aloM0d.client.gui

import kpp.dev.aloM0d.client.core.module.Module
import kpp.dev.aloM0d.client.core.module.ModuleCategory
import kpp.dev.aloM0d.client.core.module.ModuleManager
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

class ClickGuiScreen : Screen(Component.literal("aloM0d")) {
    private var draggingCategory: ModuleCategory? = null
    private var dragOffsetX = 0
    private var dragOffsetY = 0

    private data class PanelBounds(
        val category: ModuleCategory,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )

    private data class ModuleBounds(
        val module: Module,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        graphics.fill(0, 0, width, height, COLOR_OVERLAY)
        graphics.text(font, "aloM0d", OUTER_MARGIN, 8, COLOR_TEXT)

        layoutPanels().forEach { panel ->
            renderPanel(graphics, panel, mouseX, mouseY)
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, isDoubleClick)

        panelHeaderAt(event.x(), event.y())?.let { panel ->
            draggingCategory = panel.category
            dragOffsetX = event.x().toInt() - panel.x
            dragOffsetY = event.y().toInt() - panel.y
            bringToFront(panel.category)
            return true
        }

        moduleAt(event.x(), event.y())?.let { module ->
            module.toggle()
            return true
        }

        return super.mouseClicked(event, isDoubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseDragged(event, deltaX, deltaY)

        val category = draggingCategory ?: return super.mouseDragged(event, deltaX, deltaY)
        val panelHeight = panelHeight(category)
        val nextX = event.x().toInt() - dragOffsetX
        val nextY = event.y().toInt() - dragOffsetY

        panelPositions[category] = PanelPosition(
            x = nextX.coerceIn(0, maxOf(0, width - PANEL_WIDTH)),
            y = nextY.coerceIn(0, maxOf(0, height - panelHeight))
        )

        return true
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingCategory != null) {
            draggingCategory = null
            return true
        }

        return super.mouseReleased(event)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose()
            return true
        }

        return super.keyPressed(event)
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    override fun removed() {
        super.removed()
        draggingCategory = null
    }

    private fun renderPanel(graphics: GuiGraphicsExtractor, panel: PanelBounds, mouseX: Int, mouseY: Int) {
        graphics.fill(panel.x, panel.y, panel.x + panel.width, panel.y + panel.height, COLOR_PANEL)
        graphics.fill(panel.x, panel.y, panel.x + panel.width, panel.y + HEADER_HEIGHT, COLOR_HEADER)
        graphics.fill(panel.x, panel.y + HEADER_HEIGHT - 2, panel.x + panel.width, panel.y + HEADER_HEIGHT, COLOR_ACCENT)
        graphics.outline(panel.x, panel.y, panel.width, panel.height, COLOR_BORDER)
        graphics.text(font, panel.category.displayName, panel.x + TEXT_PADDING, panel.y + 6, COLOR_TEXT)

        val rows = moduleRows(panel)

        if (rows.isEmpty()) {
            graphics.text(font, "No modules", panel.x + TEXT_PADDING, panel.y + HEADER_HEIGHT + 6, COLOR_MUTED_TEXT)
            return
        }

        rows.forEach { row ->
            renderModuleRow(graphics, row, mouseX, mouseY)
        }
    }

    private fun renderModuleRow(graphics: GuiGraphicsExtractor, row: ModuleBounds, mouseX: Int, mouseY: Int) {
        val hovered = contains(row.x, row.y, row.width, row.height, mouseX.toDouble(), mouseY.toDouble())
        val background = when {
            row.module.enabled -> COLOR_ENABLED
            hovered -> COLOR_HOVER
            else -> COLOR_ROW
        }

        graphics.fill(row.x, row.y, row.x + row.width, row.y + row.height, background)
        graphics.text(
            font,
            fitText(row.module.title, row.width - TEXT_PADDING * 2 - TOGGLE_SIZE - 6),
            row.x + TEXT_PADDING,
            row.y + 5,
            if (row.module.enabled) COLOR_ENABLED_TEXT else COLOR_TEXT
        )

        val toggleX = row.x + row.width - TEXT_PADDING - TOGGLE_SIZE
        val toggleY = row.y + (row.height - TOGGLE_SIZE) / 2
        graphics.fill(toggleX, toggleY, toggleX + TOGGLE_SIZE, toggleY + TOGGLE_SIZE, COLOR_TOGGLE_TRACK)

        if (row.module.enabled) {
            graphics.fill(toggleX + 2, toggleY + 2, toggleX + TOGGLE_SIZE - 2, toggleY + TOGGLE_SIZE - 2, COLOR_TOGGLE_ON)
        }

        if (hovered) {
            graphics.setTooltipForNextFrame(font, Component.literal(row.module.description), mouseX, mouseY)
        }
    }

    private fun moduleAt(mouseX: Double, mouseY: Double): Module? {
        return layoutPanels()
            .asSequence()
            .toList()
            .asReversed()
            .asSequence()
            .flatMap { panel -> moduleRows(panel).asSequence() }
            .firstOrNull { row -> contains(row.x, row.y, row.width, row.height, mouseX, mouseY) }
            ?.module
    }

    private fun panelHeaderAt(mouseX: Double, mouseY: Double): PanelBounds? {
        return layoutPanels().asReversed().firstOrNull { panel ->
            contains(panel.x, panel.y, panel.width, HEADER_HEIGHT, mouseX, mouseY)
        }
    }

    private fun moduleRows(panel: PanelBounds): List<ModuleBounds> {
        return ModuleManager.byCategory(panel.category).mapIndexed { index, module ->
            ModuleBounds(
                module = module,
                x = panel.x + 1,
                y = panel.y + HEADER_HEIGHT + index * ROW_HEIGHT,
                width = panel.width - 2,
                height = ROW_HEIGHT
            )
        }
    }

    private fun layoutPanels(): List<PanelBounds> {
        ensureDefaultPanelPositions()

        return panelOrder.map { category ->
            val height = panelHeight(category)
            val position = panelPositions.getValue(category)

            PanelBounds(
                category = category,
                x = position.x.coerceIn(0, maxOf(0, width - PANEL_WIDTH)),
                y = position.y.coerceIn(0, maxOf(0, this.height - height)),
                width = PANEL_WIDTH,
                height = height
            )
        }
    }

    private fun ensureDefaultPanelPositions() {
        ModuleCategory.entries
            .filterNot(panelOrder::contains)
            .forEach(panelOrder::add)

        if (panelPositions.size == ModuleCategory.entries.size) return

        val maxX = width - OUTER_MARGIN
        var x = OUTER_MARGIN
        var y = TOP_MARGIN
        var rowHeight = 0

        ModuleCategory.entries.forEach { category ->
            if (panelPositions.containsKey(category)) return@forEach

            val height = panelHeight(category)

            if (x != OUTER_MARGIN && x + PANEL_WIDTH > maxX) {
                x = OUTER_MARGIN
                y += rowHeight + PANEL_GAP
                rowHeight = 0
            }

            panelPositions[category] = PanelPosition(x, y)
            x += PANEL_WIDTH + PANEL_GAP
            rowHeight = maxOf(rowHeight, height)
        }
    }

    private fun panelHeight(category: ModuleCategory): Int {
        return HEADER_HEIGHT + maxOf(1, ModuleManager.byCategory(category).size) * ROW_HEIGHT
    }

    private fun bringToFront(category: ModuleCategory) {
        panelOrder.remove(category)
        panelOrder.add(category)
    }

    private fun fitText(text: String, maxWidth: Int): String {
        if (font.width(text) <= maxWidth) return text
        if (maxWidth <= font.width(ELLIPSIS)) return ""

        return font.plainSubstrByWidth(text, maxWidth - font.width(ELLIPSIS)) + ELLIPSIS
    }

    private fun contains(x: Int, y: Int, width: Int, height: Int, mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
    }

    private companion object {
        private data class PanelPosition(val x: Int, val y: Int)

        private val panelPositions = mutableMapOf<ModuleCategory, PanelPosition>()
        private val panelOrder = mutableListOf<ModuleCategory>()

        private const val OUTER_MARGIN = 18
        private const val TOP_MARGIN = 28
        private const val PANEL_WIDTH = 122
        private const val PANEL_GAP = 8
        private const val HEADER_HEIGHT = 20
        private const val ROW_HEIGHT = 18
        private const val TEXT_PADDING = 7
        private const val TOGGLE_SIZE = 8
        private const val ELLIPSIS = "..."

        private val COLOR_OVERLAY = 0x99000000.toInt()
        private val COLOR_PANEL = 0xFF171717.toInt()
        private val COLOR_HEADER = 0xFF252525.toInt()
        private val COLOR_ROW = 0xFF1E1E1E.toInt()
        private val COLOR_HOVER = 0xFF303030.toInt()
        private val COLOR_BORDER = 0xFF555555.toInt()
        private val COLOR_ACCENT = 0xFF43D17A.toInt()
        private val COLOR_ENABLED = 0xFF2E7D53.toInt()
        private val COLOR_TOGGLE_TRACK = 0xFF000000.toInt()
        private val COLOR_TOGGLE_ON = 0xFFB8F3CA.toInt()
        private val COLOR_TEXT = 0xFFEFEFEF.toInt()
        private val COLOR_ENABLED_TEXT = 0xFFFFFFFF.toInt()
        private val COLOR_MUTED_TEXT = 0xFF9F9F9F.toInt()
    }
}
