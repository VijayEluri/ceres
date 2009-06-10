package com.bc.ceres.binding;

import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.validators.ArrayValidator;
import com.bc.ceres.binding.validators.IntervalValidator;
import com.bc.ceres.binding.validators.MultiValidator;
import com.bc.ceres.binding.validators.NotEmptyValidator;
import com.bc.ceres.binding.validators.NotNullValidator;
import com.bc.ceres.binding.validators.PatternValidator;
import com.bc.ceres.binding.validators.TypeValidator;
import com.bc.ceres.binding.validators.ValueSetValidator;
import com.bc.ceres.core.Assert;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Describes a value by its name, type and a set of optional (mutable) properties.
 * Examples for such properties are a {@link ValueSet}, a {@link Pattern Pattern} or
 * an {@link ValueRange}.
 * Property changes may be observed by adding a property change listeners
 * to instances of this class.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class ValueDescriptor {

    private final String name;
    private final Class<?> type;
    private volatile Validator effectiveValidator;

    private Map<String, Object> properties;
    private PropertyChangeSupport propertyChangeSupport;

    public ValueDescriptor(String name, Class<?> type) {
        this(name, type, new HashMap<String, Object>(8));
    }

    public ValueDescriptor(ValueDescriptor valueDescriptor) {
        this(valueDescriptor.getName(), valueDescriptor.getType(), valueDescriptor.properties);
    }

    public ValueDescriptor(String name, Class<?> type, Map<String, Object> properties) {
        Assert.notNull(name, "name");
        Assert.notNull(type, "type");
        Assert.notNull(properties, "properties");
        this.name = name;
        this.type = type;
        this.properties = new HashMap<String, Object>(properties);

        addPropertyChangeListener(new EffectiveValidatorUpdater());
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public String getDisplayName() {
        return (String) getProperty("displayName");
    }

    public void setDisplayName(String displayName) {
        setProperty("displayName", displayName);
    }

    public String getAlias() {
        return (String) getProperty("alias");
    }

    public void setAlias(String alias) {
        setProperty("alias", alias);
    }

    public String getUnit() {
        return (String) getProperty("unit");
    }

    public void setUnit(String unit) {
        setProperty("unit", unit);
    }

    public String getDescription() {
        return (String) getProperty("description");
    }

    public void setDescription(String description) {
        setProperty("description", description);
    }

    public boolean isNotNull() {
        return getBooleanProperty("notNull");
    }

    public void setNotNull(boolean notNull) {
        setProperty("notNull", notNull);
    }

    public boolean isNotEmpty() {
        return getBooleanProperty("notEmpty");
    }

    public void setNotEmpty(boolean notEmpty) {
        setProperty("notEmpty", notEmpty);
    }

    public boolean isTransient() {
        return getBooleanProperty("transient");
    }

    public void setTransient(boolean b) {
        setProperty("transient", b);
    }


    public String getFormat() {
        return (String) getProperty("format");
    }

    public void setFormat(String format) {
        setProperty("format", format);
    }

    public ValueRange getValueRange() {
        return (ValueRange) getProperty("valueRange");
    }

    public void setValueRange(ValueRange valueRange) {
        setProperty("valueRange", valueRange);
    }

    public Pattern getPattern() {
        return (Pattern) getProperty("pattern");
    }

    public Object getDefaultValue() {
        return getProperty("defaultValue");
    }

    public void setDefaultValue(Object defaultValue) {
        setProperty("defaultValue", defaultValue);
    }

    public void setPattern(Pattern pattern) {
        setProperty("pattern", pattern);
    }

    public ValueSet getValueSet() {
        return (ValueSet) getProperty("valueSet");
    }

    public void setValueSet(ValueSet valueSet) {
        setProperty("valueSet", valueSet);
    }

    public Converter<?> getConverter() {
        return getConverter(false);
    }

    public Converter<?> getConverter(boolean notNull) {
        final Converter<?> converter = (Converter<?>) getProperty("converter");
        if (converter == null && notNull) {
            throw new IllegalStateException("no converter defined for value '" + getName() + "'");
        }
        return converter;
    }

    public void setDefaultConverter() {
        Class<?> type = getType();
        if (getItemAlias() != null && type.isArray()) {
            type = type.getComponentType();
        }
        setConverter(ConverterRegistry.getInstance().getConverter(type));
    }

    public void setConverter(Converter<?> converter) {
        setProperty("converter", converter);
    }

    public DomConverter getDomConverter() {
        return (DomConverter) getProperty("domConverter");
    }

    public void setDomConverter(DomConverter converter) {
        setProperty("domConverter", converter);
    }

    public Validator getValidator() {
        return (Validator) getProperty("validator");
    }

    public void setValidator(Validator validator) {
        setProperty("validator", validator);
    }

    Validator getEffectiveValidator() {
        if (effectiveValidator == null) {
            synchronized (this) {
                if (effectiveValidator == null) {
                    effectiveValidator = createEffectiveValidator();
                }
            }
        }
        return effectiveValidator;
    }


    //////////////////////////////////////////////////////////////////////////////
    // Array/List item properties

    public String getItemAlias() {
        return (String) getProperty("itemAlias");
    }

    public void setItemAlias(String alias) {
        setProperty("itemAlias", alias);
    }

    public boolean getItemsInlined() {
        return getBooleanProperty("itemsInlined");
    }

    public void setItemsInlined(boolean inlined) {
        setProperty("itemsInlined", inlined);
    }

    //////////////////////////////////////////////////////////////////////////////
    // Generic properties

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public void setProperty(String name, Object value) {
        Object oldValue = getProperty(name);
        if (value != null) {
            properties.put(name, value);
        } else {
            properties.remove(name);
        }
        if (!equals(oldValue, value)) {
            firePropertyChange(name, oldValue, value);
        }
    }

    public final void addPropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport == null) {
            propertyChangeSupport = new PropertyChangeSupport(this);
        }
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public final void removePropertyChangeListener(PropertyChangeListener listener) {
        if (propertyChangeSupport != null) {
            propertyChangeSupport.removePropertyChangeListener(listener);
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        if (propertyChangeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return this.propertyChangeSupport.getPropertyChangeListeners();
    }


    /////////////////////////////////////////////////////////////////////////
    // Package Local

    static ValueDescriptor createValueDescriptor(String name, Class<?> type) {
        final ValueDescriptor valueDescriptor = new ValueDescriptor(name, type);
        valueDescriptor.initialize();
        return valueDescriptor;
    }

    static ValueDescriptor createValueDescriptor(Field field, ClassFieldDescriptorFactory factory) {
        final ValueDescriptor valueDescriptor = factory.createValueDescriptor(field);
        if (valueDescriptor == null) {
            return null;
        }
        valueDescriptor.initialize();
        return valueDescriptor;
    }

    /////////////////////////////////////////////////////////////////////////
    // Private

    private void initialize() {
        if (getConverter() == null) {
            setDefaultConverter();
        }
        if (getDefaultValue() == null && getType().isPrimitive()) {
            setDefaultValue(ValueModel.PRIMITIVE_ZERO_VALUES.get(getType()));
        }
    }

    private void firePropertyChange(String propertyName, Object newValue, Object oldValue) {
        if (propertyChangeSupport == null) {
            return;
        }
        PropertyChangeListener[] propertyChangeListeners = getPropertyChangeListeners();
        PropertyChangeEvent evt = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
        for (PropertyChangeListener propertyChangeListener : propertyChangeListeners) {
            propertyChangeListener.propertyChange(evt);
        }
    }

    private static boolean equals(Object a, Object b) {
        return a == b || !(a == null || b == null) && a.equals(b);
    }

    private boolean getBooleanProperty(String name) {
        Object v = getProperty(name);
        return v != null && (Boolean) v;
    }

    private Validator createEffectiveValidator() {
        List<Validator> validators = new ArrayList<Validator>(3);

        validators.add(new TypeValidator());

        if (isNotNull()) {
            validators.add(new NotNullValidator());
        }
        if (isNotEmpty()) {
            validators.add(new NotEmptyValidator());
        }
        if (getPattern() != null) {
            validators.add(new PatternValidator(getPattern()));
        }
        if (getValueSet() != null) {
            Validator valueSetValidator = new ValueSetValidator(this);
            if (getType().isArray()) {
                valueSetValidator = new ArrayValidator(valueSetValidator);
            }
            validators.add(valueSetValidator);
        }
        if (getValueRange() != null) {
            validators.add(new IntervalValidator(getValueRange()));
        }
        if (getValidator() != null) {
            validators.add(getValidator());
        }
        Validator validator;
        if (validators.isEmpty()) {
            validator = null;
        } else if (validators.size() == 1) {
            validator = validators.get(0);
        } else {
            validator = new MultiValidator(validators);
        }
        return validator;
    }

    private class EffectiveValidatorUpdater implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            // Force recreation of validator
            effectiveValidator = null;
        }
    }
}
