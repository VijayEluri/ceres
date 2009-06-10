package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.ComponentAdapter;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * A binding for a set of {@link javax.swing.AbstractButton} components sharing a multiple-exclusion scope.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class ButtonGroupAdapter extends ComponentAdapter implements ActionListener {
    private final ButtonGroup buttonGroup;
    private AbstractButton[] buttons;
    private final Map<AbstractButton, Object> buttonToValueMap;
    private final Map<Object, AbstractButton> valueToButtonMap;

    public ButtonGroupAdapter(ButtonGroup buttonGroup, Map<AbstractButton, Object> buttonToValueMap) {
        this.buttonGroup = buttonGroup;
        this.buttonToValueMap = buttonToValueMap;
        this.valueToButtonMap = new HashMap<Object, AbstractButton>(buttonToValueMap.size());
    }

    public ButtonGroup getButtonGroup() {
        return buttonGroup;
    }

    @Override
    public JComponent[] getComponents() {
        return buttons.clone();
    }

    @Override
    public void bindComponents() {
        Enumeration<AbstractButton> buttonEnum = buttonGroup.getElements();
        int count = buttonGroup.getButtonCount();
        buttons = new AbstractButton[count];
        for (int i = 0; i < count; i++) {
            AbstractButton button = buttonEnum.nextElement();
            button.addActionListener(this);
            buttons[i] = button;
            valueToButtonMap.put(buttonToValueMap.get(button), button);
        }
    }

    @Override
    public void unbindComponents() {
        Enumeration<AbstractButton> buttonEnum = buttonGroup.getElements();
        int count = buttonGroup.getButtonCount();
        for (int i = 0; i < count; i++) {
            AbstractButton button = buttonEnum.nextElement();
            button.removeActionListener(this);
            valueToButtonMap.remove(buttonToValueMap.get(button));
        }
        valueToButtonMap.clear();
    }

    @Override
    public void adjustComponents() {
        Object value = getBinding().getPropertyValue();
        if (value != null) {
            AbstractButton button = valueToButtonMap.get(value);
            if (button != null) {
                button.setSelected(true);
            }
        }
    }

    public void actionPerformed(ActionEvent e) {
        AbstractButton button = (AbstractButton) e.getSource();
        getBinding().setPropertyValue(buttonToValueMap.get(button));
    }

    public static Map<AbstractButton, Object> createButtonToValueMap(ButtonGroup buttonGroup, ValueContainer valueContainer, String propertyName) {
        ValueSet valueSet = valueContainer.getDescriptor(propertyName).getValueSet();
        if (valueSet == null) {
            throw new IllegalStateException("valueSet == null");
        }
        Object[] items = valueSet.getItems();
        if (buttonGroup.getButtonCount() != items.length) {
            throw new IllegalStateException("buttonGroup.getButtonCount() != items.length");
        }
        Enumeration<AbstractButton> buttonEnum = buttonGroup.getElements();
        HashMap<AbstractButton, Object> buttonToValueMap = new HashMap<AbstractButton, Object>(items.length);
        for (Object item : items) {
            buttonToValueMap.put(buttonEnum.nextElement(), item);
        }
        return buttonToValueMap;
    }

}
