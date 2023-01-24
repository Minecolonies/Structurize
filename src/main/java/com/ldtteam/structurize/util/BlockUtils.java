package com.ldtteam.structurize.util;

import com.ldtteam.domumornamentum.entity.block.IMateriallyTexturedBlockEntity;
import com.ldtteam.structurize.api.util.constant.Constants;
import com.ldtteam.structurize.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.AirItem;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.registries.GameData;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utility class for all Block type checking.
 */
public final class BlockUtils
{
    /**
     * Predicated to determine if a block is free to place.
     */
        public static final List<BiPredicate<Block, BlockState>> FREE_TO_PLACE_BLOCKS = Arrays.asList(
        (block, iBlockState) -> block.equals(Blocks.AIR),
        (block, iBlockState) -> iBlockState.getMaterial().isLiquid(),
        (block, iBlockState) -> BlockUtils.isWater(block.defaultBlockState()),
        (block, iBlockState) -> block instanceof LeavesBlock,
        (block, iBlockState) -> block instanceof DoublePlantBlock,
        (block, iBlockState) -> block.equals(Blocks.GRASS),
        (block, iBlockState) -> block instanceof DoorBlock && iBlockState != null && iBlockState.getValue(BooleanProperty.create("upper")));

    /**
     * Private constructor to hide the public one.
     */
    private BlockUtils()
    {
        // Hides implicit constructor.
    }

    /**
     * Updates the rotation of the structure depending on the input.
     *
     * @param rotation the rotation to be set.
     * @return returns the Rotation object.
     */
    public static Rotation getRotation(final int rotation)
    {
        switch (rotation)
        {
            case 1:
                return Rotation.CLOCKWISE_90;
            case 2:
                return Rotation.CLOCKWISE_180;
            case 3:
                return Rotation.COUNTERCLOCKWISE_90;
            default:
                return Rotation.NONE;
        }
    }

    /**
     * Gets a rotation from a block facing.
     *
     * @param facing the block facing.
     * @return the int rotation.
     */
    public static int getRotationFromFacing(final Direction facing)
    {
        switch (facing)
        {
            case SOUTH:
                return 2;
            case EAST:
                return 1;
            case WEST:
                return 3;
            default:
                return 0;
        }
    }

    /**
     * Get the filler block at a certain location.
     * If block follows gravity laws return dirt.
     *
     * @param world    the world the block is in.
     * @param location the location it is at.
     * @return the BlockState of the filler block.
     */
    @Deprecated(forRemoval = true, since="1.18.2")
    public static BlockState getSubstitutionBlockAtWorld(final Level world, final BlockPos location)
    {
        return Blocks.DIRT.defaultBlockState();
    }

    /**
     * Get the filler block at a certain location.
     *
     * @param  level         the world the block is in.
     * @param  location      the location it is at.
     * @param  virtualBlocks if null use level instead for getting surrounding block states, fnc may should return null if virtual
     *                       block is not available
     * @return               the BlockState of the filler block.
     */
    public static BlockState getSubstitutionBlockAtWorld(final Level level,
        final BlockPos location,
        @Nullable final Function<BlockPos, BlockState> virtualBlocks)
    {
        BlockState result = getWorldgenBlock(level, location, virtualBlocks);

        if (result != null && result.getBlock() == Blocks.POWDER_SNOW)
        {
            result = Blocks.SNOW_BLOCK.defaultBlockState();
        }
        else if (result == null || !result.getMaterial().isSolid() || result.getBlock() == Blocks.BEDROCK)
        {
            // try default level block
            result = getDefaultBlockForLevel(level, null);

            // oh non-solid again + vanilla has stupid settings so override them
            if (result == null || !result.getMaterial().isSolid() || result.getBlock() == Blocks.STONE)
            {
                result = Blocks.DIRT.defaultBlockState();
            }
        }

        return result;
    }

    /**
     * Get the worldGen block for a certain location. Always gives DIRT for non vanilla worlds including blueprint.
     *
     * @param  level         the world the block is in.
     * @param  location      the real world location.
     * @param  virtualBlocks if null use level instead for getting surrounding block states, fnc may should return null if virtual
     *                       block is not available
     * @return               the BlockState of the filler block.
     * @see                  net.minecraft.data.worldgen.SurfaceRuleData for possible blockstates
     */
    @Nullable
    public static BlockState getWorldgenBlock(final Level level, final BlockPos location, @Nullable final Function<BlockPos, BlockState> virtualBlocks)
    {
        if (level instanceof ServerLevel serverLevel)
        {
            final ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
            if (generator instanceof NoiseBasedChunkGenerator chunkGenerator)
            {
                final NoiseGeneratorSettings generatorSettings = chunkGenerator.generatorSettings().value();

                // VANILLA INLINE: look at usage of generatorSettings.surfaceRule()

                final ChunkAccess chunk = serverLevel.getChunk(location);
                final SurfaceRules.Context ctx = new SurfaceRules.Context(serverLevel.getChunkSource().randomState().surfaceSystem(),
                    serverLevel.getChunkSource().randomState(),
                    chunk,
                    chunk.getOrCreateNoiseChunk(c -> createNoiseBiome(serverLevel, chunkGenerator, c)),
                    serverLevel.getBiomeManager()::getBiome,
                    serverLevel.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY),
                    new WorldGenerationContext(chunkGenerator, serverLevel));

                final int locX = location.getX();
                final int locY = location.getY();
                final int locZ = location.getZ();

                int stoneDepthAbove = 1;
                int stoneDepthBelow = DimensionType.WAY_BELOW_MIN_Y;
                int waterHeight = Integer.MIN_VALUE;

                final MutableBlockPos temp = new MutableBlockPos(locX, locY, locZ);
                for (int tempY = locY + 1; tempY <= chunk.getMaxBuildHeight() + 1; ++tempY)
                {
                    temp.setY(tempY);
                    final BlockState bs = virtualBlocks == null ? chunk.getBlockState(temp) :
                        Objects.requireNonNullElseGet(virtualBlocks.apply(temp), () -> chunk.getBlockState(temp));
                    if (bs.isAir())
                    {
                        break;
                    }
                    else
                    {
                        if (!bs.getFluidState().isEmpty())
                        {
                            waterHeight = tempY + 1;
                        }
                        stoneDepthAbove++;
                    }
                }

                for (int tempY = locY - 1; tempY >= chunk.getMinBuildHeight() - 1; --tempY)
                {
                    temp.setY(tempY);
                    final BlockState bs = virtualBlocks == null ? chunk.getBlockState(temp) :
                        Objects.requireNonNullElseGet(virtualBlocks.apply(temp), () -> chunk.getBlockState(temp));
                    if (bs.isAir() || !bs.getFluidState().isEmpty())
                    {
                        stoneDepthBelow = tempY + 1;
                        break;
                    }
                }

                stoneDepthBelow = locY - stoneDepthBelow + 1;

                ctx.updateXZ(locX, locZ);
                ctx.updateY(stoneDepthAbove, stoneDepthBelow, waterHeight, locX, locY, locZ);

                return generatorSettings.surfaceRule().apply(ctx).tryApply(locX, locY, locZ);
            }
            else if (generator instanceof FlatLevelSource chunkGenerator)
            {
                final List<BlockState> layers = chunkGenerator.settings().getLayers();
                final int locY = location.getY() - serverLevel.getMinBuildHeight();
                if (locY >= 0 && locY < layers.size())
                {
                    return layers.get(locY);
                }
            }
        }

        return null;
    }

    private static NoiseChunk createNoiseBiome(
        final ServerLevel serverLevel,
        final NoiseBasedChunkGenerator chunkGenerator,
        final ChunkAccess chunk)
    {
        final int chunkX = chunk.getPos().x;
        final int chunkZ = chunk.getPos().z;
        final int chunkRange = ChunkStatus.SURFACE.getRange();
        final List<ChunkAccess> surroundingChunks = new ArrayList<>(4 * chunkRange * (chunkRange + 1) + 1);

        for (int z = -chunkRange; z <= chunkRange; z++)
        {
            for (int x = -chunkRange; x <= chunkRange; x++)
            {
                ChunkAccess surroundingChunk = serverLevel.getChunk(chunkX + x, chunkZ + z, ChunkStatus.SURFACE);
   
                if (surroundingChunk instanceof ImposterProtoChunk imposterProtoChunk)
                {
                    surroundingChunk = new ImposterProtoChunk(imposterProtoChunk.getWrapped(), true);
                }
                else if (surroundingChunk instanceof LevelChunk levelChunk)
                {
                    surroundingChunk = new ImposterProtoChunk(levelChunk, true);
                }
   
                surroundingChunks.add(surroundingChunk);
            }
        }
        final WorldGenRegion worldGenRegion = new OurWorldGenRegion(serverLevel, surroundingChunks);
        return chunkGenerator.createNoiseChunk(chunk,
            serverLevel.structureManager().forWorldGenRegion(worldGenRegion),
            Blender.of(worldGenRegion),
            serverLevel.getChunkSource().randomState());
    }

    /**
     * Checks if the block is water.
     *
     * @param iBlockState block state to be checked.
     * @return true if is water.
     */
    public static boolean isWater(final BlockState iBlockState)
    {
        return iBlockState.getBlock() == Blocks.WATER;
    }

    private static Item getItem(final BlockState blockState)
    {
        final Block block = blockState.getBlock();
        if (block.equals(Blocks.LAVA))
        {
            return Items.LAVA_BUCKET;
        }
        else if (block instanceof CropBlock)
        {
            final ItemStack stack = ((CropBlock) block).getCloneItemStack(null, null, blockState);
            if (stack != null)
            {
                return stack.getItem();
            }

            return Items.WHEAT_SEEDS;
        }
        // oh no... 
        else if (block instanceof FarmBlock || block instanceof DirtPathBlock)
        {
            return getItemFromBlock(Blocks.DIRT);
        }
        else if (block instanceof FireBlock)
        {
            return Items.FLINT_AND_STEEL;
        }
        else if (block instanceof FlowerPotBlock)
        {
            return Items.FLOWER_POT;
        }
        else if (block == Blocks.BAMBOO_SAPLING)
        {
            return Items.BAMBOO;
        }
        else
        {
            return getItemFromBlock(block);
        }
    }

    private static Item getItemFromBlock(final Block block)
    {
        return GameData.getBlockItemMap().get(block);
    }

    /**
     * For structure placement, check if two blocks are alike or if action has to be taken.
     * @param structureState the first blockState.
     * @param worldState the second blockState.
     * @param shallReplace the not solid condition.
     * @param fancy if fancy paste.
     * @param tileEntityData
     * @param worldEntity
     * @return true if nothing has to be done.
     */
    public static boolean areBlockStatesEqual(
      final BlockState structureState,
      final BlockState worldState,
      final Predicate<BlockState> shallReplace,
      final boolean fancy,
      final BiPredicate<BlockState, BlockState> specialEqualRule,
      final CompoundTag tileEntityData, final BlockEntity worldEntity)
    {
        if (structureState == null || worldState == null)
        {
            return true;
        }

        final Block structureBlock = structureState.getBlock();
        final Block worldBlock = worldState.getBlock();

        if (worldState.equals(structureState))
        {
            if (tileEntityData == null)
            {
                return true;
            }
            else if (worldEntity == null)
            {
                return false;
            }
            else if (worldEntity instanceof IMateriallyTexturedBlockEntity)
            {
                CompoundTag tag = tileEntityData.copy();
                tag.putInt("x", worldEntity.getBlockPos().getX());
                tag.putInt("y", worldEntity.getBlockPos().getY());
                tag.putInt("z", worldEntity.getBlockPos().getZ());
                return worldEntity.saveWithFullMetadata().equals(tag);
            }
            return true;
        }

        if (fancy)
        {
            if (structureBlock == ModBlocks.blockSubstitution.get())
            {
                return true;
            }

            if (structureBlock instanceof AirBlock && worldBlock instanceof AirBlock)
            {
                return true;
            }

            if (structureBlock == Blocks.DIRT && worldState.is(BlockTags.DIRT))
            {
                return true;
            }

            if (structureBlock == ModBlocks.blockSolidSubstitution.get() && !shallReplace.test(worldState))
            {
                return true;
            }

            // if the other block has fluid already or is not waterloggable, take no action
            if ((structureBlock == ModBlocks.blockFluidSubstitution.get()
                && (worldState.getFluidState().isSource()
                    || !worldState.hasProperty(BlockStateProperties.WATERLOGGED)
                    && worldState.getMaterial().isSolid()))
             || (worldBlock == ModBlocks.blockFluidSubstitution.get()
                && (structureState.getFluidState().isSource()
                    || !structureState.hasProperty(BlockStateProperties.WATERLOGGED)
                    && structureState.getMaterial().isSolid())))
            {
                return true;
            }
        }

        return specialEqualRule.test(structureState, worldState);
    }

    /**
     * Get a blockState from an itemStack.
     *
     * @param stack the stack to analyze.
     * @return the IBlockState.
     */
    public static BlockState getBlockStateFromStack(final ItemStack stack)
    {
        return getBlockStateFromStack(stack, Blocks.AIR.defaultBlockState());
    }

    /**
     * Get a blockState from an itemStack.
     *
     * @param stack the stack to analyze.
     * @param def   default blockstate if stack is not transformable
     * @return the IBlockState.
     */
    public static BlockState getBlockStateFromStack(final ItemStack stack, final BlockState def)
    {
        if (stack.getItem() == Items.AIR)
        {
            return Blocks.AIR.defaultBlockState();
        }
        else if (stack.getItem() instanceof BucketItem)
        {
            return ((BucketItem) stack.getItem()).getFluid().defaultFluidState().createLegacyBlock();
        }
        else if (stack.getItem() instanceof BlockItem)
        {
            return ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        }

        return def;
    }

    /**
     * Mimics pick block.
     *
     * @param blockState the block and state we are creating an ItemStack for.
     * @return ItemStack fromt the BlockState.
     */
    public static ItemStack getItemStackFromBlockState(final BlockState blockState)
    {
        if (blockState.getBlock() instanceof LiquidBlock)
        {
            return new ItemStack(((LiquidBlock) blockState.getBlock()).getFluid().getBucket(), 1);
        }
        final Item item = getItem(blockState);
        if (item != Items.AIR && item != null)
        {
            return new ItemStack(item, 1);
        }

        return new ItemStack(blockState.getBlock(), 1);
    }

    /**
     * Handle the placement of a specific block for a blockState at a certain position with a fakePlayer.
     *
     * @param world      the world object.
     * @param fakePlayer the fake player to place.
     * @param itemStack  the describing itemStack.
     * @param blockState the blockState in the world.
     * @param here       the position.
     */
    public static void handleCorrectBlockPlacement(
        final Level world,
        final FakePlayer fakePlayer,
        final ItemStack itemStack,
        final BlockState blockState,
        final BlockPos here)
    {
        final ItemStack stackToPlace = itemStack.copy();
        final Item item = stackToPlace.getItem();
        stackToPlace.setCount(stackToPlace.getMaxStackSize());

        if (item instanceof AirItem)
        {
            world.removeBlock(here, false);
        }
        else if (item instanceof BlockItem)
        {
            final Block targetBlock = ((BlockItem) item).getBlock();
            BlockState newState = copyFirstCommonBlockStateProperties(targetBlock.defaultBlockState(), blockState);

            if (newState == null)
            {
                fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, stackToPlace);
                if (stackToPlace.is(ItemTags.BEDS) && blockState.hasProperty(HorizontalDirectionalBlock.FACING))
                {
                    fakePlayer.setYRot(blockState.getValue(HorizontalDirectionalBlock.FACING).get2DDataValue() * 90);
                }

                newState = targetBlock.getStateForPlacement(new BlockPlaceContext(new UseOnContext(fakePlayer,
                    InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(0, 0, 0),
                        itemStack.getItem() instanceof BedItem ? Direction.UP : Direction.NORTH,
                        here,
                        true))));

                if (newState == null)
                {
                    return;
                }
            }

            // place
            world.setBlock(here, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            world.setBlock(here, newState, Constants.UPDATE_FLAG);
            targetBlock.setPlacedBy(world, here, newState, fakePlayer, stackToPlace);
        }
        else if (item instanceof BucketItem)
        {
            final Block sourceBlock = blockState.getBlock();
            final BucketItem bucket = (BucketItem) item;
            final Fluid fluid = bucket.getFluid();

            // place
            if (sourceBlock instanceof final LiquidBlockContainer liquidContainer)
            {
                if (liquidContainer.canPlaceLiquid(world, here, blockState, fluid))
                {
                    liquidContainer.placeLiquid(world, here, blockState, fluid.defaultFluidState());
                    bucket.checkExtraContent(null, world, stackToPlace, here);
                }
            }
            else
            {
                world.setBlock(here, Blocks.COBBLESTONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                world.setBlock(here, fluid.defaultFluidState().createLegacyBlock(), Constants.UPDATE_FLAG);
                bucket.checkExtraContent(null, world, stackToPlace, here);
            }
        }
        else
        {
            throw new IllegalArgumentException(
                MessageFormat.format("Cannot handle placing of {0} instead of {1}?!", itemStack.toString(), blockState.toString()));
        }
    }

    /**
     * Removes the fluid from the given position.
     * 
     * @param world the world to remove the fluid from.
     * @param pos   the position where to remove the fluid.
     */
    public static void removeFluid(Level world, BlockPos pos)
    {
        final BlockState state = world.getBlockState(pos);
        final Block block = state.getBlock();
        if((!(block instanceof BucketPickup) || ((BucketPickup)block).pickupBlock(world, pos, state).isEmpty()) && block instanceof LiquidBlock)
        {
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), Constants.UPDATE_FLAG);
        }
    }

    /**
     * A simple check to fetch the default fluid block for this dimension
     * @param world the world of the dimension
     * @return the default blockstate for the default fluid
     */
    public static BlockState getFluidForDimension(final Level world)
    {
        if (world instanceof ServerLevel serverLevel)
        {
            final ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
            if (generator instanceof NoiseBasedChunkGenerator chunkGenerator)
            {
                final BlockState defaultFluid = chunkGenerator.generatorSettings().value().defaultFluid();
                if (!defaultFluid.getFluidState().isEmpty())
                {
                    return defaultFluid;
                }
            }
        }
        return world == null || !world.dimensionType().ultraWarm() ? Blocks.WATER.defaultBlockState() : Blocks.LAVA.defaultBlockState();
    }

    /**
     * A simple check to fetch the default block for this dimension
     * @param level the world of the dimension
     * @param defaultValue return this if unable to get default block for given level
     * @return the default blockstate 
     */
    public static BlockState getDefaultBlockForLevel(final Level level, final BlockState defaultValue)
    {
        if (level instanceof ServerLevel serverLevel)
        {
            final ChunkGenerator generator = serverLevel.getChunkSource().getGenerator();
            if (generator instanceof NoiseBasedChunkGenerator chunkGenerator)
            {
                return chunkGenerator.generatorSettings().value().defaultBlock();
            }
        }
        return defaultValue;
    }

    /**
     * Returns a list of drops possible mining a specific block with specific
     * fortune level.
     *
     * @param world   World the block is in.
     * @param coords  Coordinates of the block.
     * @param fortune Level of fortune on the pickaxe.
     * @param stack the tool.
     * @return List of {@link ItemStack} with possible drops.
     */
    public static List<ItemStack> getBlockDrops(final Level world, final BlockPos coords, final int fortune, final ItemStack stack)
    {
        return world.getBlockState(coords).getDrops(new LootContext.Builder((ServerLevel) world)
                                                      .withLuck(fortune)
                                                      .withParameter(LootContextParams.ORIGIN, new Vec3(coords.getX(), coords.getY(), coords.getZ()))
                                                      .withOptionalParameter(LootContextParams.BLOCK_ENTITY, world.getBlockEntity(coords))
                                                      .withParameter(LootContextParams.TOOL, stack));
    }

    /**
     * Copies property values from propertiesOrigin into new blockstate made from target block.
     *
     * @param target           properties destination
     * @param propertiesOrigin properties source
     * @return blockState of target block with properties of propertiesOrigin or null if both blocks are not the same class
     */
    public static BlockState copyBlockStateProperties(final Block target, final BlockState propertiesOrigin)
    {
        return target.getClass().equals(propertiesOrigin.getBlock().getClass())
            ? unsafeCopyBlockStateProperties(target.defaultBlockState(), propertiesOrigin, propertiesOrigin.getProperties())
            : null;
    }

    /**
     * Copies property values from propertiesOrigin into new blockstate made from target Block.
     * If source and target are not the same block find the first common superclass and use its properties.
     *
     * @param target           properties destination
     * @param propertiesOrigin properties source
     * @return blockState of target block with properties of common super class or null if no common superclass found
     */
    public static BlockState copyFirstCommonBlockStateProperties(final BlockState target, final BlockState propertiesOrigin)
    {
        final BlockState sameClass = copyBlockStateProperties(target.getBlock(), propertiesOrigin);
        if (sameClass != null)
        {
            return sameClass;
        }

        final Class<?> firstCommonClass = JavaUtils.getFirstCommonSuperClass(target.getBlock().getClass(), propertiesOrigin.getBlock().getClass());
        if (firstCommonClass == Block.class || !Block.class.isAssignableFrom(firstCommonClass))
        {
            return null;
        }

        // It would be the best to get properties of firstCommonClass but since defaultstate is non-static and created in top level block classes
        // it's literally impossible to get them
        final Collection<Property<?>> properties = new ArrayList<>(target.getProperties());
        properties.retainAll(propertiesOrigin.getProperties());
        return unsafeCopyBlockStateProperties(target, propertiesOrigin, properties);
    }

    /**
     * Copies property values from properties using propertiesOrigin into new blockstate made from target Block.
     *
     * @param target           properties destination
     * @param propertiesOrigin properties source
     * @param properties       which properties to copy
     * @return blockState of target with given properties of propertiesOrigin
     * @throws IllegalArgumentException if target does not accept any of properties
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState unsafeCopyBlockStateProperties(final BlockState target,
        final BlockState propertiesOrigin,
        final Collection<Property<?>> properties)
    {
        BlockState blockState = target;
        for (final Property property : properties)
        {
            blockState = blockState.setValue(property, propertiesOrigin.getValue(property));
        }
        return blockState;
    }

    private static class OurWorldGenRegion extends WorldGenRegion
    {
        private OurWorldGenRegion(ServerLevel p_143484_, List<ChunkAccess> p_143485_)
        {
            super(p_143484_, p_143485_, null, -1);
        }

        @Override
        public boolean destroyBlock(BlockPos p_9550_, boolean p_9551_, @Nullable Entity p_9552_, int p_9553_)
        {
            return false;
        }

        @Override
        public boolean ensureCanWrite(BlockPos p_181031_)
        {
            return false;
        }

        @Override
        public boolean setBlock(BlockPos p_9539_, BlockState p_9540_, int p_9541_, int p_9542_)
        {
            return false;
        }

        @Override
        public boolean addFreshEntity(Entity p_9580_)
        {
            return false;
        }

        @Override
        public boolean removeBlock(BlockPos p_9547_, boolean p_9548_)
        {
            return false;
        }
    }
}
