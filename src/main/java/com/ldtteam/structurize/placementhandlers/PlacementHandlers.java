package com.ldtteam.structurize.placementhandlers;

import com.ldtteam.structurize.api.util.ItemStackUtils;
import com.ldtteam.structurize.blocks.schematic.BlockSolidSubstitution;
import com.ldtteam.structurize.util.BlockUtils;
import com.ldtteam.structurize.util.PlacementSettings;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.state.properties.BedPart;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ldtteam.structurize.api.util.constant.Constants.UPDATE_FLAG;

/**
 * Class containing all placement handler implementations.
 * <p>
 * We suppress warning squid:S2972 which handles the max size of internal classes. This doesn't apply here since it wouldn't make sense extracting all of those in separate
 * classes.
 */
@SuppressWarnings("squid:S2972")
public final class PlacementHandlers
{
    public static final List<IPlacementHandler> handlers = new ArrayList<>();
    static
    {
        handlers.add(new AirPlacementHandler());
        handlers.add(new FirePlacementHandler());
        handlers.add(new GrassPlacementHandler());
        handlers.add(new DoorPlacementHandler());
        handlers.add(new BedPlacementHandler());
        handlers.add(new DoublePlantPlacementHandler());
        handlers.add(new SpecialBlockPlacementAttemptHandler());
        handlers.add(new FlowerPotPlacementHandler());
        handlers.add(new BlockGrassPathPlacementHandler());
        handlers.add(new StairBlockPlacementHandler());
        handlers.add(new ChestPlacementHandler());
        handlers.add(new FallingBlockPlacementHandler());
        handlers.add(new BannerPlacementHandler());
        handlers.add(new BlockSolidSubstitutionPlacementHandler());
        handlers.add(new GeneralBlockPlacementHandler());
    }
    /**
     * Private constructor to hide implicit one.
     */
    private PlacementHandlers()
    {
        /*
         * Intentionally left empty.
         */
    }

    public static class BlockSolidSubstitutionPlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() instanceof BlockSolidSubstitution;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            final BlockState newBlockState = BlockUtils.getSubstitutionBlockAtWorld(world, pos);
            if (complete)
            {
                if (!world.setBlockState(pos, blockState, UPDATE_FLAG))
                {
                    return ActionProcessingResult.DENY;
                }
            }
            else
            {
                if (!world.setBlockState(pos, newBlockState, UPDATE_FLAG))
                {
                    return ActionProcessingResult.DENY;
                }
            }

            return newBlockState;
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            final BlockState newBlockState = BlockUtils.getSubstitutionBlockAtWorld(world, pos);
            for (final IPlacementHandler handler : PlacementHandlers.handlers)
            {
                if (handler.canHandle(world, pos, newBlockState))
                {
                    return handler.getRequiredItems(world, pos, newBlockState, tileEntityData, complete);
                }
            }
            final List<ItemStack> itemList = new ArrayList<>();
            itemList.add(BlockUtils.getItemStackFromBlockState(newBlockState));
            return itemList;
        }
    }

    public static class FirePlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() instanceof FireBlock;
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            final List<ItemStack> itemList = new ArrayList<>();
            itemList.add(new ItemStack(Items.FLINT_AND_STEEL, 1));
            return itemList;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            world.setBlockState(pos, blockState, UPDATE_FLAG);
            return ActionProcessingResult.ACCEPT;
        }
    }

    public static class FallingBlockPlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() instanceof FallingBlock;
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            final List<ItemStack> itemList = new ArrayList<>(getItemsFromTileEntity(tileEntityData, world));
            itemList.add(BlockUtils.getItemStackFromBlockState(blockState));
            itemList.removeIf(ItemStackUtils::isEmpty);
            if (!world.getBlockState(pos.down()).getMaterial().isSolid())
            {
                itemList.add(BlockUtils.getItemStackFromBlockState(BlockUtils.getSubstitutionBlockAtWorld(world, pos)));
            }
            return itemList;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            if (world.getBlockState(pos).equals(blockState))
            {
                return ActionProcessingResult.ACCEPT;
            }

            if (!world.getBlockState(pos.down()).getMaterial().isSolid())
            {
                world.setBlockState(pos.down(), BlockUtils.getSubstitutionBlockAtWorld(world, pos), UPDATE_FLAG);
            }
            if (!world.setBlockState(pos, blockState, UPDATE_FLAG))
            {
                return ActionProcessingResult.DENY;
            }

            if (tileEntityData != null)
            {
                handleTileEntityPlacement(tileEntityData, world, pos);
            }

            return blockState;
        }
    }

    public static class GrassPlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() == Blocks.GRASS_BLOCK;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            if (!world.setBlockState(pos, Blocks.GRASS_BLOCK.getDefaultState(), UPDATE_FLAG))
            {
                return ActionProcessingResult.DENY;
            }
            return Blocks.DIRT.getDefaultState();
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            final List<ItemStack> itemList = new ArrayList<>();
            itemList.add(new ItemStack(Blocks.DIRT));
            return itemList;
        }
    }

    public static class DoorPlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() instanceof DoorBlock;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            // todo maybe doors work from scratch?
            if (blockState.get(DoorBlock.HALF).equals(DoubleBlockHalf.LOWER))
            {
                world.setBlockState(pos, blockState.with(DoorBlock.HALF, DoubleBlockHalf.LOWER), UPDATE_FLAG);
                world.setBlockState(pos.up(), blockState.with(DoorBlock.HALF, DoubleBlockHalf.UPPER), UPDATE_FLAG);
            }

            return blockState;
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            final List<ItemStack> itemList = new ArrayList<>();
            itemList.add(BlockUtils.getItemStackFromBlockState(blockState));
            return itemList;
        }
    }

    public static class BedPlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() instanceof BedBlock;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            if (blockState.get(BedBlock.PART) == BedPart.HEAD)
            {
                final Direction facing = blockState.get(BedBlock.HORIZONTAL_FACING);

                // pos.offset(facing) will get the other part of the bed
                world.setBlockState(pos.offset(facing.getOpposite()), blockState.with(BedBlock.PART, BedPart.FOOT), UPDATE_FLAG);
                world.setBlockState(pos, blockState.with(BedBlock.PART, BedPart.HEAD), UPDATE_FLAG);

                if (tileEntityData != null)
                {
                    handleTileEntityPlacement(tileEntityData, world, pos);
                    handleTileEntityPlacement(tileEntityData, world, pos.offset(facing.getOpposite()));
                }
                return blockState;
            }

            return ActionProcessingResult.ACCEPT;
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            if (blockState.get(BedBlock.PART) == BedPart.HEAD)
            {
                final List<ItemStack> list = new ArrayList<>();
                list.add(new ItemStack(blockState.getBlock(), 1));
                return list;
            }
            return Collections.emptyList();
        }
    }

    public static class DoublePlantPlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() instanceof DoublePlantBlock;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            if (blockState.get(DoublePlantBlock.HALF).equals(DoubleBlockHalf.LOWER))
            {
                world.setBlockState(pos, blockState.with(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER), UPDATE_FLAG);
                world.setBlockState(pos.up(), blockState.with(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER), UPDATE_FLAG);
                return blockState;
            }
            return ActionProcessingResult.ACCEPT;
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            final List<ItemStack> itemList = new ArrayList<>();
            itemList.add(BlockUtils.getItemStackFromBlockState(blockState));
            return itemList;
        }
    }

    public static class SpecialBlockPlacementAttemptHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() instanceof EndPortalBlock || blockState.getBlock() instanceof SpawnerBlock ||
                     blockState.getBlock() instanceof DragonEggBlock || blockState.getBlock() instanceof EndPortalBlock;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            return ActionProcessingResult.ACCEPT;
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            return new ArrayList<>();
        }
    }

    public static class FlowerPotPlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() instanceof FlowerPotBlock;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            if (!world.setBlockState(pos, blockState, UPDATE_FLAG))
            {
                return false;
            }

            if (tileEntityData != null)
            {
                handleTileEntityPlacement(tileEntityData, world, pos);
            }
            return blockState;
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            final List<ItemStack> itemList = new ArrayList<>();
            itemList.add(BlockUtils.getItemStackFromBlockState(blockState));
            itemList.addAll(getItemsFromTileEntity(tileEntityData, world));
            itemList.removeIf(ItemStackUtils::isEmpty);

            return itemList;
        }
    }

    public static class AirPlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() instanceof AirBlock;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            if (!world.isAirBlock(pos))
            {
                final List<Entity> entityList =
                  world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos), entity -> !(entity instanceof LivingEntity || entity instanceof ItemEntity));
                if (!entityList.isEmpty())
                {
                    for (final Entity entity : entityList)
                    {
                        entity.remove();
                    }
                }

                world.removeBlock(pos, false);
            }
            return ActionProcessingResult.ACCEPT;
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            return new ArrayList<>();
        }
    }

    public static class BlockGrassPathPlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() instanceof GrassPathBlock;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            if (!world.setBlockState(pos, Blocks.GRASS_PATH.getDefaultState(), UPDATE_FLAG))
            {
                return ActionProcessingResult.DENY;
            }

            return Blocks.DIRT.getDefaultState();
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            final List<ItemStack> itemList = new ArrayList<>();
            itemList.add(new ItemStack(Blocks.DIRT, 1));
            return itemList;
        }
    }

    public static class StairBlockPlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() instanceof StairsBlock
                     && world.getBlockState(pos).getBlock() instanceof StairsBlock
                     && world.getBlockState(pos).get(StairsBlock.FACING) == blockState.get(StairsBlock.FACING)
                     && blockState.getBlock() == world.getBlockState(pos).getBlock();
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            return ActionProcessingResult.ACCEPT;
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            return new ArrayList<>();
        }
    }

    public static class GeneralBlockPlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return true;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos,
          final PlacementSettings settings)
        {
            if (world.getBlockState(pos).equals(blockState))
            {
                if (tileEntityData != null)
                {
                    handleTileEntityPlacement(tileEntityData, world, pos, settings);
                }
                return ActionProcessingResult.ACCEPT;
            }

            if (!world.setBlockState(pos, blockState, UPDATE_FLAG))
            {
                return ActionProcessingResult.DENY;
            }

            if (tileEntityData != null)
            {
                handleTileEntityPlacement(tileEntityData, world, pos, settings);
            }

            return blockState;
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            final List<ItemStack> itemList = new ArrayList<>(getItemsFromTileEntity(tileEntityData, world));
            itemList.add(BlockUtils.getItemStackFromBlockState(blockState));
            itemList.removeIf(ItemStackUtils::isEmpty);
            return itemList;
        }
    }

    public static class ChestPlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() instanceof ChestBlock;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            if (!world.setBlockState(pos, blockState, UPDATE_FLAG))
            {
                return ActionProcessingResult.DENY;
            }

            if (tileEntityData != null)
            {
                handleTileEntityPlacement(tileEntityData, world, pos);
            }

            return blockState;
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            final List<ItemStack> itemList = new ArrayList<>();
            itemList.add(BlockUtils.getItemStackFromBlockState(blockState));
            itemList.addAll(getItemsFromTileEntity(tileEntityData, world));

            itemList.removeIf(ItemStackUtils::isEmpty);

            return itemList;
        }
    }

    public static class BannerPlacementHandler implements IPlacementHandler
    {
        @Override
        public boolean canHandle(@NotNull final World world, @NotNull final BlockPos pos, @NotNull final BlockState blockState)
        {
            return blockState.getBlock() instanceof BannerBlock;
        }

        @Override
        public Object handle(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete,
          final BlockPos centerPos)
        {
            if (world.getBlockState(pos).equals(blockState))
            {
                if (tileEntityData != null)
                {
                    handleTileEntityPlacement(tileEntityData, world, pos);
                }
                return ActionProcessingResult.ACCEPT;
            }

            if (!world.setBlockState(pos, blockState, UPDATE_FLAG))
            {
                return ActionProcessingResult.DENY;
            }

            if (tileEntityData != null)
            {
                handleTileEntityPlacement(tileEntityData, world, pos);
            }

            return blockState;
        }

        @Override
        public List<ItemStack> getRequiredItems(
          @NotNull final World world,
          @NotNull final BlockPos pos,
          @NotNull final BlockState blockState,
          @Nullable final CompoundNBT tileEntityData,
          final boolean complete)
        {
            final List<ItemStack> itemList = new ArrayList<>(getItemsFromTileEntity(tileEntityData, world));
            itemList.removeIf(ItemStackUtils::isEmpty);
            return itemList;
        }
    }

    /**
     * Handles tileEntity placement.
     *
     * @param tileEntityData the data of the tile entity.
     * @param world          the world.
     * @param pos            the position.
     * @param settings       the placement settings.
     */
    public static void handleTileEntityPlacement(
      final CompoundNBT tileEntityData,
      final World world,
      @NotNull final BlockPos pos,
      final PlacementSettings settings)
    {
        if (tileEntityData != null)
        {
            final TileEntity newTile = TileEntity.create(tileEntityData);
            if (newTile != null)
            {
                world.setTileEntity(pos, newTile);
                newTile.rotate(settings.rotation);
                newTile.mirror(settings.mirror);
                final Chunk chunk = world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                PacketDistributor.TRACKING_CHUNK.with(() -> chunk).send(new SUpdateTileEntityPacket(pos, 0, tileEntityData));
            }
        }
    }

    /**
     * Handles tileEntity placement.
     *
     * @param tileEntityData the data of the tile entity.
     * @param world          the world.
     * @param pos            the position.
     */
    public static void handleTileEntityPlacement(final CompoundNBT tileEntityData, final World world, @NotNull final BlockPos pos)
    {
        handleTileEntityPlacement(tileEntityData, world, pos, new PlacementSettings());
    }

    /**
     * Gets the list of items from a possible tileEntity.
     *
     * @param tileEntityData the data.
     * @param world          the world.
     * @return the required list.
     */
    public static List<ItemStack> getItemsFromTileEntity(final CompoundNBT tileEntityData, final World world)
    {
        if (tileEntityData != null)
        {
            return ItemStackUtils.getItemStacksOfTileEntity(tileEntityData, world);
        }
        return Collections.emptyList();
    }
}
