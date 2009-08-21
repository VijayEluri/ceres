package com.bc.ceres.binding.swing;


import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;
import com.bc.ceres.binding.swing.internal.AbstractButtonAdapter;
import com.bc.ceres.binding.swing.internal.BindingImpl;
import com.bc.ceres.binding.swing.internal.ButtonGroupAdapter;
import com.bc.ceres.binding.swing.internal.ComboBoxAdapter;
import com.bc.ceres.binding.swing.internal.FormattedTextFieldAdapter;
import com.bc.ceres.binding.swing.internal.ListSelectionAdapter;
import com.bc.ceres.binding.swing.internal.SpinnerAdapter;
import com.bc.ceres.binding.swing.internal.TextComponentAdapter;
import com.bc.ceres.core.Assert;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A context used to bind Swing components to properties in a value container.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since Ceres 0.6
 */
public class BindingContext {

    private final ValueContainer valueContainer;
    private BindingContext.ErrorHandler errorHandler;
    private Map<String, BindingImpl> bindingMap;
    private Map<String, EnablePCL> enablePCLMap;
    private ArrayList<ChangeListener> stateChangeListeners;
    private final PropertyChangeListener valueContainerListener;

    /**
     * Constructor.
     */
    public BindingContext() {
        this(new ValueContainer());
    }

    /**
     * Constructor.
     *
     * @param valueContainer The value container.
     */
    public BindingContext(ValueContainer valueContainer) {
        this(valueContainer, new BindingContext.DefaultErrorHandler());
    }

    /**
     * Constructor.
     *
     * @param valueContainer The value container.
     * @param errorHandler   The error handler.
     *
     * @deprecated Since 0.10, for error handling use {@link #addStateChangeListener(javax.swing.event.ChangeListener)}
     *             and {@link #getProblems()} instead
     */
    @Deprecated
    public BindingContext(ValueContainer valueContainer, BindingContext.ErrorHandler errorHandler) {
        this.valueContainer = valueContainer;
        this.errorHandler = errorHandler;
        this.bindingMap = new HashMap<String, BindingImpl>(17);
        this.enablePCLMap = new HashMap<String, EnablePCL>(11);
        valueContainerListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                fireStateChanged();
            }
        };
        this.valueContainer.addPropertyChangeListener(valueContainerListener);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        this.valueContainer.removePropertyChangeListener(valueContainerListener);
    }

    public ValueContainer getValueContainer() {
        return valueContainer;
    }

    /**
     * @return the error handler
     */
    @Deprecated
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Sets the error handler.
     *
     * @param errorHandler the error handler
     *
     * @deprecated Since 0.10, use {@link #addStateChangeListener(javax.swing.event.ChangeListener)}
     *             and {@link #getProblems()} instead
     */
    @Deprecated
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * @return {@code true} if this context has problems.
     *
     * @since Ceres 0.10
     */
    public boolean hasProblems() {
        for (Map.Entry<String, BindingImpl> entry : bindingMap.entrySet()) {
            if (entry.getValue().getProblem() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return The array of problems this context might have.
     *
     * @since Ceres 0.10
     */
    public BindingProblem[] getProblems() {
        ArrayList<BindingProblem> list = new ArrayList<BindingProblem>();
        for (Map.Entry<String, BindingImpl> entry : bindingMap.entrySet()) {
            final BindingProblem problem = entry.getValue().getProblem();
            if (problem != null) {
                list.add(problem);
            }
        }
        return list.toArray(new BindingProblem[list.size()]);
    }

    /**
     * Adds a state change listener to this context.
     *
     * @param listener The listener.
     *
     * @since Ceres 0.10
     */
    public void addStateChangeListener(ChangeListener listener) {
        Assert.notNull(listener, "listener");
        if (stateChangeListeners == null) {
            stateChangeListeners = new ArrayList<ChangeListener>();
        }
        stateChangeListeners.add(listener);
    }

    /**
     * Removes a state change listener from this context.
     *
     * @param listener The listener.
     *
     * @since Ceres 0.10
     */
    public void removeStateChangeListener(ChangeListener listener) {
        Assert.notNull(listener, "listener");
        if (stateChangeListeners != null) {
            stateChangeListeners.remove(listener);
        }
    }

    /**
     * @return The array of state change listeners.
     *
     * @since Ceres 0.10
     */
    public ChangeListener[] getStateChangeListeners() {
        return stateChangeListeners != null ? stateChangeListeners.toArray(new ChangeListener[stateChangeListeners.size()]) : new ChangeListener[0];
    }

    /**
     * Fires a state change event by notifying all listeners.
     */
    public void fireStateChanged() {
        ChangeEvent changeEvent = new ChangeEvent(this);
        for (ChangeListener listener : getStateChangeListeners()) {
            listener.stateChanged(changeEvent);
        }
    }

    /**
     * Adjusts all associated Swing components so that they reflect the
     * values of the associated value container.
     */
    public void adjustComponents() {
        for (Map.Entry<String, BindingImpl> entry : bindingMap.entrySet()) {
            entry.getValue().adjustComponents();
        }
    }

    public Binding getBinding(String propertyName) {
        return bindingMap.get(propertyName);
    }

    public Binding bind(String propertyName, ComponentAdapter adapter) {
        BindingImpl binding = new BindingImpl(this, propertyName, adapter);
        addBinding(binding);
        adapter.setBinding(binding);
        adapter.bindComponents();
        binding.bindProperty();
        binding.adjustComponents();
        configureComponents(binding);
        return binding;
    }

    public void unbind(Binding binding) {
        removeBinding(binding.getPropertyName());
        if (binding instanceof BindingImpl) {
            ((BindingImpl) binding).unbindProperty();
        }
        binding.getComponentAdapter().unbindComponents();
    }

    public Binding bind(final String propertyName, final JTextComponent textComponent) {
        return bind(propertyName, new TextComponentAdapter(textComponent));
    }

    public Binding bind(final String propertyName, final JTextField textField) {
        return bind(propertyName, new TextComponentAdapter(textField));
    }

    public Binding bind(final String propertyName, final JFormattedTextField textField) {
        return bind(propertyName, new FormattedTextFieldAdapter(textField));
    }

    public Binding bind(final String propertyName, final JCheckBox checkBox) {
        return bind(propertyName, new AbstractButtonAdapter(checkBox));
    }

    public Binding bind(String propertyName, JRadioButton radioButton) {
        return bind(propertyName, new AbstractButtonAdapter(radioButton));
    }

    public Binding bind(final String propertyName, final JList list, final boolean selectionIsValue) {
        if (selectionIsValue) {
            return bind(propertyName, new ListSelectionAdapter(list));
        } else {
            throw new RuntimeException("not implemented");
        }
    }

    public Binding bind(final String propertyName, final JSpinner spinner) {
        return bind(propertyName, new SpinnerAdapter(spinner));
    }

    public Binding bind(final String propertyName, final JComboBox comboBox) {
        return bind(propertyName, new ComboBoxAdapter(comboBox));
    }

    public Binding bind(final String propertyName, final ButtonGroup buttonGroup) {
        return bind(propertyName, buttonGroup,
                    ButtonGroupAdapter.createButtonToValueMap(buttonGroup, getValueContainer(), propertyName));
    }

    public Binding bind(final String propertyName, final ButtonGroup buttonGroup,
                        final Map<AbstractButton, Object> valueSet) {
        ComponentAdapter adapter = new ButtonGroupAdapter(buttonGroup, valueSet);
        return bind(propertyName, adapter);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        valueContainer.addPropertyChangeListener(l);
    }

    public void addPropertyChangeListener(String name, PropertyChangeListener l) {
        valueContainer.addPropertyChangeListener(name, l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        valueContainer.removePropertyChangeListener(l);
    }

    public void removePropertyChangeListener(String name, PropertyChangeListener l) {
        valueContainer.removePropertyChangeListener(name, l);
    }

    /**
     * Permits component validation and property changes of the value container
     * triggered by the given component.
     *
     * @param component The component.
     *
     * @see #preventPropertyChanges(javax.swing.JComponent)
     * @see #getValueContainer()
     * @since Ceres 0.10
     */
    @SuppressWarnings({"MethodMayBeStatic"})
    public void permitPropertyChanges(JComponent component) {
        component.setVerifyInputWhenFocusTarget(true);
    }

    /**
     * Prevents component validation and property changes of the value container
     * triggered by the given component.
     * <p/>
     * For example, if a text component loses keyboard focus because another component requests it,
     * its text value will be converted, validated and the value container's property will be changed.
     * For some focus targets, like a dialog's "Cancel" button, this is not desired.
     * <p/>
     * By default, component validation and property changes are permitted for most Swing components.
     *
     * @param component The component.
     *
     * @see #permitPropertyChanges(javax.swing.JComponent)
     * @see #getValueContainer()
     * @since Ceres 0.10
     */
    @SuppressWarnings({"MethodMayBeStatic"})
    public void preventPropertyChanges(JComponent component) {
        component.setVerifyInputWhenFocusTarget(false);
    }

    /**
     * Delegates the call to the error handler.
     *
     * @param error     The error.
     * @param component The Swing component in which the error occured.
     *
     * @see #getErrorHandler()
     * @see #setErrorHandler(ErrorHandler)
     * @deprecated Since 0.10, for error handling use {@link BindingContext#addStateChangeListener(javax.swing.event.ChangeListener)}
     *             and {@link BindingContext#getProblems()} instead
     */
    @Deprecated
    public void handleError(Exception error, JComponent component) {
        errorHandler.handleError(error, component);
    }

    private void configureComponents(Binding binding) {
        final String propertyName = binding.getPropertyName();
        final String toolTipTextStr = getToolTipText(propertyName);
        final JComponent[] components = binding.getComponents();
        JComponent primaryComponent = components[0];
        configureComponent(primaryComponent, propertyName, toolTipTextStr);
        for (int i = 1; i < components.length; i++) {
            JComponent component = components[i];
            configureComponent(component, propertyName + "." + i, toolTipTextStr);
        }
    }

    private String getToolTipText(String propertyName) {
        final ValueModel valueModel = valueContainer.getModel(propertyName);
        StringBuilder toolTipText = new StringBuilder(32);                                                  
        final ValueDescriptor valueDescriptor = valueModel.getDescriptor();
        if (valueDescriptor.getDescription() != null) {
            toolTipText.append(valueDescriptor.getDescription());
        }
        if (valueDescriptor.getUnit()!= null && valueDescriptor.getUnit().length() > 0) {
            toolTipText.append(" (");
            toolTipText.append(valueDescriptor.getUnit());
            toolTipText.append(")");
        }
        return toolTipText.toString();
    }

    private static void configureComponent(JComponent component, String name, String toolTipText) {
        if (component.getName() == null) {
            component.setName(name);
        }
        if (component.getToolTipText() == null && toolTipText.length() > 0) {
            component.setToolTipText(toolTipText);
        }
    }

    /**
     * Sets the <i>enabled</i> state of the components associated with {@code targetProperty}.
     * If the current value of {@code sourceProperty} equals {@code sourcePropertyValue} then
     * the enabled state will be set to the value of {@code enabled}, otherwise it is the negated value
     * of {@code enabled}. Neither the source property nor the target property need to have an active binding.
     *
     * @param targetPropertyName  The name of the target property.
     * @param enabled             The enabled state.
     * @param sourcePropertyName  The name of the source property.
     * @param sourcePropertyValue The value of the source property.
     */
    public void bindEnabledState(final String targetPropertyName,
                                 final boolean enabled,
                                 final String sourcePropertyName,
                                 final Object sourcePropertyValue) {
        final EnablePCL enablePCL = new EnablePCL(targetPropertyName, enabled, sourcePropertyName, sourcePropertyValue);
        final Binding binding = getBinding(targetPropertyName);
        if (binding != null) {
            enablePCL.apply();
            valueContainer.addPropertyChangeListener(sourcePropertyName, enablePCL);
        } else {
            enablePCLMap.put(targetPropertyName, enablePCL);
        }
    }

    private void setComponentsEnabled(final JComponent[] components,
                                      final boolean enabled,
                                      final String sourcePropertyName,
                                      final Object sourcePropertyValue) {
        Object propertyValue = valueContainer.getValue(sourcePropertyName);
        boolean conditionIsTrue = propertyValue == sourcePropertyValue
                || (propertyValue != null && propertyValue.equals(sourcePropertyValue));
        for (JComponent component : components) {
            component.setEnabled(conditionIsTrue ? enabled : !enabled);
        }
    }

    private void addBinding(BindingImpl binding) {
        bindingMap.put(binding.getPropertyName(), binding);
        if (enablePCLMap.containsKey(binding.getPropertyName())) {
            EnablePCL enablePCL = enablePCLMap.remove(binding.getPropertyName());
            enablePCL.apply();
            valueContainer.addPropertyChangeListener(enablePCL.sourcePropertyName, enablePCL);
        }
    }

    private void removeBinding(String propertyName) {
        bindingMap.remove(propertyName);
    }

    /**
     * An error handler.
     *
     * @deprecated Since Ceres 0.10, for error handling use {@link BindingContext#addStateChangeListener(javax.swing.event.ChangeListener)}
     *             and {@link BindingContext#getProblems()} instead
     */
    @Deprecated
    public interface ErrorHandler {
        /**
         * Handles an error.
         *
         * @param error     A {@link com.bc.ceres.binding.BindingException}
         * @param component The component.
         */
        void handleError(Exception error, JComponent component);
    }

    @Deprecated
    private static class DefaultErrorHandler implements BindingContext.ErrorHandler {

        public void handleError(Exception error, JComponent component) {
            Window window = component != null ? SwingUtilities.windowForComponent(component) : null;
            if (error instanceof ValidationException) {
                JOptionPane.showMessageDialog(window, error.getMessage(), "Invalid Input",
                                              JOptionPane.ERROR_MESSAGE);
            } else {
                String message = MessageFormat.format("An internal error occured:\nType: {0}\nMessage: {1}\n",
                                                      error.getClass(), error.getMessage());
                JOptionPane.showMessageDialog(window, message, "Internal Error", JOptionPane.ERROR_MESSAGE);
                error.printStackTrace();
            }
            if (component != null) {
                component.requestFocus();
            }
        }
    }

    private class EnablePCL implements PropertyChangeListener {

        private final String targetPropertyName;
        private final boolean enabled;
        private final String sourcePropertyName;
        private final Object sourcePropertyValue;

        private EnablePCL(String targetPropertyName, boolean enabled, String sourcePropertyName,
                          Object sourcePropertyValue) {
            this.targetPropertyName = targetPropertyName;
            this.enabled = enabled;
            this.sourcePropertyName = sourcePropertyName;
            this.sourcePropertyValue = sourcePropertyValue;
        }

        public void propertyChange(PropertyChangeEvent evt) {
            apply();
        }

        public void apply() {
            setComponentsEnabled(getBinding(targetPropertyName).getComponents(),
                                 enabled,
                                 sourcePropertyName,
                                 sourcePropertyValue);
        }
    }
}
