package com.ldtteam.structurize.api.util;

import com.ldtteam.structurize.api.util.constant.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Logging utility class.
 */
public final class Log
{
    /**
     * Mod logger.
     */
    private static Logger logger = null;

    /**
     * Private constructor to hide the public one.
     */
    private Log()
    {
        /*
         * Intentionally left empty.
         */
    }

    /**
     * Getter for the structurize Logger.
     *
     * @return the logger.
     */
    public static Logger getLogger()
    {
        //Only create logger if current logger is empty.
        if (logger == null)
        {
            Log.logger = LogManager.getLogger(Constants.MOD_ID);
        }
        return logger;
    }
}
