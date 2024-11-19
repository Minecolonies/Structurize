package com.ldtteam.structurize.network.messages;

import com.ldtteam.common.network.AbstractClientPlayMessage;
import com.ldtteam.common.network.PlayMessageType;
import com.ldtteam.structurize.api.constants.Constants;
import com.ldtteam.structurize.storage.ClientStructurePackLoader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Transfer a zipped structure pack to the client.
 */
public class TransferStructurePackToClient extends AbstractClientPlayMessage
{
    public static final PlayMessageType<?> TYPE = PlayMessageType.forClient(Constants.MOD_ID, "transfer_structure_pack_to_client", TransferStructurePackToClient::new);

    /**
     * Payload of the message (to transfer to client).
     */
    private final ByteBuf payload;

    /**
     * The name of the structure pack.
     */
    private final String packname;

    /**
     * If the last message.
     */
    private final boolean eol;

    /**
     * Public standard constructor.
     */
    protected TransferStructurePackToClient(final RegistryFriendlyByteBuf buf, final PlayMessageType<?> type)
    {
        super(buf, type);
        this.packname = buf.readUtf(32767);
        this.eol = buf.readBoolean();
        final int size = buf.readInt();
        this.payload = Unpooled.wrappedBuffer(buf.readBytes(size));
    }

    /**
     * Transfer a zipped structure pack to the client.
     * @param packName the name of the structure pack.
     * @param payload the payload.
     * @param eol if last message to client.
     */
    public TransferStructurePackToClient(final String packName, final ByteBuf payload, final boolean eol)
    {
        super(TYPE);
        this.packname = packName;
        this.payload = payload;
        this.eol = eol;
    }

    @Override
    protected void toBytes(final RegistryFriendlyByteBuf buf)
    {
        buf.writeUtf(this.packname);
        buf.writeBoolean(this.eol);
        final int size = this.payload.readableBytes();
        buf.writeInt(size);
        buf.writeBytes(this.payload, size);
        this.payload.resetReaderIndex();
    }

    @Override
    protected void onExecute(final IPayloadContext context, final Player player)
    {
        ClientStructurePackLoader.onStructurePackTransfer(this.packname, this.payload, this.eol);
    }
}
