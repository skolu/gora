package org.db.gora.accessors;

import org.db.gora.ValueAccess;

import java.lang.reflect.Field;

public class StringFieldValueAccessor implements ValueAccess {

    final Field mField;
    final StringValueClass mJavaClass;
    final Object[] mValues;

    public StringFieldValueAccessor(Field field) {
        this.mField = field;
        mJavaClass = field.getType().isEnum() ? StringValueClass.Enum : StringValueClass.String;
        if (mJavaClass == StringValueClass.Enum) {
            mValues = field.getType().getEnumConstants();
        } else {
            mValues = null;
        }
    }

    @Override
    public String getValue(Object storage) throws Exception {
        Object value = mField.get(storage);
        if (value == null) {
            return null;
        }
        if (mJavaClass == StringValueClass.Enum) {
            return ((Enum) value).name();
        }
        return (String) value;
    }

    @Override
    public void setValue(Object value, Object storage) throws Exception {
        if (value != null) {
            if (mJavaClass == StringValueClass.Enum) {
                String name = (String) value;
                value = null;
                for (Object o: mValues) {
                    Enum e = (Enum) o;
                    if (name.equals(e.name())) {
                        value = e;
                        break;
                    }
                }
            }
        }

        mField.set(storage, value);
    }

    @Override
    public Class<?> getStorageClass() {
        return mField.getDeclaringClass();
    }
}
