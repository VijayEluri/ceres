package com.bc.ceres.binding.validators;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueModel;

import java.text.MessageFormat;

public class TypeValidator implements Validator {

    public void validateValue(ValueModel valueModel, Object value) throws ValidationException {
        final Class<?> type = valueModel.getDescriptor().getType();
        if (!isAssignableFrom(type, value)) {
            String name = valueModel.getDescriptor().getDisplayName();
            if (name == null) {
                name = valueModel.getDescriptor().getName();
            }
            if (value != null) {
                throw new ValidationException(MessageFormat.format("Value for ''{0}'' must be of type ''{1}'', but was ''{2}'' (''{3}'').",
                                                                   name, type.getSimpleName(), value.getClass().getSimpleName(), value));
            } else {
                throw new ValidationException(MessageFormat.format("Value for ''{0}'' must be of type ''{1}'', " +
                        "but was null.",
                                                                   name, type.getSimpleName()));
            }
        }
    }

    boolean isAssignableFrom(Class<?> type, Object value) {
        if (value == null) {
            return !type.isPrimitive();
        }
        final Class<?> valueType = value.getClass();
        return type.isAssignableFrom(valueType)
                || type.isPrimitive()
                && (type.equals(Boolean.TYPE) && valueType.equals(Boolean.class)
                || type.equals(Character.TYPE) && valueType.equals(Character.class)
                || type.equals(Byte.TYPE) && valueType.equals(Byte.class)
                || type.equals(Short.TYPE) && valueType.equals(Short.class)
                || type.equals(Integer.TYPE) && valueType.equals(Integer.class)
                || type.equals(Long.TYPE) && valueType.equals(Long.class)
                || type.equals(Float.TYPE) && valueType.equals(Float.class)
                || type.equals(Double.TYPE) && valueType.equals(Double.class));
    }
}
