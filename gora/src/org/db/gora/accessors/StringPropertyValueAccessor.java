package org.db.gora.accessors;

import org.db.gora.ValueAccess;

import java.lang.reflect.Method;

public class StringPropertyValueAccessor implements ValueAccess {

    final Method mGetter;
    final Method mSetter;
    final StringValueClass mJavaClass;
    final Object[] mValues;

    public StringPropertyValueAccessor(Method getter, Method setter) {
        this.mGetter = getter;
        this.mSetter = setter;
        this.mJavaClass = mGetter.getReturnType().isEnum() ? StringValueClass.Enum : StringValueClass.String;
        if (mJavaClass == StringValueClass.Enum) {
            mValues = mGetter.getReturnType().getEnumConstants();
        } else {
            mValues = null;
        }
    }

    @Override
    public String getValue(Object storage) throws Exception {
        Object value = mGetter.invoke(storage);
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

        mSetter.invoke(storage, value);
    }

    @Override
    public Class<?> getStorageClass() {
        return mGetter.getDeclaringClass();
    }
}
