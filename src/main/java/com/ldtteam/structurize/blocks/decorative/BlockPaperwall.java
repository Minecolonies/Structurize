package com.ldtteam.structurize.blocks.decorative;

import com.ldtteam.structurize.blocks.AbstractBlockStructurizePane;
import com.ldtteam.structurize.blocks.types.PaperwallType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraftforge.registries.IForgeRegistry;

/**
 * The paperwall block class defining the paperwall.
 */
public class BlockPaperwall extends AbstractBlockStructurizePane<BlockPaperwall>
{
    /**
     * The variants for the paperwall.
     */
    public static final EnumProperty<PaperwallType> VARIANT = EnumProperty.create("variant", PaperwallType.class);

    /**
     * This block's name.
     */
    public static final String                      BLOCK_NAME     = "blockpaperwall";

    /**
     * The hardness this block has.
     */
    private static final float                      BLOCK_HARDNESS = 3F;

    /**
     * The resistance this block has.
     */
    private static final float                      RESISTANCE     = 1F;

    public BlockPaperwall(final String type)
    {
        super(Properties.create(Material.GLASS).hardnessAndResistance(BLOCK_HARDNESS, RESISTANCE));
        setRegistryName(type + "_" + BLOCK_NAME);
    }

    /**
     * Registry block at gameregistry.
     *
     * @param registry the registry to use.
     */
    @Override
    public void registerItemBlock(final IForgeRegistry<Item> registry, final Item.Properties properties)
    {
        registry.register((new BlockItem(this, properties)).setRegistryName(this.getRegistryName()));
    }

    @Override
    public BlockState rotate(final BlockState state, final IWorld world, final BlockPos pos, final Rotation direction)
    {
        switch (direction)
        {
            case CLOCKWISE_180:
                return state.with(NORTH, state.get(SOUTH))
                         .with(EAST, state.get(WEST)).with(SOUTH, state.get(NORTH))
                         .with(WEST, state.get(EAST));
            case COUNTERCLOCKWISE_90:
                return state.with(NORTH, state.get(EAST))
                         .with(EAST, state.get(SOUTH)).with(SOUTH, state.get(WEST))
                         .with(WEST, state.get(NORTH));
            case CLOCKWISE_90:
                return state.with(NORTH, state.get(WEST))
                         .with(EAST, state.get(NORTH)).with(SOUTH, state.get(EAST))
                         .with(WEST, state.get(SOUTH));
            default:
                return state;
        }
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, WEST, SOUTH, VARIANT, WATERLOGGED);
    }
}
