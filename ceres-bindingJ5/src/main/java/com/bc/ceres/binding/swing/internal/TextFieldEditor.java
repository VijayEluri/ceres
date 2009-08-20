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
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ComponentAdapter;
import com.bc.ceres.binding.swing.ValueEditor;

import javax.swing.JComponent;
import javax.swing.JTextField;

/**
 * A value editor that uses a text field.
 * 
 * This editor does not qualify itself 
 * for any edit because it is the default editor.
 * Otherwise it would take precedence over other editors.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since Ceres 0.9
 */
public class TextFieldEditor extends ValueEditor {

    @Override
    public JComponent createEditorComponent(ValueDescriptor valueDescriptor, BindingContext bindingContext) {
        JTextField textField = new JTextField();
        ComponentAdapter adapter = new TextFieldAdapter(textField);
        bindingContext.bind(valueDescriptor.getName(), adapter);
        return textField;
    }
}
