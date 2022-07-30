package com.ldtteam.structurize.commands;

import com.ldtteam.structurize.items.ItemScanTool;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static com.ldtteam.structurize.api.util.constant.Constants.MOD_ID;

/**
 * Command for opening WindowScanTool or scanning a structure into a file
 */
public class ScanCommand extends AbstractCommand
{
    /**
     * Descriptive string.
     */
    public static final String NAME = "scan";

    /**
     * The player not found for a scan message.
     */
    private static final String PLAYER_NOT_FOUND = "com.structurize.command.playernotfound";

    /**
     * The scan success message.
     */
    private static final String SCAN_SUCCESS_MESSAGE = "com.structurize.command.scan.success";

    /**
     * The no permission message reply.
     */
    private static final String NO_PERMISSION_MESSAGE = "com.structurize.command.scan.no.perm";

    /**
     * The filename command argument.
     */
    public static final String FILE_NAME = "filename";

    /**
     * The player name command argument.
     */
    public static final String PLAYER_NAME = "player";

    /**
     * Position 1 command argument.
     */
    public static final String POS1 = "pos1";

    /**
     * Position 2 command argument.
     */
    public static final String POS2 = "pos2";

    /**
     * Anchor position command argument.
     */
    public static final String ANCHOR_POS = "anchor_pos";

    private static int execute(final CommandSourceStack source, final BlockPos from, final BlockPos to, final Optional<BlockPos> anchorPos, final GameProfile profile, final String name) throws CommandSyntaxException
    {
        @Nullable final Level world = source.getLevel();
        if (source.getEntity() instanceof Player && !source.getPlayerOrException().isCreative())
        {
            source.sendFailure(new TextComponent(NO_PERMISSION_MESSAGE));
        }

        final Player player;
        if (profile != null && world.getServer() != null)
        {
            player = world.getServer().getPlayerList().getPlayer(profile.getId());
            if (player == null)
            {
                source.sendFailure(new TranslatableComponent(PLAYER_NOT_FOUND, profile.getName()));
                return 0;
            }
        } 
        else if (source.getEntity() instanceof Player)
        {
            player = source.getPlayerOrException();
        } 
        else
        {
            source.sendFailure(new TranslatableComponent(PLAYER_NOT_FOUND));
            return 0;
        }

        ItemScanTool.saveStructure(world, from, to, player, name == null ? "" : name, true, anchorPos);
        source.sendFailure(new TranslatableComponent(SCAN_SUCCESS_MESSAGE));
        return 1;
    }

    private static int onExecute(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final BlockPos from = BlockPosArgument.getSpawnablePos(context, POS1);
        final BlockPos to = BlockPosArgument.getSpawnablePos(context, POS2);
        return execute(context.getSource(), from, to, Optional.empty(), null, null);
    }

    private static int onExecuteWithAnchor(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final BlockPos from = BlockPosArgument.getSpawnablePos(context, POS1);
        final BlockPos to = BlockPosArgument.getSpawnablePos(context, POS2);
        final BlockPos anchorPos = BlockPosArgument.getSpawnablePos(context, ANCHOR_POS);
        return execute(context.getSource(), from, to, Optional.of(anchorPos), null, null);
    }

    private static int onExecuteWithPlayerName(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final BlockPos from = BlockPosArgument.getSpawnablePos(context, POS1);
        final BlockPos to = BlockPosArgument.getSpawnablePos(context, POS2);
        GameProfile profile = GameProfileArgument.getGameProfiles(context, PLAYER_NAME).stream().findFirst().orElse(null);
        return execute(context.getSource(), from, to, Optional.empty(), profile, null);
    }

    private static int onExecuteWithPlayerNameAndFileName(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final BlockPos from = BlockPosArgument.getSpawnablePos(context, POS1);
        final BlockPos to = BlockPosArgument.getSpawnablePos(context, POS2);
        GameProfile profile = GameProfileArgument.getGameProfiles(context, PLAYER_NAME).stream().findFirst().orElse(null);
        String name = StringArgumentType.getString(context, FILE_NAME);
        return execute(context.getSource(), from, to, Optional.empty(), profile, name);
    }

    private static int onExecuteWithPlayerNameAndFileNameAndAnchorPos(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        final BlockPos from = BlockPosArgument.getSpawnablePos(context, POS1);
        final BlockPos to = BlockPosArgument.getSpawnablePos(context, POS2);
        final BlockPos anchorPos = BlockPosArgument.getSpawnablePos(context, ANCHOR_POS);
        GameProfile profile = GameProfileArgument.getGameProfiles(context, PLAYER_NAME).stream().findFirst().orElse(null);
        String name = StringArgumentType.getString(context, FILE_NAME);
        return execute(context.getSource(), from, to, Optional.of(anchorPos), profile, name);
    }

    protected static LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return newLiteral(NAME)
                .then(newArgument(POS1, BlockPosArgument.blockPos())
                        .then(newArgument(POS2, BlockPosArgument.blockPos())
                                .executes(ScanCommand::onExecute)
                                .then(newArgument(ANCHOR_POS, BlockPosArgument.blockPos())
                                        .executes(ScanCommand::onExecuteWithAnchor))
                                .then(newArgument(PLAYER_NAME, GameProfileArgument.gameProfile())
                                        .executes(ScanCommand::onExecuteWithPlayerName)
                                        .then(newArgument(FILE_NAME, StringArgumentType.word())
                                                .executes(ScanCommand::onExecuteWithPlayerNameAndFileName)
                                                .then(newArgument(ANCHOR_POS, BlockPosArgument.blockPos())
                                                        .executes(ScanCommand::onExecuteWithPlayerNameAndFileNameAndAnchorPos))))));
    }

    /**
     * Generates a command string for the given parameters.
     *
     * @param from The first corner position.
     * @param to The second corner position.
     * @param anchor The anchor position, or null.
     * @param name The scan filename.
     * @return The command string.
     */
    @NotNull
    public static String format(@NotNull final BlockPos from,
                                @NotNull final BlockPos to,
                                @Nullable final BlockPos anchor,
                                @NotNull final String name)
    {
        final StringBuilder builder = new StringBuilder();
        builder.append(String.format("/%s %s %d %d %d %d %d %d @p %s", MOD_ID, NAME,
                from.getX(), from.getY(), from.getZ(),
                to.getX(), to.getY(), to.getZ(),
                name));
        if (anchor != null && new AABB(from, to).contains(anchor.getX(), anchor.getY(), anchor.getZ()))
        {
            builder.append(String.format(" %d %d %d", anchor.getX(), anchor.getY(), anchor.getZ()));
        }
        return builder.toString();
    }
}
