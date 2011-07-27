/*
 * SEG modification of AWTInputHandler.java (gov.nasa.worldwind.awt)
 * =====================================================================
 * Copyright (C) 2009 Shawn E. Gano
 *
 * This file is part of JSatTrak.
 *
 * JSatTrak is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JSatTrak is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JSatTrak.  If not, see <http://www.gnu.org/licenses/>.
 * =====================================================================
*/
package View;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.DragSelectEvent;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.util.Logging;

import javax.swing.event.EventListenerList;
import java.awt.*;
import java.awt.event.*;

/**
 * @author tag
 * @version $Id: AWTInputHandler.java 5121 2008-04-22 17:54:54Z tgaskins $
 */
public class BasicModelViewInputHandler3 extends WWObjectImpl
        implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener, FocusListener, InputHandler,
    Disposable
{
    private WorldWindow wwd = null;
    private final EventListenerList eventListeners = new EventListenerList();
    private java.awt.Point mousePoint = new java.awt.Point();
    private PickedObjectList hoverObjects;
    private PickedObjectList objectsAtButtonPress;
    private boolean isHovering = false;
    private boolean isDragging = false;
    private javax.swing.Timer hoverTimer = new javax.swing.Timer(600, new ActionListener()
    {
        @Override
        public void actionPerformed(ActionEvent actionEvent)
        {
            if (BasicModelViewInputHandler3.this.pickMatches(BasicModelViewInputHandler3.this.hoverObjects))
            {
                BasicModelViewInputHandler3.this.isHovering = true;
                BasicModelViewInputHandler3.this.callSelectListeners(new SelectEvent(BasicModelViewInputHandler3.this.wwd,
                        SelectEvent.HOVER, mousePoint, BasicModelViewInputHandler3.this.hoverObjects));
                BasicModelViewInputHandler3.this.hoverTimer.stop();
            }
        }
    });
    // Delegate handler for View.
    private final BasicModelViewInputBroker3 viewInputBroker = new BasicModelViewInputBroker3();
    private SelectListener selectListener;

    public void clear()
    {
        if (this.hoverObjects != null)
            this.hoverObjects.clear();
        this.hoverObjects = null;

        if (this.objectsAtButtonPress != null)
            this.objectsAtButtonPress.clear();
        this.objectsAtButtonPress = null;
    }

    // -- after upgrade to WWJ v0.6
    @Override
    public void dispose()
    {
        this.hoverTimer.stop();
        this.hoverTimer = null;

        this.setEventSource(null);

        if (this.hoverObjects != null)
            this.hoverObjects.clear();
        this.hoverObjects = null;

        if (this.objectsAtButtonPress != null)
            this.objectsAtButtonPress.clear();
        this.objectsAtButtonPress = null;
    }

    @Override
    public void setEventSource(WorldWindow newWorldWindow)
    {
        if (newWorldWindow != null && !(newWorldWindow instanceof Component))
        {
            String message = Logging.getMessage("Awt.AWTInputHandler.EventSourceNotAComponent");
            Logging.logger().finer(message);
            throw new IllegalArgumentException(message);
        }

        if (newWorldWindow == this.wwd)
        {
            return;
        }

        if (this.wwd != null)
        {
            Component c = (Component) this.wwd;
            c.removeKeyListener(this);
            c.removeMouseMotionListener(this);
            c.removeMouseListener(this);
            c.removeMouseWheelListener(this);
            c.removeFocusListener(this);
        }

        this.wwd = newWorldWindow;
        this.viewInputBroker.setWorldWindow(this.wwd);

        if (this.wwd == null)
        {
            return;
        }

        Component c = (java.awt.Component) this.wwd;
        c.addKeyListener(this);
        c.addMouseMotionListener(this);
        c.addMouseListener(this);
        c.addMouseWheelListener(this);
        c.addFocusListener(this);

        selectListener = new SelectListener()
        {
            @Override
            public void selected(SelectEvent event)
            {
                if (event.getEventAction().equals(SelectEvent.ROLLOVER))
                {
                    //doHover(true);
                }
            }
        };
        this.wwd.addSelectListener(selectListener);
    }

    public void removeHoverSelectListener()
    {
        hoverTimer.stop();
        hoverTimer = null;
        this.wwd.removeSelectListener(selectListener);
    }

    @Override
    public WorldWindow getEventSource()
    {
        return this.wwd;
    }

    @Override
    public void setHoverDelay(int delay)
    {
        this.hoverTimer.setDelay(delay);
    }

    @Override
    public int getHoverDelay()
    {
        return this.hoverTimer.getDelay();
    }

    public boolean isSmoothViewChanges()
    {
        return this.viewInputBroker.isSmoothViewChanges();
    }

    public void setSmoothViewChanges(boolean smoothViewChanges)
    {
        this.viewInputBroker.setSmoothViewChanges(smoothViewChanges);
    }

    public boolean isLockViewHeading()
    {
        return this.viewInputBroker.isLockHeading();
    }

    public void setLockViewHeading(boolean lockHeading)
    {
        this.viewInputBroker.setLockHeading(lockHeading);
    }

    protected WorldWindow getWorldWindow()
    {
        return wwd;
    }

    protected Point getMousePoint()
    {
        return mousePoint;
    }

    protected void setMousePoint(Point mousePoint)
    {
        this.mousePoint = mousePoint;
    }

    protected boolean isHovering()
    {
        return isHovering;
    }

    protected void setHovering(boolean hovering)
    {
        isHovering = hovering;
    }

    protected boolean isDragging()
    {
        return isDragging;
    }

    protected void setDragging(boolean dragging)
    {
        isDragging = dragging;
    }

    protected PickedObjectList getHoverObjects()
    {
        return hoverObjects;
    }

    protected void setHoverObjects(PickedObjectList hoverObjects)
    {
        this.hoverObjects = hoverObjects;
    }

    protected PickedObjectList getObjectsAtButtonPress()
    {
        return objectsAtButtonPress;
    }

    protected void setObjectsAtButtonPress(PickedObjectList objectsAtButtonPress)
    {
        this.objectsAtButtonPress = objectsAtButtonPress;
    }

    protected BasicModelViewInputBroker3 getViewInputBroker()
    {
        return viewInputBroker;
    }

    @Override
    public void keyTyped(KeyEvent keyEvent)
    {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (keyEvent == null)
        {
            return;
        }

        this.viewInputBroker.keyTyped(keyEvent);
    }

    @Override
    public void keyPressed(KeyEvent keyEvent)
    {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (keyEvent == null)
        {
            return;
        }

        this.viewInputBroker.keyPressed(keyEvent);
    }

    @Override
    public void keyReleased(KeyEvent keyEvent)
    {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (keyEvent == null)
        {
            return;
        }

        this.viewInputBroker.keyReleased(keyEvent);
    }

    @Override
    public void mouseClicked(final MouseEvent mouseEvent)
    {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (this.wwd.getView() == null)
        {
            return;
        }

        if (mouseEvent == null)
        {
            return;
        }

        PickedObjectList pickedObjects = this.wwd.getObjectsAtCurrentPosition();

        this.callMouseClickedListeners(mouseEvent);

        if (pickedObjects != null && pickedObjects.getTopPickedObject() != null
                && !pickedObjects.getTopPickedObject().isTerrain())
        {
            // Something is under the cursor, so it's deemed "selected".
            if (MouseEvent.BUTTON1 == mouseEvent.getButton())
            {
                if (mouseEvent.getClickCount() % 2 == 1)
                {
                    this.callSelectListeners(new SelectEvent(this.wwd, SelectEvent.LEFT_CLICK,
                            mouseEvent, pickedObjects));
                }
                else
                {
                    this.callSelectListeners(new SelectEvent(this.wwd, SelectEvent.LEFT_DOUBLE_CLICK,
                            mouseEvent, pickedObjects));
                }
            }
            else if (MouseEvent.BUTTON3 == mouseEvent.getButton())
            {
                this.callSelectListeners(new SelectEvent(this.wwd, SelectEvent.RIGHT_CLICK,
                        mouseEvent, pickedObjects));
            }

            this.wwd.getView().firePropertyChange(AVKey.VIEW, null, this.wwd.getView());
        }
        else
        {
            if (!mouseEvent.isConsumed())
            {
                this.viewInputBroker.mouseClicked(mouseEvent);
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent)
    {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (mouseEvent == null)
        {
            return;
        }

        this.mousePoint = mouseEvent.getPoint();
        this.cancelHover();
        this.cancelDrag();

        this.objectsAtButtonPress = this.wwd.getObjectsAtCurrentPosition();

        this.callMousePressedListeners(mouseEvent);

        if (this.objectsAtButtonPress != null && objectsAtButtonPress.getTopPickedObject() != null
                && !this.objectsAtButtonPress.getTopPickedObject().isTerrain())
        {
            // Something is under the cursor, so it's deemed "selected".
            if (MouseEvent.BUTTON1 == mouseEvent.getButton())
            {
                this.callSelectListeners(new SelectEvent(this.wwd, SelectEvent.LEFT_PRESS,
                        mouseEvent, this.objectsAtButtonPress));
            }
            else if (MouseEvent.BUTTON3 == mouseEvent.getButton())
            {
                this.callSelectListeners(new SelectEvent(this.wwd, SelectEvent.RIGHT_PRESS,
                        mouseEvent, this.objectsAtButtonPress));
            }

            // TODO: Why is this event fired?
            this.wwd.getView().firePropertyChange(AVKey.VIEW, null, this.wwd.getView());
        }
        else
        {
            if (!mouseEvent.isConsumed())
            {
                this.viewInputBroker.mousePressed(mouseEvent);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent)
    {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (mouseEvent == null)
        {
            return;
        }

        this.mousePoint = mouseEvent.getPoint();
        this.callMouseReleasedListeners(mouseEvent);
        if (!mouseEvent.isConsumed())
        {
            this.viewInputBroker.mouseReleased(mouseEvent);
        }
        //this.doHover(true);
        this.cancelDrag();
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent)
    {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (mouseEvent == null)
        {
            return;
        }

        this.viewInputBroker.mouseEntered(mouseEvent);
        this.cancelHover();
        this.cancelDrag();
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent)
    {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (mouseEvent == null)
        {
            return;
        }

        this.viewInputBroker.mouseExited(mouseEvent);
        this.cancelHover();
        this.cancelDrag();
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent)
    {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (mouseEvent == null)
        {
            return;
        }

        Point prevMousePoint = this.mousePoint;
        this.mousePoint = mouseEvent.getPoint();
        this.callMouseDraggedListeners(mouseEvent);

        if (MouseEvent.BUTTON1_DOWN_MASK == mouseEvent.getModifiersEx())
        {
            PickedObjectList pickedObjects = this.objectsAtButtonPress;
            if (this.isDragging
                    || (pickedObjects != null && pickedObjects.getTopPickedObject() != null
                    && !pickedObjects.getTopPickedObject().isTerrain()))
            {
                this.isDragging = true;
                this.callSelectListeners(new DragSelectEvent(this.wwd, SelectEvent.DRAG, mouseEvent, pickedObjects,
                        prevMousePoint));
            }
        }

        if (!this.isDragging)
        {
            if (!mouseEvent.isConsumed())
            {
                this.viewInputBroker.mouseDragged(mouseEvent);
            }
        }

        // Redraw to update the current position and selection.
        if (this.wwd.getSceneController() != null)
        {
            this.wwd.getSceneController().setPickPoint(mouseEvent.getPoint());
            this.wwd.redraw();
        }
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent)
    {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (mouseEvent == null)
        {
            return;
        }

        this.mousePoint = mouseEvent.getPoint();
        this.callMouseMovedListeners(mouseEvent);

        if (!mouseEvent.isConsumed())
        {
            this.viewInputBroker.mouseMoved(mouseEvent);
        }

        // Redraw to update the current position and selection.
        if (this.wwd.getSceneController() != null)
        {
            this.wwd.getSceneController().setPickPoint(mouseEvent.getPoint());
            this.wwd.redraw();
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mouseWheelEvent)
    {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (mouseWheelEvent == null)
        {
            return;
        }

        this.viewInputBroker.mouseWheelMoved(mouseWheelEvent);
    }

    @Override
    public void focusGained(FocusEvent focusEvent)
    {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (focusEvent == null)
        {
            return;
        }

        this.viewInputBroker.focusGained(focusEvent);
    }

    @Override
    public void focusLost(FocusEvent focusEvent)
    {
        if (this.wwd == null) // include this test to ensure any derived implementation performs it
        {
            return;
        }

        if (focusEvent == null)
        {
            return;
        }

        this.viewInputBroker.focusLost(focusEvent);
    }

    protected boolean isPickListEmpty(PickedObjectList pickList)
    {
        return pickList == null || pickList.size() < 1;
    }

    protected void doHover(boolean reset)
    {
        //
        
        // SEG - removed doHover -- got Exception in thread "AWT-EventQueue-0" java.lang.NullPointerException
//        PickedObjectList pickedObjects = this.wwd.getObjectsAtCurrentPosition();
//        if (!(this.isPickListEmpty(this.hoverObjects) || this.isPickListEmpty(pickedObjects)))
//        {
//            PickedObject hover = this.hoverObjects.getTopPickedObject();
//            PickedObject last = pickedObjects.getTopPickedObject();
//
//            Object oh = hover == null ? null : hover.getObject() != null ? hover.getObject() :
//                    hover.getParentLayer() != null ? hover.getParentLayer() : null;
//            Object ol = last == null ? null : last.getObject() != null ? last.getObject() :
//                    last.getParentLayer() != null ? last.getParentLayer() : null;
//            if (oh != null && ol != null && oh.equals(ol))
//            {
//                return; // object picked is the hover object. don't do anything but wait for the timer to expire.
//            }
//        }
//
//        this.cancelHover();
//
//        if (!reset)
//        {
//            return;
//        }
//
//        if ((pickedObjects != null)
//                && (pickedObjects.getTopObject() != null)
//                && pickedObjects.getTopPickedObject().isTerrain())
//        {
//            return;
//        }
//
//        this.hoverObjects = pickedObjects;
//        this.hoverTimer.restart();
    }

    private void cancelHover()
    {
        if (this.isHovering)
        {
            this.callSelectListeners(new SelectEvent(this.wwd, SelectEvent.HOVER, this.mousePoint, null));
        }

        this.isHovering = false;
        this.hoverObjects = null;
        this.hoverTimer.stop();
    }

    protected boolean pickMatches(PickedObjectList pickedObjects)
    {
        if (this.isPickListEmpty(this.wwd.getObjectsAtCurrentPosition()) || this.isPickListEmpty(pickedObjects))
        {
            return false;
        }

        PickedObject lastTop = this.wwd.getObjectsAtCurrentPosition().getTopPickedObject();

        if (null != lastTop && lastTop.isTerrain())
        {
            return false;
        }

        PickedObject newTop = pickedObjects.getTopPickedObject();
        //noinspection SimplifiableIfStatement
        if (lastTop == null || newTop == null || lastTop.getObject() == null || newTop.getObject() == null)
        {
            return false;
        }

        return lastTop.getObject().equals(newTop.getObject());
    }

    protected void cancelDrag()
    {
        if (this.isDragging)
        {
            this.callSelectListeners(new DragSelectEvent(this.wwd, SelectEvent.DRAG_END, null,
                    this.objectsAtButtonPress, this.mousePoint));
        }

        this.isDragging = false;
    }

    @Override
    public void addSelectListener(SelectListener listener)
    {
        this.eventListeners.add(SelectListener.class, listener);
    }

    @Override
    public void removeSelectListener(SelectListener listener)
    {
        this.eventListeners.remove(SelectListener.class, listener);
    }

    protected void callSelectListeners(SelectEvent event)
    {
        for (SelectListener listener : this.eventListeners.getListeners(SelectListener.class))
        {
            listener.selected(event);
        }
    }

    @Override
    public void addMouseListener(MouseListener listener)
    {
        this.eventListeners.add(MouseListener.class, listener);
    }

    @Override
    public void removeMouseListener(MouseListener listener)
    {
        this.eventListeners.remove(MouseListener.class, listener);
    }

    @Override
    public void addMouseMotionListener(MouseMotionListener listener)
    {
        this.eventListeners.add(MouseMotionListener.class, listener);
    }

    @Override
    public void removeMouseMotionListener(MouseMotionListener listener)
    {
        this.eventListeners.remove(MouseMotionListener.class, listener);
    }

    @Override
    public void addMouseWheelListener(MouseWheelListener listener)
    {
        this.eventListeners.add(MouseWheelListener.class, listener);
    }

    @Override
    public void removeMouseWheelListener(MouseWheelListener listener)
    {
        this.eventListeners.remove(MouseWheelListener.class, listener);
    }

    protected void callMousePressedListeners(MouseEvent event)
    {
        for (MouseListener listener : this.eventListeners.getListeners(MouseListener.class))
        {
            listener.mousePressed(event);
        }
    }

    protected void callMouseReleasedListeners(MouseEvent event)
    {
        for (MouseListener listener : this.eventListeners.getListeners(MouseListener.class))
        {
            listener.mouseReleased(event);
        }
    }

    protected void callMouseClickedListeners(MouseEvent event)
    {
        for (MouseListener listener : this.eventListeners.getListeners(MouseListener.class))
        {
            listener.mouseClicked(event);
        }
    }

    protected void callMouseDraggedListeners(MouseEvent event)
    {
        for (MouseMotionListener listener : this.eventListeners.getListeners(MouseMotionListener.class))
        {
            listener.mouseDragged(event);
        }
    }

    protected void callMouseMovedListeners(MouseEvent event)
    {
        for (MouseMotionListener listener : this.eventListeners.getListeners(MouseMotionListener.class))
        {
            listener.mouseMoved(event);
        }
    }

    // -- after upgrade to WWJ v0.6
    @Override
    public void removeKeyListener(KeyListener listener)
    {
        this.eventListeners.remove(KeyListener.class, listener);
    }

    @Override
     public void addKeyListener(KeyListener listener)
    {
        this.eventListeners.add(KeyListener.class, listener);
    }
}