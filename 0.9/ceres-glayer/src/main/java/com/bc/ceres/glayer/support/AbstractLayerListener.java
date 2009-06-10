package com.bc.ceres.glayer.support;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerListener;
import com.bc.ceres.glayer.Style;
import com.bc.ceres.binding.ValueContainer;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;

/**
 * An (empty) {@code LayerListener} implementation.
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public abstract class AbstractLayerListener implements LayerListener {

    /**
     * Called if a property of the given layer has changed. The source of the property change event
     * may be either the layer itself or its configuration (see {@link Layer#getConfiguration()}).
     *
     * @param layer The layer which triggered the change.
     * @param event The layer property change event.
     */
    public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
    }

    /**
     * Called if the data of the given layer has changed.
     *
     * @param layer       The layer which triggered the change.
     * @param modelRegion The region in model coordinates which are affected by the change. May be null, if not available.
     */
    public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
    }

    /**
     * Called if a new layer has been added to a collection layer.
     *
     * @param parentLayer The parent layer which triggered the change.
     * @param childLayers The child layers added.
     */
    public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
    }

    /**
     * Called if an existing layer has been removed from a collection layer.
     *
     * @param parentLayer The parent layer which triggered the change.
     * @param childLayers The child layers removed.
     */
    public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
    }

    /**
     * @param propertyChangeEvent A property change event fired by a {@link Layer}.
     * @return {@code true} if the change event is visible, e.g. causes (re)rendering of some kind of view
     */
    protected boolean isVisibleChangeEvent(PropertyChangeEvent propertyChangeEvent) {
        return propertyChangeEvent.getPropertyName().equals("visible")
                || propertyChangeEvent.getPropertyName().equals("transparency")
                || propertyChangeEvent.getPropertyName().equals("composite")
                || propertyChangeEvent.getSource() instanceof ValueContainer;
    }
}
