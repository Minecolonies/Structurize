package com.ldtteam.structurize.client;

import com.ldtteam.structurize.Structurize;
import com.ldtteam.structurize.blockentities.BlockEntityTagSubstitution;
import com.ldtteam.structurize.blocks.ModBlocks;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.blueprints.v1.BlueprintUtils;
import com.ldtteam.structurize.client.fakelevel.BlueprintBlockAccess;
import com.ldtteam.structurize.component.CapturedBlock;
import com.ldtteam.structurize.storage.rendering.types.BlueprintPreviewData;
import com.ldtteam.structurize.tag.ModTags;
import com.ldtteam.structurize.util.BlockInfo;
import com.ldtteam.structurize.util.BlueprintMissHitResult;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexBuffer.Usage;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.CrashReport;
import net.minecraft.ReportType;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The renderer for blueprint.
 * Holds all information required to render a blueprint.
 */
public class BlueprintRenderer implements AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintRenderer.class);

    private static final RenderBuffers renderBuffers = new RenderBuffers(0);
    private static boolean hasWarnedExceptions = false;

    private final BlueprintBlockAccess blockAccess;
    private List<Entity> entities;
    private List<BlockEntity> tileEntities;
    private Map<RenderType, VertexBuffer> vertexBuffers;
    private long lastGameTime;
    private boolean bypassMainFrustum = false;
    private Set<Object> crashingObjects = Collections.newSetFromMap(new IdentityHashMap<>());

    /**
     * Static factory utility method to handle the extraction of the values from the blueprint.
     *
     * @param blueprint The blueprint to create an instance for.
     * @return The renderer.
     */
    public static BlueprintRenderer buildRendererForBlueprint(final Blueprint blueprint)
    {
        final BlueprintBlockAccess blockAccess = new BlueprintBlockAccess(blueprint);
        return new BlueprintRenderer(blockAccess);
    }

    private BlueprintRenderer(final BlueprintBlockAccess blockAccess)
    {
        this.blockAccess = blockAccess;
    }

    /**
     * Updates blueprint reference if it has same hash.
     *
     * @param previewData blueprint and context from active structure
     */
    public void updateBlueprint(final BlueprintPreviewData previewData)
    {
        if (blockAccess.getLevelSource() != previewData.getBlueprint() && blockAccess.getLevelSource().hashCode() == previewData.getBlueprint().hashCode())
        {
            blockAccess.setLevelSource(previewData.getBlueprint());
        }
    }

    private void init(final Blueprint blueprint, final Map<Object, Exception> suppressedExceptions)
    {
        final BlockRenderDispatcher blockRenderer = Minecraft.getInstance().getBlockRenderer();
        final RandomSource random = RandomSource.create();

        final Map<BlockPos, ModelData> teModelData = new HashMap<>();
        final Map<BlockPos, BlockEntity> tileEntitiesMap = BlueprintUtils.instantiateTileEntities(blueprint, blockAccess, teModelData);
        entities = BlueprintUtils.instantiateEntities(blueprint, blockAccess);

        blockAccess.setBlockEntities(tileEntitiesMap);
        blockAccess.setEntities(entities);

        final PoseStack matrixStack = new PoseStack();
        matrixStack.translate(0.001, 0.001, 0.001);

        final ChunkOffsetBufferBuilderWrapper fluidBufferWrapper = new ChunkOffsetBufferBuilderWrapper();
        final Map<RenderType, BufferBuilder> chunkBuffers = new Reference2ObjectArrayMap<>(RenderType.chunkBufferLayers().size());
        RenderType.chunkBufferLayers().forEach(type -> chunkBuffers.put(type, new BufferBuilder(renderBuffers.fixedBufferPack().buffer(type), type.mode(), type.format())));

        for (final BlockInfo blockInfo : blueprint.getBlockInfoAsList())
        {
            final BlockPos blockPos = blockInfo.getPos();
            BlockState state = blockInfo.getState();
            // specially handle blockTagSub here cuz of block entity changes
            if (Structurize.getConfig().getClient().renderPlaceholdersNice.get() && state.getBlock() == ModBlocks.blockTagSubstitution.get())
            {
                if (tileEntitiesMap.remove(blockPos) instanceof final BlockEntityTagSubstitution tagTE)
                {
                    final CapturedBlock replacement = tagTE.getReplacement();
                    state = replacement.blockState();

                    replacement.serializedBE().map(tag -> BlockEntity.loadStatic(blockPos, replacement.blockState(), tag, blueprint.getRegistryAccess())).ifPresent(newBe -> {
                        newBe.setLevel(blockAccess);
                        teModelData.put(blockPos, newBe.getModelData());
                        tileEntitiesMap.put(blockPos, newBe);
                    });
                }
                else
                {
                    state = Blocks.AIR.defaultBlockState();
                }
            }
            else
            {
                state = blockAccess.prepareBlockStateForRendering(state, blockPos);
            }

            final FluidState fluidState = state.getFluidState();
            try
            {
                if (!fluidState.isEmpty())
                {
                    final RenderType renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);

                    final int chunkOffsetX = blockPos.getX() - (blockPos.getX() & 15),
                        chunkOffsetY = blockPos.getY() - (blockPos.getY() & 15),
                        chunkOffsetZ = blockPos.getZ() - (blockPos.getZ() & 15);

                    fluidBufferWrapper.setOffset(chunkBuffers.get(renderType), chunkOffsetX, chunkOffsetY, chunkOffsetZ);
                    blockRenderer.renderLiquid(blockPos, blockAccess, fluidBufferWrapper, state, fluidState);
                }

                if (state.getRenderShape() != RenderShape.INVISIBLE)
                {
                    final BakedModel blockModel = blockRenderer.getBlockModel(state);
                    final ModelData modelData = blockModel.getModelData(blockAccess, blockPos, state, teModelData.getOrDefault(blockPos, ModelData.EMPTY));

                    matrixStack.pushPose();
                    matrixStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());

                    for (final RenderType renderType : blockModel.getRenderTypes(state, random, modelData))
                    {
                        final BufferBuilder buffer = chunkBuffers.get(renderType);
                        blockRenderer.renderBatched(state, blockPos, blockAccess, matrixStack, buffer, true, random, modelData, renderType);
                        renderType.clearRenderState();
                    }
                    matrixStack.popPose();
                }

            }
            catch (final ReportedException e)
            {
                suppressedExceptions.put(blockInfo, e);
            }
        }

        clearVertexBuffers();
        vertexBuffers = new Reference2ObjectArrayMap<>(RenderType.chunkBufferLayers().size());
        chunkBuffers.forEach((type, buffer) -> {
            final MeshData meshData = buffer.build();
            if (meshData != null)
            {
                final VertexBuffer vertexBuffer = new VertexBuffer(Usage.STATIC);
                vertexBuffer.bind();
                vertexBuffer.upload(meshData);
                vertexBuffers.put(type, vertexBuffer);
            }
        });
        VertexBuffer.unbind();

        tileEntities = new ArrayList<>(tileEntitiesMap.values());
    }

    /**
     * Draws structure into world.
     */
    public void draw(final BlueprintPreviewData previewData, final BlockPos pos, final RenderLevelStageEvent ctx)
    {
        // we've crashed hard before, full skip
        if (crashingObjects == null)
        {
            return;
        }

        try
        {
            final Map<Object, Exception> suppressedExceptions = drawUnsafe(previewData, pos, ctx);
            if (!suppressedExceptions.isEmpty())
            {
                if (!hasWarnedExceptions)
                {
                    hasWarnedExceptions = true;
                    Minecraft.getInstance().player.sendSystemMessage(Component.translatable("structurize.preview_renderer.exception"));
                }

                boolean crashReported = false;
                boolean isEmpty = true;
                for (final Map.Entry<Object, Exception> e : suppressedExceptions.entrySet())
                {
                    if (!crashingObjects.add(e.getKey()))
                    {
                        continue;
                    }
                    isEmpty = false;

                    if (e.getValue() instanceof final ReportedException reportedException)
                    {
                        printCrashReport(reportedException.getReport(), previewData);
                        crashReported = true;
                    }
                    else
                    {
                        LOGGER.error("", e.getValue());
                    }
                }

                if (!crashReported && !isEmpty)
                {
                    printCrashReport(CrashReport.forThrowable(new Exception(), "Small exception, rendering partially"), previewData);
                }
            }
        }
        catch (final Exception e)
        {
            printCrashReport(CrashReport.forThrowable(e, "Fatal exception, cannot render"), previewData);

            crashingObjects = null;
            Minecraft.getInstance().player.sendSystemMessage(
                Component.translatable("structurize.preview_renderer.cannot_render", previewData.getBlueprint().getName()));
        }
    }

    private static void printCrashReport(final CrashReport report, final BlueprintPreviewData previewData)
    {
        previewData.getBlueprint().describeSelfInCrashReport(report.addCategory("Blueprint"));
        LOGGER.error(report.getFriendlyReport(new ReportType("Problem during blueprint rendering", ReportType.TEST.nuggets())));
    }

    /**
     * Draws structure into world.
     * 
     * @return suppressed exceptions
     */
    public Map<Object, Exception> drawUnsafe(final BlueprintPreviewData previewData, final BlockPos pos, final RenderLevelStageEvent ctx)
    {
        final BlockPos anchorPos = pos.subtract(previewData.getBlueprint().getPrimaryBlockOffset());

        // cull entire rendering
        if (!ctx.getFrustum().isVisible(previewData.getBlueprint().getAABB().move(anchorPos)) && !bypassMainFrustum)
        {
            return Map.of();
        }
     
        final Map<Object, Exception> suppressedExceptions = new IdentityHashMap<>();
        final Minecraft mc = Minecraft.getInstance();
        final long gameTime = mc.level.getGameTime();
        final PoseStack matrixStack = ctx.getPoseStack();
        final DeltaTracker deltaTracker = ctx.getPartialTick();
        final ProfilerFiller profiler = mc.getProfiler();
        final Matrix4f mvMatrix = ctx.getModelViewMatrix();
        final Matrix4f pMatrix = ctx.getProjectionMatrix();

        profiler.push("struct_render_init");
        
        // make sure instances are synced
        updateBlueprint(previewData);
        blockAccess.setWorldPos(anchorPos);

        // init
        if (vertexBuffers == null)
        {
            init(previewData.getBlueprint(), suppressedExceptions);
        }

        profiler.popPush("struct_render_prepare");
        final Vec3 viewPosition = ctx.getCamera().getPosition();
        final Vec3 realRenderRootVecd = Vec3.atLowerCornerOf(anchorPos).subtract(viewPosition);
        final Vector3f realRenderRootVecf = realRenderRootVecd.toVector3f();

        final float partialTicks;
        {
            final Entity entity = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();
            partialTicks = mc.level.tickRateManager().isEntityFrozen(entity) ? 1.0F : deltaTracker.getGameTimeDeltaPartialTick(!mc.level.tickRateManager().isFrozen());
        }

        // cache old dispatchers
        final Level dispLevel = mc.getBlockEntityRenderDispatcher().level; // they are same for both anyway
        final Camera dispCamera = mc.getBlockEntityRenderDispatcher().camera; // also same
        final HitResult beHitResult = mc.getBlockEntityRenderDispatcher().cameraHitResult;
        final Entity ePickEntity = mc.getEntityRenderDispatcher().crosshairPickEntity;

        final Camera ourCamera = new Camera();
        ourCamera.setup(blockAccess,
            dispCamera.getEntity(),
            !mc.options.getCameraType().isFirstPerson(),
            mc.options.getCameraType().isMirrored(),
            partialTicks);
        ourCamera.setPosition(viewPosition.subtract(anchorPos.getX(), anchorPos.getY(), anchorPos.getZ()));

        mc.getBlockEntityRenderDispatcher().prepare(blockAccess, ourCamera, BlueprintMissHitResult.MISS);
        mc.getEntityRenderDispatcher().prepare(blockAccess, ourCamera, mc.crosshairPickEntity);

        final Frustum blueprintLocalFrustum = new Frustum(ctx.getFrustum());
        blueprintLocalFrustum.prepare(ourCamera.getPosition().x(), ourCamera.getPosition().y(), ourCamera.getPosition().z());
        bypassMainFrustum = false;

        // missing chunk system! else done?

        if (mc.level.effects().constantAmbientLight())
        {
            Lighting.setupNetherLevel();
        }
        else
        {
            Lighting.setupLevel();
        }

        // Render blocks

        if (ctx.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL)
        {
            FogRenderer.setupFog(ctx.getCamera(),
                FogRenderer.FogMode.FOG_TERRAIN,
                Math.max(mc.gameRenderer.getRenderDistance(), 32.0F),
                mc.level.effects().isFoggyAt(Mth.floor(viewPosition.x()), Mth.floor(viewPosition.y()))
                    || mc.gui.getBossOverlay().shouldCreateWorldFog(),
                partialTicks);
        }

        profiler.popPush("struct_render_blocks");
        renderBlockLayer(RenderType.solid(), mvMatrix, pMatrix, realRenderRootVecf, previewData, mc);
        // FORGE: fix flickering leaves when mods mess up the blurMipmap settings
        mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).setBlurMipmap(false, mc.options.mipmapLevels().get() > 0);
        renderBlockLayer(RenderType.cutoutMipped(), mvMatrix, pMatrix, realRenderRootVecf, previewData, mc);
        mc.getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).restoreLastBlurMipmap();
        renderBlockLayer(RenderType.cutout(), mvMatrix, pMatrix, realRenderRootVecf, previewData, mc);

        profiler.popPush("struct_render_entities");
        final MultiBufferSource.BufferSource renderBufferSource = renderBuffers.bufferSource();

        // Entities

        matrixStack.pushPose();
        matrixStack.translate(realRenderRootVecd.x(), realRenderRootVecd.y(), realRenderRootVecd.z());
        for (final Entity entity : entities)
        {
            if (!mc.getEntityRenderDispatcher()
                .shouldRender(entity,
                    blueprintLocalFrustum,
                    ourCamera.getPosition().x(),
                    ourCamera.getPosition().y(),
                    ourCamera.getPosition().z()))
            {
                continue;
            }

            if (gameTime != lastGameTime && entity.getType().is(ModTags.PREVIEW_TICKING_ENTITIES))
            {
                try
                {
                    entity.tick();
                }
                catch (final Exception e)
                {
                    // well, noop
                    suppressedExceptions.put(entity, e);
                }
            }

            bypassMainFrustum |= entity.noCulling;
            try
            {
                mc.getEntityRenderDispatcher().render(entity,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    entity.getYRot(),
                    partialTicks,
                    matrixStack,
                    renderBufferSource,
                    mc.getEntityRenderDispatcher().getPackedLightCoords(entity, partialTicks));
            }
            catch (final ClassCastException e)
            {
                // Oops
                suppressedExceptions.put(entity, e);
            }
        }
        matrixStack.popPose();

        profiler.popPush("struct_render_entities_finish");
        renderBufferSource.endLastBatch();
        renderBufferSource.endBatch(RenderType.entitySolid(InventoryMenu.BLOCK_ATLAS));
        renderBufferSource.endBatch(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        renderBufferSource.endBatch(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        renderBufferSource.endBatch(RenderType.entitySmoothCutout(InventoryMenu.BLOCK_ATLAS));

        // Block entities

        profiler.popPush("struct_render_blockentities");
        for (final BlockEntity tileEntity : tileEntities)
        {
            final BlockEntityRenderer<BlockEntity> renderer = mc.getBlockEntityRenderDispatcher().getRenderer(tileEntity);
            if (renderer == null || !renderer.shouldRender(tileEntity, ourCamera.getPosition()))
            {
                continue;
            }

            final BlockPos tePos = tileEntity.getBlockPos();
            final Vec3 realRenderTePos = realRenderRootVecd.add(tePos.getX(), tePos.getY(), tePos.getZ());

            if (gameTime != lastGameTime)
            {
                // hooks from EntityBlock#getTicker(Level, BlockState, BlockEntityType) for client side
                // either mc.level and anchorPos - particles, player distance etc.
                // or blockAccess and tePos - blueprint neighborhood
                if (tileEntity instanceof final SpawnerBlockEntity spawner)
                {
                    SpawnerBlockEntity.clientTick(mc.level, anchorPos.offset(tePos), blockAccess.getBlockState(tePos), spawner);
                }
                else if (tileEntity instanceof final EnchantingTableBlockEntity enchTable)
                {
                    EnchantingTableBlockEntity
                        .bookAnimationTick(mc.level, anchorPos.offset(tePos), blockAccess.getBlockState(tePos), enchTable);
                }
                else if (tileEntity instanceof final CampfireBlockEntity campfire)
                {
                    final BlockState bs = blockAccess.getBlockState(tePos);
                    if (bs.getBlock() instanceof CampfireBlock && bs.getValue(CampfireBlock.LIT))
                    {
                        CampfireBlockEntity.particleTick(mc.level, anchorPos.offset(tePos), bs, campfire);
                    }
                }
                else if (tileEntity instanceof final SkullBlockEntity skull)
                {
                    final BlockState bs = blockAccess.getBlockState(tePos);
                    if (bs.getBlock() instanceof SkullBlock && (bs.is(Blocks.DRAGON_HEAD) || bs.is(Blocks.DRAGON_WALL_HEAD) ||
                        bs.is(Blocks.PIGLIN_HEAD) ||
                        bs.is(Blocks.PIGLIN_WALL_HEAD)))
                    {
                        SkullBlockEntity.animation(blockAccess, tePos, bs, skull);
                    }
                }
                else if (tileEntity instanceof final BeaconBlockEntity beacon)
                {
                    // uses sound and applies buffs, but we dont want any of this since we're preview
                    BeaconBlockEntity.tick(blockAccess, tePos, blockAccess.getBlockState(tePos), beacon);
                }
                else if (tileEntity instanceof final VaultBlockEntity vault)
                {
                    VaultBlockEntity.Client.tick(mc.level, anchorPos.offset(tePos), blockAccess.getBlockState(tePos), vault.getClientData(), vault.getSharedData());
                }
                else if (tileEntity instanceof final TrialSpawnerBlockEntity trialSpawner)
                {
                    trialSpawner.getTrialSpawner().tickClient(mc.level, anchorPos.offset(tePos), blockAccess.getBlockState(tePos).getOptionalValue(TrialSpawnerBlock.OMINOUS).orElse(false));
                }
            }

            bypassMainFrustum |= renderer.shouldRenderOffScreen(tileEntity);
            if (!blueprintLocalFrustum.isVisible(renderer.getRenderBoundingBox(tileEntity)) && !renderer.shouldRenderOffScreen(tileEntity))
            {
                continue;
            }

            matrixStack.pushPose();
            matrixStack.translate(realRenderTePos.x, realRenderTePos.y, realRenderTePos.z);

            mc.getBlockEntityRenderDispatcher().render(tileEntity, partialTicks, matrixStack, renderBufferSource);
            matrixStack.popPose();
        }

        profiler.popPush("struct_render_blockentities_finish");
        renderBufferSource.endBatch(RenderType.solid());
        renderBufferSource.endBatch(RenderType.endPortal());
        renderBufferSource.endBatch(RenderType.endGateway());
        renderBufferSource.endBatch(Sheets.solidBlockSheet());
        renderBufferSource.endBatch(Sheets.cutoutBlockSheet());
        renderBufferSource.endBatch(Sheets.bedSheet());
        renderBufferSource.endBatch(Sheets.shulkerBoxSheet());
        renderBufferSource.endBatch(Sheets.signSheet());
        renderBufferSource.endBatch(Sheets.hangingSignSheet());
        renderBufferSource.endBatch(Sheets.chestSheet());
        renderBuffers.outlineBufferSource().endOutlineBatch(); // not used now

        renderBufferSource.endLastBatch();
        renderBufferSource.endBatch(Sheets.translucentCullBlockSheet());
        renderBufferSource.endBatch(Sheets.bannerSheet());
        renderBufferSource.endBatch(Sheets.shieldSheet());
        renderBufferSource.endBatch(RenderType.armorEntityGlint());
        renderBufferSource.endBatch(RenderType.glint());
        renderBufferSource.endBatch(RenderType.glintTranslucent());
        renderBufferSource.endBatch(RenderType.entityGlint());
        renderBufferSource.endBatch(RenderType.entityGlintDirect());
        renderBufferSource.endBatch(RenderType.waterMask());
        renderBuffers.crumblingBufferSource().endBatch(); // not used now

        profiler.popPush("struct_render_blocks2");
        renderBlockLayer(RenderType.translucent(), mvMatrix, pMatrix, realRenderRootVecf, previewData, mc);

        renderBufferSource.endBatch(RenderType.lines());
        renderBufferSource.endBatch();
        renderBlockLayer(RenderType.tripwire(), mvMatrix, pMatrix, realRenderRootVecf, previewData, mc);

        RenderSystem.applyModelViewMatrix(); // ensure no polution
        Lighting.setupLevel();
        if (ctx.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL)
        {
            FogRenderer.setupNoFog();
        }

        // restore vanilla setup
        mc.getBlockEntityRenderDispatcher().prepare(dispLevel, dispCamera, beHitResult);
        mc.getEntityRenderDispatcher().prepare(dispLevel, dispCamera, ePickEntity);

        lastGameTime = gameTime;
        profiler.pop();

        return suppressedExceptions;
    }

    /**
     * Clears GL references and frees GL objects.
     */
    private void clearVertexBuffers()
    {
        if (vertexBuffers != null)
        {
            vertexBuffers.values().forEach(VertexBuffer::close);
            vertexBuffers = null;
        }
    }

    @Override
    public void close()
    {
        clearVertexBuffers();
    }

    private void renderBlockLayer(final RenderType layerRenderType, final Matrix4f mvMatrix, final Matrix4f pMatrix, final Vector3f realRenderRootPos, final BlueprintPreviewData previewData, final Minecraft mc)
    {
        final VertexBuffer vertexBuffer = vertexBuffers.get(layerRenderType);
        if (vertexBuffer == null)
        {
            return;
        }

        layerRenderType.setupRenderState();

        final ShaderInstance shaderinstance = RenderSystem.getShader();
        shaderinstance.setDefaultUniforms(VertexFormat.Mode.QUADS, mvMatrix, pMatrix, mc.getWindow());
        shaderinstance.apply();

        final Uniform uniform = shaderinstance.CHUNK_OFFSET;
        if (uniform != null)
        {
            uniform.set(realRenderRootPos);
            uniform.upload();
        }

        TransparencyHack.apply(previewData.getOverridePreviewTransparency());

        vertexBuffer.bind();
        vertexBuffer.draw();

        TransparencyHack.reset();

        if (uniform != null)
        {
            uniform.set(0f, 0, 0);
        }

        shaderinstance.clear();

        VertexBuffer.unbind();
        layerRenderType.clearRenderState();
    }

    /**
     * Assuming there's no blend function active let's take advantage of OpenGL blend color constant
     * which doesnt require any shader changes at all.
     * More info at: https://registry.khronos.org/OpenGL-Refpages/gl4/html/glBlendColor.xhtml
     */
    public static class TransparencyHack
    {
        public static final float THRESHOLD = 0.99f;
        protected static boolean applied = false;

        public static void apply(final float overrideValue)
        {
            if (applied || GlStateManager.BLEND.mode.enabled)
            {
                // do not override if there is running blend fnc
                return;
            }

            float alpha = Structurize.getConfig().getClient().rendererTransparency.get().floatValue();
            if (alpha < 0 || alpha > THRESHOLD)
            {
                return;
            }
            alpha = overrideValue < 0 ? alpha : Mth.clamp(overrideValue, 0, 1);

            applied = true;

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SourceFactor.CONSTANT_ALPHA, DestFactor.ONE_MINUS_CONSTANT_ALPHA);
            GL20C.glBlendColor(0, 0, 0, alpha);
        }

        public static void reset()
        {
            if (!applied)
            {
                return;
            }

            applied = false;

            RenderSystem.disableBlend();
        }
    }
}
