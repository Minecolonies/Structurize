package com.ldtteam.structurize.items;

import com.ldtteam.structurize.api.util.IMiddleClickableItem;
import com.ldtteam.structurize.api.util.IScrollableItem;
import com.ldtteam.structurize.blueprints.v1.Blueprint;
import com.ldtteam.structurize.blueprints.v1.BlueprintUtil;
import com.ldtteam.structurize.Network;
import com.ldtteam.structurize.Structurize;
import com.ldtteam.structurize.api.util.BlockPosUtil;
import com.ldtteam.structurize.blockentities.interfaces.IBlueprintDataProviderBE;
import com.ldtteam.structurize.client.gui.WindowScan;
import com.ldtteam.structurize.network.messages.SaveScanMessage;
import com.ldtteam.structurize.network.messages.ShowScanMessage;
import com.ldtteam.structurize.storage.rendering.RenderingCache;
import com.ldtteam.structurize.storage.rendering.types.BoxPreviewData;
import com.ldtteam.structurize.util.BlockInfo;
import com.ldtteam.structurize.util.LanguageHandler;
import com.ldtteam.structurize.util.ScanToolData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.ldtteam.structurize.api.util.constant.TranslationConstants.ANCHOR_POS_OUTSIDE_SCHEMATIC;
import static com.ldtteam.structurize.api.util.constant.TranslationConstants.MAX_SCHEMATIC_SIZE_REACHED;
import static com.ldtteam.structurize.blockentities.interfaces.IBlueprintDataProviderBE.TAG_BLUEPRINTDATA;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Item used to scan structures.
 */
public class ItemScanTool extends AbstractItemWithPosSelector implements IScrollableItem, IMiddleClickableItem
{
    private static final String ANCHOR_POS_TKEY = "item.possetter.anchorpos";
    private static final String NBT_ANCHOR_POS  = "structurize:anchor_pos";
    private static final String NBT_NAME = "structurize:name";

    /**
     * Creates default scan tool item.
     *
     * @param itemGroup creative tab
     */
    public ItemScanTool(final CreativeModeTab itemGroup)
    {
        this(new Item.Properties().durability(0).setNoRepair().rarity(Rarity.UNCOMMON).tab(itemGroup));
    }

    /**
     * MC constructor.
     *
     * @param properties properties
     */
    public ItemScanTool(final Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResult onAirRightClick(final BlockPos start, final BlockPos end, final Level worldIn, final Player playerIn, final ItemStack itemStack)
    {
        final ScanToolData data = new ScanToolData(itemStack.getOrCreateTag());
        saveSlot(data, itemStack, playerIn);

        if (!worldIn.isClientSide)
        {
            if (playerIn.isShiftKeyDown())
            {
                saveStructure(worldIn, playerIn, data.getCurrentSlotData(), true);
            }
        }
        else
        {
            if (!playerIn.isShiftKeyDown())
            {
                final WindowScan window = new WindowScan(data);
                window.open();
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public AbstractItemWithPosSelector getRegisteredItemInstance()
    {
        return ModItems.scanTool.get();
    }

    /**
     * Scan the structure and save it to the disk.
     *
     * @param world        Current world.
     * @param player       causing this action.
     * @param slot         the scan data.
     * @param saveEntities whether to scan in entities
     */
    public static void saveStructure(
      final Level world,
      final Player player,
      final ScanToolData.Slot slot,
      final boolean saveEntities)
    {
        if (slot.getBox().getAnchor().isPresent())
        {
            if (!BlockPosUtil.isInbetween(slot.getBox().getAnchor().get(), slot.getBox().getPos1(), slot.getBox().getPos2()))
            {
                LanguageHandler.sendPlayerMessage(player, ANCHOR_POS_OUTSIDE_SCHEMATIC);
                return;
            }
        }

        final BoundingBox box = BoundingBox.fromCorners(slot.getBox().getPos1(), slot.getBox().getPos2());
        if (box.getXSpan() * box.getYSpan() * box.getZSpan() > Structurize.getConfig().getServer().schematicBlockLimit.get())
        {
            LanguageHandler.sendPlayerMessage(player, MAX_SCHEMATIC_SIZE_REACHED, Structurize.getConfig().getServer().schematicBlockLimit.get());
            return;
        }

        final long currentMillis = System.currentTimeMillis();
        final String currentMillisString = Long.toString(currentMillis);
        String fileName;
        if (slot.getName().isEmpty())
        {
            fileName = Component.translatable("item.sceptersteel.scanformat", "", currentMillisString).getString();
        }
        else
        {
            fileName = slot.getName();
        }

        if (!fileName.contains(".blueprint"))
        {
            fileName+= ".blueprint";
        }

        final BlockPos zero = new BlockPos(box.minX(), box.minY(), box.minZ());
        final Blueprint bp = BlueprintUtil.createBlueprint(world, zero, saveEntities, (short) box.getXSpan(), (short) box.getYSpan(), (short) box.getZSpan(), fileName, slot.getBox().getAnchor());

        if (slot.getBox().getAnchor().isEmpty() && bp.getPrimaryBlockOffset().equals(new BlockPos(bp.getSizeX() / 2, 0, bp.getSizeZ() / 2)))
        {
            final List<BlockInfo> list = bp.getBlockInfoAsList().stream()
              .filter(blockInfo -> blockInfo.hasTileEntityData() && blockInfo.getTileEntityData().contains(TAG_BLUEPRINTDATA))
              .collect(Collectors.toList());

            if (list.size() > 1)
            {
                player.displayClientMessage(Component.translatable("com.ldtteam.structurize.gui.scantool.scanbadanchor", fileName), false);
            }
        }

        Network.getNetwork().sendToPlayer(new SaveScanMessage(BlueprintUtil.writeBlueprintToNBT(bp), fileName), (ServerPlayer) player);
    }

    @Override
    public boolean canAttackBlock(final BlockState state, final Level worldIn, final BlockPos pos, final Player player)
    {
        if (!player.isShiftKeyDown())
        {
            return super.canAttackBlock(state, worldIn, pos, player);
        }

        if (worldIn.isClientSide())
        {
            LanguageHandler.sendMessageToPlayer(player, ANCHOR_POS_TKEY, pos.getX(), pos.getY(), pos.getZ());
        }

        ItemStack itemstack = player.getMainHandItem();
        if (!itemstack.getItem().equals(getRegisteredItemInstance()))
        {
            itemstack = player.getOffhandItem();
        }

        final BlockEntity te = worldIn.getBlockEntity(pos);
        if (te instanceof IBlueprintDataProviderBE && !((IBlueprintDataProviderBE) te).getSchematicName().isEmpty())
        {
            if (worldIn.isClientSide && RenderingCache.getBoxPreviewData("scan") != null)
            {
                RenderingCache.getBoxPreviewData("scan").setAnchor(Optional.of(pos));
            }

            final BlockPos start = ((IBlueprintDataProviderBE) te).getInWorldCorners().getA();
            final BlockPos end = ((IBlueprintDataProviderBE) te).getInWorldCorners().getB();

            if (!(start.equals(pos)) && !(end.equals(pos)))
            {
                if (worldIn.isClientSide)
                {
                    RenderingCache.queue("scan", new BoxPreviewData(((IBlueprintDataProviderBE) te).getInWorldCorners().getA(), ((IBlueprintDataProviderBE) te).getInWorldCorners().getB(), Optional.of(pos)));
                }
                setBounds(itemstack, start, end);
            }
        }

        setAnchorPos(itemstack, pos);
        return false;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack,
                                @Nullable Level world,
                                @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flags)
    {
        super.appendHoverText(stack, world, tooltip, flags);

        if (stack.hasTag())
        {
            tooltip.add(getCurrentSlotDescription(stack));
        }
    }

    @Override
    public Component getHighlightTip(@NotNull final ItemStack stack, @NotNull final Component displayName)
    {
        return Component.empty()
                .append(super.getHighlightTip(stack, displayName))
                .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
                .append(getCurrentSlotDescription(stack));
    }

    private Component getCurrentSlotDescription(@NotNull final ItemStack stack)
    {
        final ScanToolData data = new ScanToolData(stack.getOrCreateTag());
        MutableComponent desc = Component.empty()
                .append(Component.literal(String.valueOf(data.getCurrentSlotId())).withStyle(ChatFormatting.GRAY));

        final String name = getStructureName(stack);
        if (!name.isEmpty())
        {
            desc = desc.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(name));
        }

        return desc;
    }

    @Override
    public InteractionResult onMiddleClick(@NotNull final Player player,
                                           @NotNull final ItemStack stack,
                                           @Nullable final BlockPos pos)
    {
        if (pos == null)
        {
            // treat click in air like mouse scrolling (just in case someone doesn't have a wheel)
            final double delta = player.isShiftKeyDown() ? -1 : 1;
            return onMouseScroll(player, stack, delta);
        }

        // ignore middle click on blocks for now (standard pick-block)
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult onMouseScroll(@NotNull final Player player,
                                           @NotNull final ItemStack stack,
                                           final double delta)
    {
        if (player.getLevel().isClientSide())
        {
            return InteractionResult.SUCCESS;
        }

        switchSlot((ServerPlayer) player, stack, delta < 0 ? ScanToolData::prevSlot : ScanToolData::nextSlot);

        return InteractionResult.SUCCESS;
    }

    private void switchSlot(@NotNull final ServerPlayer player,
                            @NotNull final ItemStack stack,
                            @NotNull final Consumer<ScanToolData> action)
    {
        final ScanToolData data = new ScanToolData(stack.getOrCreateTag());
        saveSlot(data, stack, player);
        action.accept(data);
        final ScanToolData.Slot slot = loadSlot(data, stack);

        Network.getNetwork().sendToPlayer(new ShowScanMessage(slot.getBox()), player);
    }

    private void saveSlot(@NotNull final ScanToolData data,
                          @NotNull final ItemStack stack,
                          @NotNull final Player player)
    {
        data.setCurrentSlotData(new ScanToolData.Slot(getStructureName(stack), getBox(stack, player)));
    }

    public ScanToolData.Slot loadSlot(@NotNull final ScanToolData data,
                                      @NotNull final ItemStack stack)
    {
        final ScanToolData.Slot slot = data.getCurrentSlotData();

        // this seems a little silly at first, duplicating this info outside the slot storage.
        // but it preserves compatibility with AbstractItemWithPosSelector.
        setStructureName(stack, slot.getName());
        setBounds(stack, slot.getBox().getPos1(), slot.getBox().getPos2());
        setAnchorPos(stack, slot.getBox().getAnchor().orElse(null));

        return slot;
    }

    /**
     * Gets the coordinates of this tool as a {@link BoxPreviewData}
     * @param tool The tool stack (assumed already been validated)
     * @param player The player who will be notified if it has a bad anchor position
     * @return the box
     */
    public static BoxPreviewData getBox(@NotNull final ItemStack tool, @NotNull final Player player)
    {
        final Tuple<BlockPos, BlockPos> bounds = getBounds(tool);
        Optional<BlockPos> anchor = Optional.ofNullable(getAnchorPos(tool));
        if (anchor.isPresent() && !BlockPosUtil.isInbetween(anchor.get(), bounds.getA(), bounds.getB()))
        {
            if (player.getLevel().isClientSide())
            {
                player.displayClientMessage(Component.translatable("com.ldtteam.structurize.gui.scantool.outsideanchor"), false);
            }
            anchor = Optional.empty();
        }
        return new BoxPreviewData(bounds.getA(), bounds.getB(), anchor);
    }

    /**
     * Saves the anchor coordinates on this stack.
     * @param tool The tool stack (assumed already been validated)
     * @param anchor The new anchor position (or null to clear)
     */
    public static void setAnchorPos(@NotNull final ItemStack tool,
                                    @Nullable final BlockPos anchor)
    {
        if (anchor == null)
        {
            tool.getOrCreateTag().remove(NBT_ANCHOR_POS);
        }
        else
        {
            tool.getOrCreateTag().put(NBT_ANCHOR_POS, NbtUtils.writeBlockPos(anchor));
        }
    }

    /**
     * Loads the anchor coordinates from this stack.
     * @param tool The tool stack (assumed already been validated)
     * @return the anchor position or null
     */
    @Nullable
    public static BlockPos getAnchorPos(@NotNull final ItemStack tool)
    {
        final CompoundTag tag = tool.getOrCreateTag();
        return tag.contains(NBT_ANCHOR_POS) ? NbtUtils.readBlockPos(tag.getCompound(NBT_ANCHOR_POS)) : null;
    }

    /**
     * Saves the structure name on this stack.
     * @param tool The tool stack (assumed already validated)
     * @param name The structure name (or null/empty to clear)
     */
    public static void setStructureName(@NotNull final ItemStack tool,
                                        @Nullable final String name)
    {
        if (name == null || name.isEmpty())
        {
            tool.getOrCreateTag().remove(NBT_NAME);
        }
        else
        {
            tool.getOrCreateTag().putString(NBT_NAME, name);
        }
    }

    /**
     * Gets the structure name saved on this stack.
     * @param tool The tool stack (assumed already validated)
     * @return The structure name (or empty string)
     */
    public static String getStructureName(@NotNull final ItemStack tool)
    {
        return tool.getOrCreateTag().getString(NBT_NAME);
    }
}
