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
import com.bc.ceres.binding.swing.Binding;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.binding.swing.ComponentAdapter;
import com.bc.ceres.binding.swing.ValueEditor;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * An editor for file names using a file chooser dialog.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class FileEditor extends ValueEditor {

    @Override
    public boolean isValidFor(ValueDescriptor valueDescriptor) {
        return File.class.isAssignableFrom(valueDescriptor.getType());
    }
    
    @Override
    public JComponent createEditorComponent(ValueDescriptor valueDescriptor, BindingContext bindingContext) {
        JTextField textField = new JTextField();
        ComponentAdapter adapter = new TextFieldAdapter(textField);
        final Binding binding = bindingContext.bind(valueDescriptor.getName(), adapter);
        final JPanel subPanel = new JPanel(new BorderLayout(2, 2));
        subPanel.add(textField, BorderLayout.CENTER);
        JButton etcButton = new JButton("...");
        etcButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int i = fileChooser.showDialog(subPanel, "Select");
                if (i == JFileChooser.APPROVE_OPTION && fileChooser.getSelectedFile() != null) {
                    binding.setPropertyValue(fileChooser.getSelectedFile());
                }
            }
        });
        subPanel.add(etcButton, BorderLayout.EAST);
        return subPanel;
    }
}
