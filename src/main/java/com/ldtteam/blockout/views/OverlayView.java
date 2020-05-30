package com.ldtteam.blockout.views;

import com.ldtteam.blockout.PaneParams;
import org.lwjgl.glfw.GLFW;

/**
 * An OverlayView is a full screen View which is displayed on top of the window.
 */
public class OverlayView extends View
{
    /**
     * Constructs a barebones View.
     */
    public OverlayView()
    {
        super();
    }

    /**
     * Constructs a OverlayView from PaneParams.
     *
     * @param params Params for the View.
     */
    public OverlayView(final PaneParams params)
    {
        super(params);
    }

    /**
     * hide the view when click on.
     */
    @Override
    public boolean click(final double mx, final double my)
    {
        if (super.click(mx, my))
        {
            return true;
        }

        hide();
        return false;
    }

    /**
     * hide the view when click on.
     */
    @Override
    public boolean rightClick(final double mx, final double my)
    {
        if (super.rightClick(mx, my))
        {
            return true;
        }

        hide();
        return false;
    }

    /**
     * Called when a key is pressed.
     * hide the view when ESC is pressed.
     *
     * @param ch  the character.
     * @param key the key.
     * @return false at all times - do nothing.
     */
    @Override
    public boolean onKeyTyped(final char ch, final int key)
    {
        if (isVisible() && key == GLFW.GLFW_KEY_ESCAPE)
        {
            setVisible(false);
            return true;
        }

        return super.onKeyTyped(ch, key);
    }
}
