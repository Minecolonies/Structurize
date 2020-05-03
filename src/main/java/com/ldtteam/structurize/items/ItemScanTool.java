package com.ldtteam.structurize.items;

import com.ldtteam.structures.blueprints.v1.Blueprint;
import com.ldtteam.structures.blueprints.v1.BlueprintUtil;
import com.ldtteam.structurize.Network;
import com.ldtteam.structurize.api.util.Log;
import com.ldtteam.structurize.api.util.Utils;
import com.ldtteam.structurize.client.gui.WindowScan;
import com.ldtteam.structurize.management.StructureName;
import com.ldtteam.structurize.management.Structures;
import com.ldtteam.structurize.network.messages.SaveScanMessage;
import com.ldtteam.structurize.util.LanguageHandler;
import com.ldtteam.structurize.util.StructureLoadingUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;

import static com.ldtteam.structurize.api.util.constant.Constants.MAX_SCHEMATIC_SIZE;
import static com.ldtteam.structurize.api.util.constant.TranslationConstants.ANCHOR_POS_OUTSIDE_SCHEMATIC;
import static com.ldtteam.structurize.api.util.constant.TranslationConstants.MAX_SCHEMATIC_SIZE_REACHED;

/**
 * Item used to scan structures.
 */
public class ItemScanTool extends AbstractItemWithPosSelector
{
    private static final String ANCHOR_POS_TKEY = "item.possetter.anchorpos";
    private static final String NBT_ANCHOR_POS = "structurize:anchor_pos";

    /**
     * Creates default scan tool item.
     *
     * @param itemGroup creative tab
     */
    public ItemScanTool(final ItemGroup itemGroup)
    {
        this(new Item.Properties().maxDamage(0).setNoRepair().rarity(Rarity.UNCOMMON).group(itemGroup));
    }

    /**
     * MC constructor.
     *
     * @param properties properties
     */
    public ItemScanTool(final Properties properties)
    {
        super(properties);
        setRegistryName("sceptersteel");
    }

    @Override
    public ActionResultType onAirRightClick(final BlockPos start, final BlockPos end, final World worldIn, final PlayerEntity playerIn, final ItemStack itemStack)
    {
        Optional<BlockPos> anchorPos = itemStack.getOrCreateTag().contains(NBT_ANCHOR_POS) ? Optional.of(NBTUtil.readBlockPos(itemStack.getOrCreateTag().getCompound(NBT_ANCHOR_POS))) : Optional.empty();
        if (!worldIn.isRemote)
        {
            if (playerIn.isSneaking())
            {
                saveStructure(worldIn, start, end, playerIn, null, true, anchorPos);
            }
        }
        else
        {
            if (!playerIn.isSneaking())
            {
                final WindowScan window = new WindowScan(start, end, anchorPos);
                window.open();
            }
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public AbstractItemWithPosSelector getRegisteredItemInstance()
    {
        return ModItems.scanTool;
    }

    /**
     * Scan the structure and save it to the disk.
     *
     * @param world  Current world.
     * @param from   First corner.
     * @param to     Second corner.
     * @param player causing this action.
     * @param name   the name of it.
     */
    public static void saveStructure(@NotNull final World world,
        @NotNull final BlockPos from,
        @NotNull final BlockPos to,
        @NotNull final PlayerEntity player,
        final String name)
    {
        saveStructure(world, from, to, player, name, true, Optional.empty());
    }

    /**
     * Scan the structure and save it to the disk.
     *
     * @param world        Current world.
     * @param from         First corner.
     * @param to           Second corner.
     * @param player       causing this action.
     * @param name         the name of it.
     * @param saveEntities whether to scan in entities
     */
    public static void saveStructure(
            @NotNull final World world,
            @NotNull final BlockPos from,
            @NotNull final BlockPos to,
            @NotNull final PlayerEntity player,
            final String name,
            final boolean saveEntities,
            final Optional<BlockPos> anchorPos)
    {
        if (anchorPos.isPresent())
        {
            final double minX = Math.min(to.getX(), from.getX());
            final double minY = Math.min(to.getY(), from.getY());
            final double minZ = Math.min(to.getZ(), from.getZ());
            final double maxX = Math.max(to.getX(), from.getX());
            final double maxY = Math.max(to.getY(), from.getY());
            final double maxZ = Math.max(to.getZ(), from.getZ());

            final double anchorPosX = anchorPos.get().getX();
            final double anchorPosY = anchorPos.get().getY();
            final double anchorPosZ = anchorPos.get().getZ();

            final boolean isWithinX = anchorPosX >= minX && anchorPosX <= maxX;
            final boolean isWithinY = anchorPosY >= minY && anchorPosY <= maxY;
            final boolean isWithinZ = anchorPosZ >= minZ && anchorPosZ <= maxZ;

            if (!(isWithinX && isWithinY && isWithinZ))
            {
                LanguageHandler.sendPlayerMessage(player, ANCHOR_POS_OUTSIDE_SCHEMATIC);
                return;
            }
        }

        final BlockPos blockpos =
          new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
        final BlockPos blockpos1 =
          new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
        final BlockPos size = blockpos1.subtract(blockpos).add(1, 1, 1);
        if (size.getX() * size.getY() * size.getZ() > MAX_SCHEMATIC_SIZE)
        {
            LanguageHandler.sendPlayerMessage(player, MAX_SCHEMATIC_SIZE_REACHED, MAX_SCHEMATIC_SIZE);
            return;
        }

        final long currentMillis = System.currentTimeMillis();
        final String currentMillisString = Long.toString(currentMillis);
        final String fileName;
        if (name == null || name.isEmpty())
        {
            fileName = LanguageHandler.format("item.sceptersteel.scanformat", "", currentMillisString);
        }
        else
        {
            fileName = name;
        }

        final Blueprint bp = BlueprintUtil.createBlueprint(world, blockpos, saveEntities, (short) size.getX(), (short) size.getY(), (short) size.getZ(), name, anchorPos);
        Network.getNetwork().sendToPlayer(new SaveScanMessage(BlueprintUtil.writeBlueprintToNBT(bp), fileName), (ServerPlayerEntity) player);
    }

    /**
     * Save a structure on the server.
     *
     * @param world the world.
     * @param from  the start position.
     * @param to    the end position.
     * @param name  the name.
     * @return true if succesful.
     */
    public static boolean saveStructureOnServer(@NotNull final World world,
        @NotNull final BlockPos from,
        @NotNull final BlockPos to,
        final String name)
    {
        return saveStructureOnServer(world, from, to, name, true);
    }

    /**
     * Save a structure on the server.
     *
     * @param world        the world.
     * @param from         the start position.
     * @param to           the end position.
     * @param name         the name.
     * @param saveEntities whether to scan in entities
     * @return true if succesful.
     */
    public static boolean saveStructureOnServer(@NotNull final World world,
        @NotNull final BlockPos from,
        @NotNull final BlockPos to,
        final String name,
        final boolean saveEntities)
    {
        final BlockPos blockpos = new BlockPos(Math.min(from.getX(), to.getX()),
            Math.min(from.getY(), to.getY()),
            Math.min(from.getZ(), to.getZ()));
        final BlockPos blockpos1 = new BlockPos(Math.max(from.getX(), to.getX()),
            Math.max(from.getY(), to.getY()),
            Math.max(from.getZ(), to.getZ()));
        final BlockPos size = blockpos1.subtract(blockpos).add(1, 1, 1);
        if (size.getX() * size.getY() * size.getZ() > MAX_SCHEMATIC_SIZE)
        {
            Log.getLogger().warn("Saving too large schematic for:" + name);
        }

        final String prefix = "cache";
        final String fileName;
        if (name == null || name.isEmpty())
        {
            fileName = LanguageHandler.format("item.sceptersteel.scanformat");
        }
        else
        {
            fileName = name;
        }

        final StructureName structureName = new StructureName(prefix, "backup", fileName);

        final List<File> folder = StructureLoadingUtils.getCachedSchematicsFolders();
        if (folder == null || folder.isEmpty())
        {
            Log.getLogger().warn("Unable to save schematic in cache since no folder was found.");
            return false;
        }

        final Blueprint bp = BlueprintUtil.createBlueprint(world, blockpos, saveEntities, (short) size.getX(), (short) size.getY(), (short) size.getZ(), name, Optional.empty());

        final File file = new File(folder.get(0), structureName.toString() + Structures.SCHEMATIC_EXTENSION_NEW);
        Utils.checkDirectory(file.getParentFile());

        try (OutputStream outputstream = new FileOutputStream(file))
        {
            CompressedStreamTools.writeCompressed(BlueprintUtil.writeBlueprintToNBT(bp), outputstream);
        }
        catch (final Exception e)
        {
            return false;
        }
        return true;
    }

    @Override
    public ActionResultType onSpecialUse(final ItemUseContext context)
    {
        final BlockPos pos = context.getPos();
        if (context.getWorld().isRemote() && context.getPlayer() != null)
        {
            LanguageHandler.sendMessageToPlayer(context.getPlayer(), ANCHOR_POS_TKEY, pos.getX(), pos.getY(), pos.getZ());
        }
        context.getItem().getOrCreateTag().put(NBT_ANCHOR_POS, NBTUtil.writeBlockPos(pos));
        return ActionResultType.SUCCESS;
    }
}
