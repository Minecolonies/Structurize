package com.ldtteam.structurize.client;

import com.ldtteam.common.fakelevel.SingleBlockFakeLevel;
import com.ldtteam.structurize.blockentities.BlockEntityTagSubstitution;
import com.ldtteam.structurize.component.CapturedBlock;
import com.ldtteam.structurize.items.ItemTagSubstitution;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.NeoForgeRenderTypes;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

/**
 * Tag anchor renderer; renders replacement block model inside of anchor "overlay" model.
 */
public class TagSubstitutionRenderer extends BlockEntityWithoutLevelRenderer implements BlockEntityRenderer<BlockEntityTagSubstitution>
{
    private static TagSubstitutionRenderer INSTANCE;

    public static TagSubstitutionRenderer getInstance()
    {
        return INSTANCE;
    }


    private final BlockEntityRendererProvider.Context context;
    private SingleBlockFakeLevel renderLevel;

    public TagSubstitutionRenderer(@NotNull final BlockEntityRendererProvider.Context context)
    {
        super(context.getBlockEntityRenderDispatcher(), context.getModelSet());

        INSTANCE = this;
        this.context = context;
    }

    @Override
    public void render(@NotNull final BlockEntityTagSubstitution entity,
                       final float partialTick,
                       @NotNull final PoseStack poseStack,
                       @NotNull final MultiBufferSource buffers,
                       final int packedLight,
                       final int packedOverlay)
    {
        final RenderType renderType = NeoForgeRenderTypes.ITEM_LAYERED_TRANSLUCENT.get();

        render(entity.getReplacement(), entity.getTilePos(), partialTick, poseStack, buffers, packedLight, packedOverlay, renderType);
    }

    @Override
    public void renderByItem(@NotNull final ItemStack stack,
                             @NotNull final ItemDisplayContext transformType,
                             @NotNull final PoseStack poseStack,
                             @NotNull final MultiBufferSource buffers,
                             final int packedLight,
                             final int packedOverlay)
    {
        final RenderType renderType = NeoForgeRenderTypes.ITEM_LAYERED_TRANSLUCENT.get();

        if (stack.getItem() instanceof ItemTagSubstitution anchor)
        {
            this.context.getBlockRenderDispatcher().renderSingleBlock(anchor.getBlock().defaultBlockState(),
                    poseStack, buffers, packedLight, packedOverlay, ModelData.EMPTY, renderType);

            render(CapturedBlock.readFromItemStack(stack), BlockPos.ZERO, 0, poseStack, buffers, packedLight, packedOverlay, renderType);
        }
    }

    private void render(@NotNull final CapturedBlock replacement,
                        @NotNull final BlockPos pos,
                        final float partialTick,
                        @NotNull PoseStack poseStack,
                        @NotNull MultiBufferSource buffers,
                        final int packedLight,
                        final int packedOverlay,
                        @NotNull final RenderType renderType)
    {
        if (replacement.blockState().isAir())
        {
            return;
        }

        poseStack.pushPose();
        poseStack.scale(0.995f, 0.995f, 0.995f);
        poseStack.translate(0.0025f, 0.0025f, 0.0025f);

        if (replacement.hasBlockEntity())
        {
            final BlockEntityRenderDispatcher entityDispatcher = this.context.getBlockEntityRenderDispatcher();
            final Level realLevel = entityDispatcher.level;
            if (renderLevel == null)
            {
                renderLevel = new SingleBlockFakeLevel(realLevel);
            }

            renderLevel.withFakeLevelContext(replacement.blockState(),
                BlockEntity.loadStatic(BlockPos.ZERO, replacement.blockState(), replacement.serializedBE().get(), realLevel.registryAccess()),
                realLevel,
                fakeLevel -> {
                    context.getBlockRenderDispatcher()
                        .renderSingleBlock(replacement.blockState(),
                            poseStack,
                            buffers,
                            packedLight,
                            packedOverlay,
                            renderLevel.getLevelSource().blockEntity.getModelData(),
                            renderType);
                    entityDispatcher.render(renderLevel.getLevelSource().blockEntity, partialTick, poseStack, buffers);
                });
        }
        else
        {
            context.getBlockRenderDispatcher()
                .renderSingleBlock(replacement.blockState(), poseStack, buffers, packedLight, packedOverlay, ModelData.EMPTY, renderType);
        }

        poseStack.popPose();
    }
}
