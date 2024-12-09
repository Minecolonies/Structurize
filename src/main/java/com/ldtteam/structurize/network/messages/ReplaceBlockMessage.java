package com.ldtteam.structurize.network.messages;

import com.ldtteam.common.network.AbstractServerPlayMessage;
import com.ldtteam.common.network.PlayMessageType;
import com.ldtteam.structurize.api.constants.Constants;
import com.ldtteam.structurize.management.Manager;
import com.ldtteam.structurize.operations.ReplaceBlockOperation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Message to replace a block from the world with another one.
 */
public class ReplaceBlockMessage extends AbstractServerPlayMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forServer(Constants.MOD_ID, "replace_block", ReplaceBlockMessage::new);

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
    protected ReplaceBlockMessage(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        this.from = buf.readBlockPos();
        this.to = buf.readBlockPos();
        this.blockTo = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        this.blockFrom = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
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
        super(TYPE);
        this.from = pos1;
        this.to = pos2;
        this.blockFrom = blockFrom;
        this.blockTo = blockTo;
        this.pct = pct;
    }

    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf)
    {
        buf.writeBlockPos(from);
        buf.writeBlockPos(to);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, blockTo);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, blockFrom);
        buf.writeInt(pct);
    }

    @Override
    protected void onExecute(final IPayloadContext context, final ServerPlayer player)
    {
        if (!player.isCreative())
        {
            return;
        }

        Manager.addToQueue(new ReplaceBlockOperation(player, from, to, blockFrom, blockTo, pct));
    }
}
