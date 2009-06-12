/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.swing.ComponentAdapter;

import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SliderAdapter extends ComponentAdapter implements ChangeListener {
    
    private final JSlider slider;
    private double scale;

    public SliderAdapter(JSlider slider) {
        this.slider = slider;
    }
    
    @Override
    public JComponent[] getComponents() {
        return new JComponent[]{slider};
    }

    @Override
    public void bindComponents() {
        updateScale();
        slider.addChangeListener(this);
    }

    @Override
    public void unbindComponents() {
        slider.removeChangeListener(this);
    }
    
    @Override
    public void adjustComponents() {
        Number value = (Number) getBinding().getPropertyValue();
        slider.setValue((int) (value.doubleValue() / scale));
    }
    
    public void stateChanged(ChangeEvent e) {
        getBinding().setPropertyValue(slider.getValue() * scale);
    }

    private void updateScale() {
        ValueDescriptor valueDescriptor = getBinding().getContext().getValueContainer().getDescriptor(getBinding().getPropertyName());
        ValueRange range = valueDescriptor.getValueRange();
        scale = (range.getMax() - range.getMin()) / (slider.getMaximum() - slider.getMinimum());
    }
}