package com.ldtteam.structurize.component;

import com.ldtteam.structurize.api.RotationMirror;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * @param blockState   state of captured block
 * @param serializedBE related blockEntity data if needed
 * @param itemStack    itemStack representing both block and blockEntity
 */
public record CapturedBlock(BlockState blockState, Optional<CompoundTag> serializedBE, ItemStack itemStack)
{
    public static final CapturedBlock EMPTY = new CapturedBlock(Blocks.AIR.defaultBlockState(), Optional.empty(), ItemStack.EMPTY);

    public static final Codec<CapturedBlock> CODEC = RecordCodecBuilder.create(
        builder -> builder
            .group(BlockState.CODEC.fieldOf("state").forGetter(CapturedBlock::blockState),
                CompoundTag.CODEC.optionalFieldOf("entity").forGetter(CapturedBlock::serializedBE),
                ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(CapturedBlock::itemStack))
            .apply(builder, CapturedBlock::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CapturedBlock> STREAM_CODEC =
        StreamCodec.composite(ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY),
            CapturedBlock::blockState,
            ByteBufCodecs.OPTIONAL_COMPOUND_TAG,
            CapturedBlock::serializedBE,
            ItemStack.STREAM_CODEC,
            CapturedBlock::itemStack,
            CapturedBlock::new);

    /**
     * Serializes given BE.
     *
     * @param blockState  state of captured block
     * @param blockEntity related blockEntity data if needed
     * @param provider    registry access
     * @param itemStack   itemStack representing both block and blockEntity
     */
    public CapturedBlock(final BlockState blockState,
        @Nullable final BlockEntity blockEntity,
        final HolderLookup.Provider provider,
        final ItemStack itemStack)
    {
        this(blockState, blockEntity == null ? Optional.empty() : Optional.of(blockEntity.saveWithId(provider)), itemStack);
    }

    /**
     * @param rotationMirror relative rotation and mirror
     * @param level          registry access
     */
    public CapturedBlock applyRotationMirror(final RotationMirror rotationMirror, final Level level)
    {
        if (serializedBE.isEmpty())
        {
            return new CapturedBlock(rotationMirror.applyToBlockState(blockState), serializedBE, itemStack);
        }

        final Blueprint blueprint = new Blueprint((short) 1, (short) 1, (short) 1, level.registryAccess());
        blueprint.addBlockState(BlockPos.ZERO, blockState);
        blueprint.getTileEntities()[0][0][0] = serializedBE.get();
        blueprint.setCachePrimaryOffset(BlockPos.ZERO);
        blueprint.setRotationMirrorRelative(rotationMirror, level);

        return new CapturedBlock(blueprint.getPalette()[blueprint.getPalleteSize()],
            Optional.of(blueprint.getTileEntities()[0][0][0]),
            itemStack);
    }

    public boolean hasBlockEntity()
    {
        return serializedBE.isPresent() && !serializedBE.get().isEmpty();
    }

    /**
     * Writes this posSelection into given itemStack.
     * 
     * @see BlockEntity#saveToItem(ItemStack, net.minecraft.core.HolderLookup.Provider)
     */
    public void writeToItemStack(final ItemStack itemStack)
    {
        itemStack.set(ModDataComponents.CAPTURED_BLOCK, this);
    }

    /**
     * @return posSelection stored in given itemStack (or empty instance)
     */
    public static CapturedBlock readFromItemStack(final ItemStack itemStack)
    {
        return itemStack.getOrDefault(ModDataComponents.CAPTURED_BLOCK, CapturedBlock.EMPTY);
    }

    /**
     * Performs updating of posSelection in given itemStack
     */
    public static void updateItemStack(final ItemStack itemStack, final UnaryOperator<CapturedBlock> updater)
    {
        updater.apply(readFromItemStack(itemStack)).writeToItemStack(itemStack);
    }
}
