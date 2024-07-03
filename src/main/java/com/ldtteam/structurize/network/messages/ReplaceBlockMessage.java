package com.ldtteam.structurize.network.messages;

import com.ldtteam.structurize.management.Manager;
import com.ldtteam.structurize.operations.ReplaceBlockOperation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Message to replace a block from the world with another one.
 */
public class ReplaceBlockMessage implements IMessage
{
    /**
     * Position to scan from.
     */
    private final BlockPos from;

    /**
     * Position to scan to.
     */
    private final BlockPos to;

    /**
     * The block to remove from the world.
     */
    private final ItemStack blockFrom;

    /**
     * The block to remove from the world.
     */
    private final ItemStack blockTo;

    /**
     * Pct of blocks to replace.
     */
    private final int pct;

    /**
     * Empty constructor used when registering the message.
     */
    public ReplaceBlockMessage(final FriendlyByteBuf buf)
    {
        this.from = buf.readBlockPos();
        this.to = buf.readBlockPos();
        this.blockTo = buf.readItem();
        this.blockFrom = buf.readItem();
        this.pct = buf.readInt();
    }

    /**
     * Create a message to replace a block from the world.
     * @param pos1 start coordinate.
     * @param pos2 end coordinate.
     * @param blockFrom the block to replace.
     * @param blockTo the block to replace it with.
     */
    public ReplaceBlockMessage(final BlockPos pos1, final BlockPos pos2, final ItemStack blockFrom, final ItemStack blockTo, final int pct)
    {
        this.from = pos1;
        this.to = pos2;
        this.blockFrom = blockFrom;
        this.blockTo = blockTo;
        this.pct = pct;
    }

    @Override
    public void toBytes(final FriendlyByteBuf buf)
    {
        buf.writeBlockPos(from);
        buf.writeBlockPos(to);
        buf.writeItem(blockTo);
        buf.writeItem(blockFrom);
        buf.writeInt(pct);
    }

    @Nullable
    @Override
    public LogicalSide getExecutionSide()
    {
        return LogicalSide.SERVER;
    }

    @Override
    public void onExecute(final NetworkEvent.Context ctxIn, final boolean isLogicalServer)
    {
        if (!ctxIn.getSender().isCreative())
        {
            return;
        }

        Manager.addToQueue(new ReplaceBlockOperation(ctxIn.getSender(), from, to, blockFrom, blockTo, pct));
    }
}
