package com.ldtteam.structurize.operations;

import com.ldtteam.structurize.util.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Operation for removing one type of block.
 */
public class RemoveBlockOperation extends AreaOperation
{
    /**
     * What type of block to remove.
     */
    private final ItemStack block;

    /**
     * Default constructor.
     *
     * @param startPos the start pos to iterate from.
     * @param endPos   the end pos to iterate to.
     * @param block    what type of block to remove.
     */
    public RemoveBlockOperation(final Player player, final BlockPos startPos, final BlockPos endPos, final ItemStack block)
    {
        super(Component.translatable("com.ldtteam.structurize.remove_block", block.getDisplayName()), player, startPos, endPos);
        this.block = block;
    }

    @Override
    protected void apply(final ServerLevel world, final BlockPos position)
    {
        if (BlockUtils.doBlocksMatch(block, world, position))
        {
            storage.addPreviousDataFor(position, world);
            world.removeBlock(position, false);
            if (!world.getFluidState(position).isEmpty())
            {
                world.setBlock(position, Blocks.AIR.defaultBlockState(), 3);
            }
            storage.addPostDataFor(position, world);
        }
    }
}
