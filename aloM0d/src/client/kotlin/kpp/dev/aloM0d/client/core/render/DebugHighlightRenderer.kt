package kpp.dev.aloM0d.client.core.render

import kpp.dev.aloM0d.client.core.module.modules.VeinMinerModule
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.client.renderer.rendertype.aloM0dRenderTypes
import net.minecraft.util.ARGB
import net.minecraft.world.phys.shapes.Shapes

object DebugHighlightRenderer {
    fun init() {
        LevelRenderEvents.END_MAIN.register(::render)
    }

    private fun render(context: LevelRenderContext) {
        val highlights = VeinMinerModule.debugHighlights
        if (highlights.isEmpty()) return

        val cameraPos = context.levelState().cameraRenderState.pos
        val bufferSource = context.bufferSource()
        val buffer = bufferSource.getBuffer(aloM0dRenderTypes.debugLinesThroughBlocks())

        highlights.forEach { highlight ->
            val pos = highlight.pos

            ShapeRenderer.renderShape(
                context.poseStack(),
                buffer,
                Shapes.block(),
                pos.x.toDouble() - cameraPos.x,
                pos.y.toDouble() - cameraPos.y,
                pos.z.toDouble() - cameraPos.z,
                HIGHLIGHT_COLOR,
                LINE_WIDTH
            )

            if (highlight.progress > 0.0f) {
                ShapeRenderer.renderShape(
                    context.poseStack(),
                    buffer,
                    Shapes.box(0.0, 0.0, 0.0, 1.0, highlight.progress.toDouble(), 1.0),
                    pos.x.toDouble() - cameraPos.x,
                    pos.y.toDouble() - cameraPos.y,
                    pos.z.toDouble() - cameraPos.z,
                    PROGRESS_COLOR,
                    PROGRESS_LINE_WIDTH
                )
            }
        }

        bufferSource.endBatch()
    }

    private val HIGHLIGHT_COLOR = ARGB.color(150, 67, 209, 122)
    private val PROGRESS_COLOR = ARGB.color(240, 255, 225, 94)
    private const val LINE_WIDTH = 2.0f
    private const val PROGRESS_LINE_WIDTH = 4.0f
}
