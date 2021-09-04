package com.ldtteam.structurize.network.messages;

import com.ldtteam.structurize.Structurize;
import com.ldtteam.structurize.api.util.Log;
import com.ldtteam.structurize.management.Structures;
import com.ldtteam.structurize.util.StructureUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;
import static com.ldtteam.structurize.api.util.constant.Constants.MAX_AMOUNT_OF_PIECES;

/**
 * Save Schematic Message.
 */
public class SchematicSaveMessage implements IMessage
{
    /**
     * The schematic data.
     */
    private final byte[] data;

    /**
     * The amount of pieces.
     */
    private final int pieces;

    /**
     * The current piece.
     */
    private final int piece;

    /**
     * The UUID.
     */
    private final UUID id;

    /**
     * Public standard constructor.
     */
    public SchematicSaveMessage(final PacketBuffer buf)
    {
        final int length = buf.readInt();
        final byte[] compressedData = new byte[length];
        buf.readBytes(compressedData);
        this.data = StructureUtils.uncompress(compressedData);
        this.pieces = buf.readInt();
        this.piece = buf.readInt();
        this.id = buf.readUUID();
    }

    /**
     * Send a schematic to the client.
     *
     * @param data byte array of the schematic.
     */
    /**
     * Send a schematic between client and server or server and client.
     *
     * @param data   the schematic.
     * @param id     the unique id.
     * @param pieces the amount of pieces.
     * @param piece  the current piece.
     */
    public SchematicSaveMessage(final byte[] data, final UUID id, final int pieces, final int piece)
    {
        this.data = data.clone();
        this.id = id;
        this.pieces = pieces;
        this.piece = piece;
    }

    @Override
    public void toBytes(@NotNull final PacketBuffer buf)
    {
        final byte[] compressedData = StructureUtils.compress(data);
        if (compressedData != null)
        {
            buf.capacity(compressedData.length + buf.writerIndex());
            buf.writeInt(compressedData.length);
            buf.writeBytes(compressedData);
            buf.writeInt(pieces);
            buf.writeInt(piece);
            buf.writeUUID(id);
        }
    }

    @Nullable
    @Override
    public LogicalSide getExecutionSide()
    {
        return null;
    }

    @Override
    public void onExecute(final NetworkEvent.Context ctxIn, final boolean isLogicalServer)
    {
        if (isLogicalServer)
        {
            final PlayerEntity sender = ctxIn.getSender();
            final UUID senderUuid = sender.getUUID();

            if (!Structurize.getConfig().getServer().allowPlayerSchematics.get())
            {
                Log.getLogger().info("SchematicSaveMessage: custom schematic is not allowed on this server.");
                sender.sendMessage(new StringTextComponent("The server does not allow custom schematic!"), senderUuid);
                return;
            }

            if (pieces > MAX_AMOUNT_OF_PIECES)
            {
                Log.getLogger().error("Schematic has more than {} pieces, discarding.", MAX_AMOUNT_OF_PIECES);
                sender
                    .sendMessage(new StringTextComponent("Schematic has more than " + MAX_AMOUNT_OF_PIECES + " pieces, that's too big!"), senderUuid);
                return;
            }

            final boolean schematicSent;
            if (data == null)
            {
                Log.getLogger().error("Received empty schematic file");
                schematicSent = false;
            }
            else
            {
                schematicSent = Structures.handleSaveSchematicMessage(data, id, pieces, piece);
            }

            if (schematicSent)
            {
                sender.sendMessage(new StringTextComponent("Schematic successfully sent!"), senderUuid);
            }
            else
            {
                sender.sendMessage(new StringTextComponent("Failed to send the Schematic!"), senderUuid);
            }
        }
        else
        {
            if (data == null)
            {
                Log.getLogger().error("Received empty schematic file");
            }
            else
            {
                Structures.handleSaveSchematicMessage(data, false);
            }
        }
    }
}
