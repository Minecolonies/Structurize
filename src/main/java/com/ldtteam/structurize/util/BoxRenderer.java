package com.ldtteam.structurize.util;

import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Matrix4f;

/**
 * Helper class to render boxes.
 */
public class BoxRenderer
{
    public static void drawSelectionBoundingBox(final Matrix4f matrix4f, AxisAlignedBB box, float red, float green, float blue, float alpha)
    {
        drawBoundingBox(matrix4f, (float) box.minX, (float) box.minY, (float) box.minZ, (float) box.maxX, (float) box.maxY, (float) box.maxZ, red, green, blue, alpha);
    }

    public static void drawBoundingBox(final Matrix4f matrix4f, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float red, float green, float blue, float alpha)
    {
        IRenderTypeBuffer.Impl bufferbuilder = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();

        drawBoundingBox(bufferbuilder.getBuffer(MRenderTypes.customLineRenderer()), minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha, matrix4f);

        bufferbuilder.finish();
    }

    public static void drawBoundingBox(
      IVertexBuilder buffer,
      float minX,
      float minY,
      float minZ,
      float maxX,
      float maxY,
      float maxZ,
      float red,
      float green,
      float blue,
      float alpha,
    final Matrix4f matrix)
    {
        //lower base
        buffer.pos(matrix, minX, minY, minZ).color(red, green, blue, 0.0F).endVertex();
        buffer.pos(matrix, minX, minY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, minX, minY, minZ).color(red, green, blue, alpha).endVertex();

        //first leg
        buffer.pos(matrix, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();

        //upper base
        buffer.pos(matrix, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, maxX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, minX, maxY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, minX, maxY, minZ).color(red, green, blue, alpha).endVertex();

        //links runter
        buffer.pos(matrix, minX, maxY, maxZ).color(red, green, blue, 0.0F).endVertex();
        buffer.pos(matrix, minX, minY, maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, maxX, minY, maxZ).color(red, green, blue, alpha).endVertex();

        //rechts runter
        buffer.pos(matrix, maxX, maxY, maxZ).color(red, green, blue, 0.0F).endVertex();
        buffer.pos(matrix, maxX, maxY, minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, maxX, minY, minZ).color(red, green, blue, alpha).endVertex();
    }

    /**
     * Private constructor to hide implicit one.
     */
    private BoxRenderer()
    {
        /*
         * Intentionally left empty.
         */
    }
}
