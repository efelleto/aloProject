package net.minecraft.client.renderer.rendertype

import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.DepthStencilState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.CompareOp
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat

object aloM0dRenderTypes {
    private val DEBUG_LINES_THROUGH_BLOCKS_PIPELINE = RenderPipeline.builder()
        .withLocation("pipeline/aloM0d_debug_lines_through_blocks")
        .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        .withUniform("Fog", UniformType.UNIFORM_BUFFER)
        .withUniform("Globals", UniformType.UNIFORM_BUFFER)
        .withVertexShader("core/rendertype_lines")
        .withFragmentShader("core/rendertype_lines")
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withCull(false)
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.Mode.LINES)
        .withDepthStencilState(DepthStencilState(CompareOp.ALWAYS_PASS, false))
        .build()

    private val debugLinesThroughBlocks = RenderType.create(
        "aloM0d_debug_lines_through_blocks",
        RenderSetup.builder(DEBUG_LINES_THROUGH_BLOCKS_PIPELINE)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .createRenderSetup()
    )

    fun debugLinesThroughBlocks(): RenderType {
        return debugLinesThroughBlocks
    }
}
