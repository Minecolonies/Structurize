package com.ldtteam.structurize.blocks.cactus;

import com.ldtteam.structurize.api.util.constant.Constants;
import com.ldtteam.structurize.blocks.AbstractBlockStructurize;
import com.ldtteam.structurize.creativetab.ModCreativeTabs;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

import java.util.Locale;

public class BlockCactusPlank extends AbstractBlockStructurize<BlockCactusPlank>
{


    public BlockCactusPlank()
    {
        super(Material.WOOD, MapColor.GREEN);
        setRegistryName(Constants.MOD_ID.toLowerCase() + ":" + "blockcactusplank");
        setTranslationKey(Constants.MOD_ID.toLowerCase(Locale.ENGLISH) + "." + "blockcactusplank");
        setCreativeTab(ModCreativeTabs.STRUCTURIZE);
        setSoundType(SoundType.WOOD);

    }
}
