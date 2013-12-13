package org.db.gora.accessors;

import org.db.gora.ValueAccess;

import java.lang.reflect.Method;
import java.math.BigDecimal;

public class DoublePropertyValueAccessor implements ValueAccess {
    final Method mGetter;
    final Method mSetter;
    final DoubleValueClass mJavaClass;

    public DoublePropertyValueAccessor(Method getter, Method setter) {
        this.mGetter = getter;
        this.mSetter = setter;
        mJavaClass = DoubleValueClass.resolveType(getter.getReturnType());
    }


    @Override
    public Double getValue(Object storage) throws Exception {
        Object value = mGetter.invoke(storage);
        if (value != null) {
            switch (mJavaClass) {
                case Double:
                    return (Double) value;

                case Float:
                    return (double) (float) (Float) value;

                case BigDecimal:
                    return ((BigDecimal) value).doubleValue();
            }
        }
        return Double.valueOf(0.0);
    }

    @Override
    public void setValue(Object value, Object storage) throws Exception {
        if (value != null) {
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
        mSetter.invoke(storage, value);
    }

    @Override
    public Class<?> getStorageClass() {
        return mGetter.getDeclaringClass();
    }
}
