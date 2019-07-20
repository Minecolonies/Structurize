package com.ldtteam.structurize.blocks.decorative;

import com.ldtteam.structurize.api.util.constant.Constants;
import com.ldtteam.structurize.blocks.AbstractBlockStructurizeDirectional;
import com.ldtteam.structurize.blocks.types.ShingleSlabType;
import com.ldtteam.structurize.creativetab.ModCreativeTabs;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.BlockState;
import net.minecraft.state.EnumProperty;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Decorative block
 */
public class BlockShingleSlab extends AbstractBlockStructurizeDirectional<BlockShingleSlab>
{
    /**
     * The bounding box of the slab.
     */
    protected static final AxisAlignedBB AABB_BOTTOM_HALF = new AxisAlignedBB(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);

    /**
     * The variants of the shingle slab.
     */
    public static final EnumProperty<ShingleSlabType> VARIANT = EnumProperty.create("variant", ShingleSlabType.class);

    /**
     * The hardness this block has.
     */
    private static final float BLOCK_HARDNESS = 3F;

    /**
     * This blocks name.
     */
    private static final String BLOCK_NAME = "blockshingleslab";

    /**
     * The resistance this block has.
     */
    private static final float RESISTANCE = 1F;

    /**
     * Amount of connections with other shingle slabs.
     */
    private static final int NO_CONNECTIONS    = 0;
    private static final int THREE_CONNECTIONS = 1;
    private static final int TWO_CONNECTIONS   = 2;
    private static final int ONE_CONNECTION    = 3;

    /**
     * Constructor for the TimberFrame
     */
    public BlockShingleSlab()
    {
        super(Properties.create(Material.WOOD).hardnessAndResistance(BLOCK_HARDNESS, RESISTANCE));
        setRegistryName(Constants.MOD_ID.toLowerCase() + ":" + BLOCK_NAME);
    }

    /**
     * Convert the BlockState into the correct metadata value
     */
    @Override
    public int getMetaFromState(@NotNull final BlockState state)
    {
        return state.getValue(FACING).getHorizontalIndex();
    }

    /**
     * @deprecated remove when minecraft invents something better.
     */
    @Deprecated
    @Override
    public BlockState getStateFromMeta(final int meta)
    {
        return this.getDefaultState().withProperty(FACING, EnumFacing.HORIZONTALS[meta]);
    }

    /**
     * @deprecated remove when minecraft invents something better.
     */
    @Deprecated
    @Override
    public AxisAlignedBB getBoundingBox(@NotNull final BlockState state, @NotNull final IBlockAccess source, @NotNull final BlockPos pos)
    {
        return AABB_BOTTOM_HALF;
    }

    /**
     * @deprecated remove when minecraft invents something better.
     */
    @Deprecated
    @Override
    public BlockState getActualState(@NotNull final BlockState state, @NotNull final IBlockAccess worldIn, @NotNull final BlockPos pos)
    {
        return getSlabShape(state, worldIn, pos);
    }

    /**
     * Get the step shape of the slab
     * @param state the state.
     * @param world the world.
     * @param position the position.Re
     * @return the blockState to use.
     */
    private static BlockState getSlabShape(@NotNull final BlockState state, @NotNull final IBlockAccess world, @NotNull final BlockPos position)
    {
        final boolean[] connectors = new boolean[]{!(world.getBlockState(position.east()).getBlock() instanceof BlockShingleSlab),
            !(world.getBlockState(position.west()).getBlock() instanceof BlockShingleSlab),
            !(world.getBlockState(position.north()).getBlock() instanceof BlockShingleSlab),
            !(world.getBlockState(position.south()).getBlock() instanceof BlockShingleSlab)};

        int amount = 0;
        for(final boolean check: connectors)
        {
            if(check)
            {
                amount++;
            }
        }

        if(amount == NO_CONNECTIONS)
        {
            return state.withProperty(VARIANT, ShingleSlabType.TOP);
        }
        if(amount == THREE_CONNECTIONS)
        {
            if (connectors[0])
            {
                return state.withProperty(VARIANT, ShingleSlabType.ONE_WAY).withProperty(FACING, EnumFacing.SOUTH);
            }
            else if (connectors[1])
            {
                return state.withProperty(VARIANT, ShingleSlabType.ONE_WAY).withProperty(FACING, EnumFacing.NORTH);
            }
            else if (connectors[2])
            {
                return state.withProperty(VARIANT, ShingleSlabType.ONE_WAY).withProperty(FACING, EnumFacing.EAST);
            }
            return state.withProperty(VARIANT, ShingleSlabType.ONE_WAY).withProperty(FACING, EnumFacing.WEST);
        }
        else if(amount == TWO_CONNECTIONS)
        {
            if (connectors[0] && connectors[1] && !connectors[2] && !connectors[3])
            {
                return state.withProperty(VARIANT, ShingleSlabType.TWO_WAY).withProperty(FACING, EnumFacing.EAST);
            }
            else if (!connectors[0] && !connectors[1] && connectors[2] && connectors[3])
            {
                return state.withProperty(VARIANT, ShingleSlabType.TWO_WAY).withProperty(FACING, EnumFacing.NORTH);
            }
            else if(!connectors[0] && connectors[1] && connectors[2] && !connectors[3])
            {
                return state.withProperty(VARIANT, ShingleSlabType.CURVED).withProperty(FACING, EnumFacing.WEST);
            }
            else if(connectors[0] && !connectors[1] && !connectors[2] && connectors[3])
            {
                return state.withProperty(VARIANT, ShingleSlabType.CURVED).withProperty(FACING, EnumFacing.EAST);
            }
            else if(!connectors[0] && connectors[1] && !connectors[2] && connectors[3])
            {
                return state.withProperty(VARIANT, ShingleSlabType.CURVED).withProperty(FACING, EnumFacing.SOUTH);
            }
            return state.withProperty(VARIANT, ShingleSlabType.CURVED).withProperty(FACING, EnumFacing.NORTH);
        }
        else if(amount == ONE_CONNECTION)
        {
            if (!connectors[0] && !world.isAirBlock(position.west().down()))
            {
                return state.withProperty(VARIANT, ShingleSlabType.THREE_WAY).withProperty(FACING, EnumFacing.NORTH);
            }
            else if (!connectors[1] && !world.isAirBlock(position.east().down()))
            {
                return state.withProperty(VARIANT, ShingleSlabType.THREE_WAY).withProperty(FACING, EnumFacing.SOUTH);
            }
            else if (!connectors[2] && !world.isAirBlock(position.south().down()))
            {
                return state.withProperty(VARIANT, ShingleSlabType.THREE_WAY).withProperty(FACING, EnumFacing.WEST);
            }
            else if (!connectors[3] && !world.isAirBlock(position.north().down()))
            {
                return state.withProperty(VARIANT, ShingleSlabType.THREE_WAY).withProperty(FACING, EnumFacing.EAST);
            }
            return state.withProperty(VARIANT, ShingleSlabType.TWO_WAY)
                    .withProperty(FACING, !connectors[0] || !connectors[1] ? EnumFacing.NORTH : EnumFacing.EAST);
        }
        return state.withProperty(VARIANT, ShingleSlabType.FOUR_WAY);
    }

    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {FACING, VARIANT});
    }

    /**
     * @deprecated remove when minecraft invents something better.
     */
    @Deprecated
    @Override
    public boolean isOpaqueCube(@NotNull final BlockState state)
    {
        return false;
    }

    /**
     * @deprecated remove when minecraft invents something better.
     */
    @Deprecated
    @Override
    public boolean isFullCube(@NotNull final BlockState state)
    {
        return false;
    }

    @Override
    public boolean doesSideBlockRendering(@NotNull final BlockState state, @NotNull final IBlockAccess world, @NotNull final BlockPos pos, @NotNull final EnumFacing face)
    {
        return false;
    }
}
