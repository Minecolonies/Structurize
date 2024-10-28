package com.ldtteam.structurize.client;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.ldtteam.structurize.api.Log;
import com.ldtteam.structurize.storage.rendering.types.BlueprintPreviewData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The Blueprint render handler on the client side.
 */
public final class BlueprintHandler
{
    /**
     * A static instance on the client.
     */
    private static final BlueprintHandler ourInstance = new BlueprintHandler();
    /**
     * How long are cache entries valid
     */
    public static final int CACHE_EXPIRE_SECONDS = 45;
    /**
     * How often should cache cleanup happen
     */
    public static final int CACHE_EXPIRE_CHECK_SECONDS = CACHE_EXPIRE_SECONDS / 3;

    private final LoadingCache<RenderingCacheKey, BlueprintRenderer> rendererCache = CacheBuilder.newBuilder()
        .expireAfterAccess(CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
        .<RenderingCacheKey, BlueprintRenderer>removalListener(entry -> entry.getValue().close())
        .build(new CacheLoader<>()
        {
            @Override
            public BlueprintRenderer load(final RenderingCacheKey key)
            {
                return BlueprintRenderer.buildRendererForBlueprint(key.blueprint());
            }
        });

    /**
     * Private constructor to hide public one.
     */
    private BlueprintHandler()
    {
        /*
         * Intentionally left empty.
         */
    }

    /**
     * Get the static instance.
     *
     * @return a static instance of this class.
     */
    public static BlueprintHandler getInstance()
    {
        return ourInstance;
    }

    /**
     * Draw a blueprint at given pos.
     *
     * @param previewData the blueprint and context to draw.
     * @param pos         position to render at
     * @param ctx         rendering event
     */
    public void draw(final BlueprintPreviewData previewData, final BlockPos pos, final RenderLevelStageEvent ctx)
    {
        if (previewData == null || previewData.getBlueprint() == null)
        {
            Log.getLogger().warn("Trying to draw null blueprint!");
            return;
        }
        Minecraft.getInstance().getProfiler().push("struct_render_cache");
        
        rendererCache.getUnchecked(previewData.getRenderKey()).draw(previewData, pos, ctx);

        Minecraft.getInstance().getProfiler().pop();
    }

    /**
     * Cleans entries that are older than CACHE_EVICT_TIME.
     */
    public void cleanCache()
    {
        rendererCache.cleanUp();
    }

    /**
     * Clear all entries.
     */
    public void clearCache()
    {
        rendererCache.invalidateAll();
    }

    /**
     * Draw a blueprint at list of given pos.
     *
     * @param previewData the blueprint and context to draw.
     * @param points      list of positions to render at
     * @param ctx         rendering event
     */
    public void drawAtListOfPositions(final BlueprintPreviewData previewData,
        final Collection<BlockPos> points,
        final RenderLevelStageEvent ctx)
    {
        if (points.isEmpty() || previewData == null || previewData.getBlueprint() == null)
        {
            return;
        }

        Minecraft.getInstance().getProfiler().push("struct_render_multi");

        final BlueprintRenderer renderer = rendererCache.getUnchecked(previewData.getRenderKey());

        for (final BlockPos coord : points)
        {
            renderer.draw(previewData, coord, ctx);
        }

        Minecraft.getInstance().getProfiler().pop();
    }

    /**
     * @return list of entities for instantiated renderer (potentially immediately invalid), else empty list
     */
    public List<Entity> getOptionalEntitiesForBlueprint(final BlueprintPreviewData previewData)
    {
        final BlueprintRenderer renderer = rendererCache.getIfPresent(previewData.getRenderKey());
        return renderer == null ? List.of() : renderer.entities;
    }
}
