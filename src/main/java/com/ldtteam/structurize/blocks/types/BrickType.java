package com.ldtteam.structurize.blocks.types;

import com.ldtteam.structurize.blocks.ModBlocks;
import com.ldtteam.structurize.items.ModItemGroups;
import com.ldtteam.structurize.items.ModItems;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraftforge.fml.RegistryObject;

import java.util.List;

public enum BrickType implements IBlockCollection
{
    BROWN("brown", Items.TERRACOTTA),
    BEIGE("beige", Items.GRAVEL),
    CREAM("cream", Items.SANDSTONE);

    private static final String                 SUFFIX = "_brick";
    private final List<RegistryObject<Block>> blocks;
    private final        String                 name;
    public final         Item            ingredient;

    BrickType(final String name, final Item ingredient)
    {
        this.name = name;
        this.ingredient = ingredient;

        blocks = create(
          ModBlocks.getRegistry(), ModItems.getRegistry(),
          ModItemGroups.CONSTRUCTION,
          IBlockCollection.BlockType.BLOCK,
          IBlockCollection.BlockType.SLAB,
          IBlockCollection.BlockType.STAIRS,
          IBlockCollection.BlockType.WALL);
    }

    @Override
    public String getName()
    {
        return this.name + SUFFIX;
    }

    @Override
    public List<RegistryObject<Block>> getBlocks()
    {
        return blocks;
    }
}
