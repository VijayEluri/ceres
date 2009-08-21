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

import com.bc.ceres.binding.BindingException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.swing.BindingProblem;
import com.bc.ceres.binding.swing.ComponentAdapter;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * A binding for a {@link javax.swing.text.JTextComponent} component.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.9
 */
public class TextComponentAdapter extends ComponentAdapter implements ActionListener, FocusListener {

    private final JTextComponent textComponent;

    public TextComponentAdapter(JTextComponent textComponent) {
        super();
        this.textComponent = textComponent;
    }

    @Override
    public JComponent[] getComponents() {
        return new JComponent[]{textComponent};
    }

    @Override
    public void bindComponents() {
        if (textComponent instanceof JTextField) {
            ((JTextField) textComponent).addActionListener(this);
        }
        textComponent.addFocusListener(this);
        textComponent.setInputVerifier(createInputVerifier());
    }

    @Override
    public void unbindComponents() {
        if (textComponent instanceof JTextField) {
            ((JTextField) textComponent).removeActionListener(this);
        }
        textComponent.setInputVerifier(null);
    }

    @Override
    public void adjustComponents() {
        final ValueContainer valueContainer = getBinding().getContext().getValueContainer();
        final ValueModel model = valueContainer.getModel(getBinding().getPropertyName());
        if (model != null) {
            textComponent.setText(model.getValueAsText());
        } else {
            textComponent.setText("");
        }
    }

    void adjustValue() {
        try {
            final ValueContainer valueContainer = getBinding().getContext().getValueContainer();
            final ValueModel model = valueContainer.getModel(getBinding().getPropertyName());
            model.setValueFromText(textComponent.getText());
            getBinding().clearProblem();
        } catch (BindingException e) {
            getBinding().reportProblem(e);
        }
    }

    public InputVerifier createInputVerifier() {
        return new TextVerifier();
    }

    public void actionPerformed(ActionEvent e) {
        adjustValue();
    }

    public void focusGained(FocusEvent e) {
        if (getBinding().getProblem() != null) {
            textComponent.selectAll();
        }
    }

    public void focusLost(FocusEvent event) {
    }

    class TextVerifier extends InputVerifier {
        /*
         * Only called by base class InputVerifier.shouldYieldFocus()?
         */
        @Override
        public boolean verify(JComponent input) {
            return getBinding().getProblem() == null;
        }

        /*
         * Called by JComponent.focusController.
         */
        @Override
        public boolean shouldYieldFocus(JComponent input) {
            adjustValue();
            return getBinding().getProblem() == null;
        }
    }
}
