package com.ldtteam.structurize.api.configuration;

import net.minecraftforge.common.config.Config;

import static com.ldtteam.structurize.api.util.constant.Constants.*;

@Config(modid = MOD_ID)
public class Configurations
{
    @Config.Comment("All configuration related to gameplay")
    public static Gameplay gameplay = new Gameplay();

    public static class Gameplay
    {
        @Config.Comment("Should the default schematics be ignored (from the jar)?")
        public boolean ignoreSchematicsFromJar = false;

        @Config.Comment("Should player made schematics be allowed")
        public boolean allowPlayerSchematics = true;

        @Config.Comment("Max world operations per tick (Max blocks to place, remove or replace)")
        public int maxOperationsPerTick = 1000;

        @Config.Comment("Max amount of changes cached to be able to undo")
        public int maxCachedChanges = 10;

        @Config.Comment("Max amount of schematics to be cached on the server")
        public int maxCachedSchematics = 100;

        @Config.Comment("Max amount of blocks checked by a possible worker.")
        public int maxBlocksChecked = 1000;
    }
}
