/*
 * $Id$
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.bc.ceres.glayer.swing;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;


/**
 * An animated component which can be in one of four {@link WakefulComponent.VisualState}s.
 * The visual state is presented by different degrees of component opacity. Sate transitions are
 * done smoothly and can be controlled by various time settings.
 *
 * <pre>
 *
 *   INACTIVE   |  ACTIVATING    |      ACTIVE        |   DEACTIVATING   | INACTIVE
 *              |mouseEntered           |mouseExited
 *  ------------|----------------|------|-------------|------------------|----------
 *              |activationTime  |      |waitingTime  |deactivationTime  |
 *                               _____________________
 *                          ___/                      \_____
 *                     ___/                                 \_____
 *                ___/                                            \_____
 *  ____________/                                                       \__________
 *
 * </pre>
 *
 * Clients can observe state changes by listening to changes of the
 * {@link WakefulComponent#getVisualState() visualState} property.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class WakefulComponent extends JComponent {
    private static final int PPS = 20;

    private float currentAlpha;
    private float minAlpha = 0.2f;
    private float maxAlpha = 0.8f;
    private int activationTime = 400;
    private int deactivationTime = 200;
    private int waitingTime = 700;
    private VisualState visualState;
    private long lastHitTimestamp;

    private final Timer timer;
    private final ChildResizeHandler childResizeHandler;
    private BufferedImage image;
    private Graphics2D imageGraphics;
    private HitHandler hitHandler;

    public enum VisualState {
        INACTIVE,
        ACTIVATING,
        ACTIVE,
        DEACTIVATING,
    }

    public WakefulComponent() {
        this(null);
    }

    public WakefulComponent(JComponent other) {
        setOpaque(false);
        childResizeHandler = new ChildResizeHandler();
        visualState = VisualState.INACTIVE;
        timer = new Timer(0, new TimerListener());
        timer.setInitialDelay(0);
        timer.setRepeats(true);
        hitHandler = new HitHandler();
        currentAlpha = minAlpha;
        if (other != null) {
            add(other);
            final Dimension preferredSize = other.getPreferredSize();
            setPreferredSize(preferredSize);
            installHitHandler(other);
        } else {
            installHitHandler(this);
        }
    }

    public int getActivationTime() {
        return activationTime;
    }

    public void setActivationTime(int activationTime) {
        this.activationTime = activationTime;
    }

    public int getDeactivationTime() {
        return deactivationTime;
    }

    public void setDeactivationTime(int deactivationTime) {
        this.deactivationTime = deactivationTime;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public float getCurrentAlpha() {
        return currentAlpha;
    }

    protected void setCurrentAlpha(float currentAlpha) {
        this.currentAlpha = currentAlpha;
        repaint();
    }

    public float getMinAlpha() {
        return minAlpha;
    }

    public void setMinAlpha(float minAlpha) {
        this.minAlpha = minAlpha;
    }

    public float getMaxAlpha() {
        return maxAlpha;
    }

    public void setMaxAlpha(float maxAlpha) {
        this.maxAlpha = maxAlpha;
    }

    public VisualState getVisualState() {
        return visualState;
    }

    protected void setVisualState(VisualState visualState) {
        final VisualState oldState = getVisualState();
        if (oldState != visualState) {
            this.visualState = visualState;
            firePropertyChange("visualState", oldState, visualState);
        }
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (imageGraphics != null) {
            imageGraphics.dispose();
        }
        image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        imageGraphics = image.createGraphics();
        imageGraphics.setBackground(new Color(0, 0, 0, 0));
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
        comp.addComponentListener(childResizeHandler);
        adaptSize(comp);
        installHitHandler(comp);
    }

    @Override
    public void remove(int index) {
        final Component comp = getComponent(index);
        comp.removeComponentListener(childResizeHandler);
        deinstallHitHandler(comp);
        super.remove(index);
    }

    @Override
    public final LayoutManager getLayout() {
        return super.getLayout();
    }

    @Override
    public final void setLayout(LayoutManager mgr) {
        if (mgr != null) {
            throw new IllegalArgumentException("mgr");
        }
        super.setLayout(mgr);
    }

    @Override
    public final void doLayout() {
        super.doLayout();
    }

    @Override
    public void paint(Graphics g) {

        imageGraphics.clearRect(0, 0, image.getWidth(), image.getHeight());

        final Object oldAntialias = imageGraphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.paint(imageGraphics);
        if (oldAntialias != null) {
            imageGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialias);
        }

        Graphics2D g2d = (Graphics2D) g;
        Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1.0f, currentAlpha)));
        g2d.drawRenderedImage(image, null);
        if (oldComposite != null) {
            g2d.setComposite(oldComposite);
        }
    }

    private void installHitHandler(Component comp) {
        comp.addMouseListener(hitHandler);
        comp.addMouseMotionListener(hitHandler);
    }

    private void deinstallHitHandler(Component comp) {
        comp.removeMouseListener(hitHandler);
        comp.removeMouseMotionListener(hitHandler);
    }

    private void adaptSize(Component comp) {
        final Rectangle r = new Rectangle();
        r.add(getBounds());
        r.add(comp.getBounds());
        setSize(r.width, r.height);
    }

    private class HitHandler extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            // System.out.println("WakefulComponent.e = " + e);
            if (getVisualState() == VisualState.INACTIVE) {
                setVisualState(VisualState.ACTIVATING);
                timer.setDelay(activationTime / PPS);
                timer.restart();
            }
            lastHitTimestamp = -1;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            // System.out.println("WakefulComponent.e = " + e);
            lastHitTimestamp = System.currentTimeMillis();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            // System.out.println("WakefulComponent.e = " + e);
            lastHitTimestamp = -1;
        }
    }

    private class TimerListener implements ActionListener {
        private int counter;

        public void actionPerformed(ActionEvent e) {
            if (getVisualState() == VisualState.ACTIVE) {
                final long timestamp = System.currentTimeMillis();
                if (lastHitTimestamp > 0 && timestamp - lastHitTimestamp > waitingTime) {
                    setVisualState(VisualState.DEACTIVATING);
                    timer.setDelay(deactivationTime / PPS);
                    timer.restart();
                }
            } else if (getVisualState() == VisualState.INACTIVE) {
                // should not come here
            } else {
                final float weight = (float) counter / (float) PPS;
                if (getVisualState() == VisualState.ACTIVATING) {
                    if (counter < PPS) {
                        setCurrentAlpha(minAlpha + (maxAlpha - minAlpha) * weight);
                        counter++;
                    } else {
                        setCurrentAlpha(maxAlpha);
                        setVisualState(VisualState.ACTIVE);
                        counter = 0;
                    }
                } else if (getVisualState() == VisualState.DEACTIVATING) {
                    if (counter < PPS) {
                        setCurrentAlpha(minAlpha + (maxAlpha - minAlpha) * (1 - weight));
                        counter++;
                    } else {
                        setCurrentAlpha(minAlpha);
                        setVisualState(VisualState.INACTIVE);
                        timer.stop();
                        counter = 0;
                    }
                }
            }
        }
    }

    private class ChildResizeHandler extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            adaptSize(e.getComponent());
        }
    }
}