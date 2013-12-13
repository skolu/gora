package org.db.gora.accessors;

import org.db.gora.DataAccessException;
import org.db.gora.ValueAccess;

import java.lang.reflect.Field;
import java.math.BigDecimal;

public class DoubleFieldValueAccessor implements ValueAccess {

    final Field mField;
    final DoubleValueClass mJavaClass;

    public DoubleFieldValueAccessor(Field field) {
        this.mField = field;
        mJavaClass = DoubleValueClass.resolveType(field.getType());
    }

    @Override
    public Double getValue(Object storage) throws IllegalAccessException, DataAccessException {
        Object value = mField.get(storage);
        if (value == null) {
            return 0.0;
        }
        switch (mJavaClass) {
            case Double:
                return (Double) value;

            case Float:
                return (double) (float) (Float) value;

            case BigDecimal:
                return  ((BigDecimal) value).doubleValue();
        }
        throw new DataAccessException("Unsupported double type");
    }

    @Override
    public void setValue(Object value, Object storage) throws IllegalAccessException {
        if (value == null) {
            mField.set(storage, null);
        } else {
            switch (mJavaClass) {
                case Double:
                    break;

                case Float:
                    value = Float.valueOf((float) (double) (Double) value);
                    break;

                case BigDecimal:
                    value = BigDecimal.valueOf((Double) value);
                    break;
            }
        }
        mField.set(storage, value);
    }

    @Override
    public Class<?> getStorageClass() {
        return mField.getDeclaringClass();
    }
}
