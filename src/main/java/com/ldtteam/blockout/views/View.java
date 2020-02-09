package com.ldtteam.blockout.views;

import com.ldtteam.blockout.Alignment;
import com.ldtteam.blockout.Loader;
import com.ldtteam.blockout.Pane;
import com.ldtteam.blockout.PaneParams;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * A View is a Pane which can contain other Panes.
 */
public class View extends Pane
{
    @NotNull
    protected List<Pane> children = new ArrayList<>();
    protected int padding = 0;

    /**
     * Constructs a barebones View.
     */
    public View()
    {
        super();
    }

    /**
     * Constructs a View from PaneParams.
     *
     * @param params Params for the View.
     */
    public View(final PaneParams params)
    {
        super(params);
        padding = params.getIntAttribute("padding", padding);
    }

    @NotNull
    public List<Pane> getChildren()
    {
        return children;
    }

    @Override
    public void parseChildren(final PaneParams params)
    {
        final List<PaneParams> childNodes = params.getChildren();
        if (childNodes == null)
        {
            return;
        }

        for (final PaneParams node : childNodes)
        {
            Loader.createFromPaneParams(node, this);
        }
    }

    @Override
    public void drawSelf(final int mx, final int my)
    {
        // Translate the drawing origin to our x,y.
        RenderSystem.pushMatrix();

        final int paddedX = x + padding;
        final int paddedY = y + padding;

        RenderSystem.translatef((float) paddedX, (float) paddedY, 0.0f);

        // Translate Mouse into the View
        final int drawX = mx - paddedX;
        final int drawY = my - paddedY;

        children.stream().filter(this::childIsVisible).forEach(child -> child.draw(drawX, drawY));

        RenderSystem.popMatrix();
    }

    @Override
    public boolean scrollInput(final double wheel, final double mx, final double my)
    {
        final int mxChild = (int) mx - x - padding;
        final int myChild = (int) my - y - padding;

        for (final Pane child : children)
        {
            if (child != null && child.isPointInPane(mxChild, myChild))
            {
                return child.scrollInput(wheel, mx, my);
            }
        }
        return false;
    }

    @Override
    public void handleHover(final double mx, final double my)
    {
        for (final Pane child : children)
        {
            if (child != null)
            {
                child.handleHover(mx, my);
            }
        }

    }

    @Nullable
    @Override
    public Pane findPaneByID(final String id)
    {
        if (this.id.equals(id))
        {
            return this;
        }

        for (final Pane child : children)
        {
            final Pane found = child.findPaneByID(id);
            if (found != null)
            {
                return found;
            }
        }

        return null;
    }

    @Override
    public void setWindow(final Window w)
    {
        super.setWindow(w);
        for (final Pane child : children)
        {
            child.setWindow(w);
        }
    }

    // Mouse
    @Override
    public boolean rightClick(final double mx, final double my)
    {
        final double mxChild = mx - x - padding;
        final double myChild = my - y - padding;

        for (final Pane child : children)
        {
            if (child != null && child.isPointInPane(mxChild, myChild))
            {
                return child.rightClick(mxChild, myChild);
            }
        }
        return false;
    }

    // Mouse
    @Override
    public boolean click(final double mx, final double my)
    {
        final double mxChild = mx - x - padding;
        final double myChild = my - y - padding;

        for (final Pane child : children)
        {
            if (child != null && child.isPointInPane(mxChild, myChild))
            {
                return child.click(mxChild, myChild);
            }
        }
        return false;
    }

    /**
     * Return a Pane that will handle a click action at the specified mouse
     * coordinates.
     *
     * @param mx Mouse X, relative to the top-left of this Pane.
     * @param my Mouse Y, relative to the top-left of this Pane.
     * @return a Pane that will handle a click action.
     */
    @Nullable
    public Pane findPaneForClick(final double mx, final double my)
    {
        final ListIterator<Pane> it = children.listIterator(children.size());

        // Iterate in reverse, since Panes later in the list draw on top of earlier panes.
        while (it.hasPrevious())
        {
            final Pane child = it.previous();
            if (child.canHandleClick(mx, my))
            {
                return child;
            }
        }

        return null;
    }

    @Override
    public void onUpdate()
    {
        children.forEach(Pane::onUpdate);
    }

    protected boolean childIsVisible(final Pane child)
    {
        return child.getX() < getInteriorWidth() && child.getY() < getInteriorHeight() && (child.getX() + child.getWidth()) >= 0 &&
            (child.getY() + child.getHeight()) >= 0;
    }

    public int getInteriorWidth()
    {
        return width - (padding * 2);
    }

    public int getInteriorHeight()
    {
        return height - (padding * 2);
    }

    /**
     * Add child Pane to this view.
     *
     * @param child pane to add.
     */
    public void addChild(final Pane child)
    {
        child.setWindow(getWindow());
        children.add(child);
        adjustChild(child);
        child.setParentView(this);

    }

    protected void adjustChild(final Pane child)
    {
        int childX = child.getX();
        int childY = child.getY();
        int childWidth = child.getWidth();
        int childHeight = child.getHeight();

        // Negative width = 100% of parents width minus abs(width).
        if (childWidth < 0)
        {
            childWidth = Math.max(0, getInteriorWidth() + childWidth);
        }

        final Alignment alignment = child.getAlignment();

        // Adjust for horizontal alignment.
        if (alignment.isRightAligned())
        {
            childX = (getInteriorWidth() - childWidth) - childX;
        }
        else if (alignment.isHorizontalCentered())
        {
            childX = ((getInteriorWidth() - childWidth) / 2) + childX;
        }

        // Negative height = 100% of parents height minus abs(height).
        if (childHeight < 0)
        {
            childHeight = Math.max(0, getInteriorHeight() + childHeight);
        }

        // Adjust for vertical alignment.
        if (alignment.isBottomAligned())
        {
            childY = (getInteriorHeight() - childHeight) - childY;
        }
        else if (alignment.isVerticalCentered())
        {
            childY = ((getInteriorHeight() - childHeight) / 2) + childY;
        }

        child.setSize(childWidth, childHeight);
        child.setPosition(childX, childY);
    }

    /**
     * Remove pane from view.
     *
     * @param child pane to remove.
     */
    public void removeChild(final Pane child)
    {
        children.remove(child);
    }

    @Override
    public boolean onMouseDrag(final double x, final double y, final int speed, final double deltaX, final double deltaY)
    {
        final double mxChild = x - this.x - padding;
        final double myChild = y - this.y - padding;

        for (final Pane child : children)
        {
            if (child != null && child.isPointInPane((int) mxChild, (int) myChild))
            {
                return child.onMouseDrag(mxChild, myChild, speed, deltaX, deltaY);
            }
        }
        return false;
    }
}
