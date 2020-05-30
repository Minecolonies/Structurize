package com.ldtteam.blockout.controls;

import com.ldtteam.blockout.Pane;
import com.ldtteam.blockout.PaneParams;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Iterator;
import static com.ldtteam.blockout.Log.getLogger;

/**
 * Simple image element.
 */
public class Image extends Pane
{
    public static final int MINECRAFT_DEFAULT_TEXTURE_MAP_SIZE = 256;

    protected ResourceLocation resourceLocation;
    protected int imageOffsetX = 0;
    protected int imageOffsetY = 0;
    protected int imageWidth = 0;
    protected int imageHeight = 0;
    protected int mapWidth = MINECRAFT_DEFAULT_TEXTURE_MAP_SIZE;
    protected int mapHeight = MINECRAFT_DEFAULT_TEXTURE_MAP_SIZE;
    protected boolean customSized = true;
    protected boolean autoscale = true;

    /**
     * Default Constructor.
     */
    public Image()
    {
        super();
    }

    /**
     * Constructor used by the xml loader.
     *
     * @param params PaneParams loaded from the xml.
     */
    public Image(final PaneParams params)
    {
        super(params);
        final String source = params.getStringAttribute("source", null);
        if (source != null)
        {
            resourceLocation = new ResourceLocation(source);
            loadMapDimensions();
        }

        PaneParams.SizePair size = params.getSizePairAttribute("imageoffset", null, null);
        if (size != null)
        {
            imageOffsetX = size.getX();
            imageOffsetY = size.getY();
        }

        size = params.getSizePairAttribute("imagesize", null, null);
        if (size != null)
        {
            imageWidth = size.getX();
            imageHeight = size.getY();
        }

        autoscale = params.getBooleanAttribute("autoscale", true);
    }

    private void loadMapDimensions()
    {
        final Tuple<Integer, Integer> dimensions = getImageDimensions(resourceLocation);
        mapWidth = dimensions.getA();
        mapHeight = dimensions.getB();
    }

    /**
     * Load and image from a {@link ResourceLocation} and return a {@link Tuple} containing its width and height.
     *
     * @param resourceLocation The {@link ResourceLocation} pointing to the image.
     * @return Width and height.
     */
    public static Tuple<Integer, Integer> getImageDimensions(final ResourceLocation resourceLocation)
    {
        int width = 0;
        int height = 0;

        final Iterator<ImageReader> it = ImageIO.getImageReadersBySuffix("png");
        if (it.hasNext())
        {
            final ImageReader reader = it.next();
            try (
                ImageInputStream stream = ImageIO.createImageInputStream(Minecraft.getInstance().getResourceManager().getResource(resourceLocation).getInputStream()))
            {
                reader.setInput(stream);
                width = reader.getWidth(reader.getMinIndex());
                height = reader.getHeight(reader.getMinIndex());
            }
            catch (final IOException e)
            {
                getLogger().warn(e);
            }
            finally
            {
                reader.dispose();
            }
        }

        return new Tuple<>(width, height);
    }

    /**
     * Set the image.
     *
     * @param source String path.
     */
    public void setImage(final String source)
    {
        setImage(source, 0, 0, 0, 0);
    }

    /**
     * Set the image.
     *
     * @param source  String path.
     * @param offsetX image x offset.
     * @param offsetY image y offset.
     * @param w       image width.
     * @param h       image height.
     */
    public void setImage(final String source, final int offsetX, final int offsetY, final int w, final int h)
    {
        setImage((source != null) ? new ResourceLocation(source) : null, offsetX, offsetY, w, h);
    }

    /**
     * Set the image.
     *
     * @param loc     ResourceLocation for the image.
     * @param offsetX image x offset.
     * @param offsetY image y offset.
     * @param w       image width.
     * @param h       image height.
     */
    public void setImage(final ResourceLocation loc, final int offsetX, final int offsetY, final int w, final int h)
    {
        resourceLocation = loc;
        imageOffsetX = offsetX;
        imageOffsetY = offsetY;
        imageWidth = w;
        imageHeight = h;

        loadMapDimensions();
    }

    /**
     * Set the image.
     *
     * @param loc         ResourceLocation for the image.
     * @param offsetX     image x offset.
     * @param offsetY     image y offset.
     * @param w           image width.
     * @param h           image height.
     * @param customSized is it custom sized.
     */
    public void setImage(final ResourceLocation loc, final int offsetX, final int offsetY, final int w, final int h, final boolean customSized)
    {
        this.customSized = customSized;
        resourceLocation = loc;
        imageOffsetX = offsetX;
        imageOffsetY = offsetY;
        imageWidth = w;
        imageHeight = h;

        loadMapDimensions();
    }

    /**
     * Set the image.
     *
     * @param loc ResourceLocation for the image.
     */
    public void setImage(final ResourceLocation loc)
    {
        setImage(loc, 0, 0, 0, 0);
    }

    /**
     * Draw this image on the GUI.
     *
     * @param mx Mouse x (relative to parent)
     * @param my Mouse y (relative to parent)
     */
    @Override
    public void drawSelf(final int mx, final int my)
    {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

        setupOpenGL(resourceLocation);

        if (this.customSized)
        {
            // /Draw
            drawScaledCustomSizeModalRect(
                x,
                y,
                imageOffsetX,
                imageOffsetY,
                mapWidth,
                mapHeight,
                imageWidth != 0 ? imageWidth : getWidth(),
                imageHeight != 0 ? imageHeight : getHeight(),
                mapWidth,
                mapHeight);
        }
        else
        {
            blit(x, y, imageOffsetX, imageOffsetY, imageWidth != 0 ? imageWidth : getWidth(), imageHeight != 0 ? imageHeight : getHeight());
        }

        RenderSystem.disableBlend();
    }

    @Override
    public void drawSelfLast(final int mx, final int my)
    {

    }

    public static void drawScaledCustomSizeModalRect(
        final int x,
        final int y,
        final float u,
        final float v,
        final int uWidth,
        final int vHeight,
        final int width,
        final int height,
        final float tileWidth,
        final float tileHeight)
    {
        final float f = 1.0F / tileWidth;
        final float f1 = 1.0F / tileHeight;
        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, (y + height), 0.0D).tex((u * f), ((v + (float) vHeight) * f1)).endVertex();
        bufferbuilder.pos((x + width), (y + height), 0.0D).tex(((u + (float) uWidth) * f), ((v + (float) vHeight) * f1)).endVertex();
        bufferbuilder.pos((x + width), y, 0.0D).tex(((u + (float) uWidth) * f), (v * f1)).endVertex();
        bufferbuilder.pos(x, y, 0.0D).tex((u * f), (v * f1)).endVertex();
        tessellator.draw();
    }

    /**
     * Bind texture, set color, and enable blending.
     *
     * @param texture The texture to bind.
     */
    private void setupOpenGL(final ResourceLocation texture)
    {
        this.mc.getTextureManager().bindTexture(texture);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }
}
