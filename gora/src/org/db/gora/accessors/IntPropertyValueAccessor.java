package org.db.gora.accessors;

import org.db.gora.ValueAccess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class IntPropertyValueAccessor implements ValueAccess {

    final Method mGetter;
    final Method mSetter;
    final IntValueClass mJavaClass;

    public IntPropertyValueAccessor(Method getter, Method setter) {
        this.mGetter = getter;
        this.mSetter = setter;
        mJavaClass = IntValueClass.resolveType(getter.getReturnType());
    }

    @Override
    public Integer getValue(Object storage) throws InvocationTargetException, IllegalAccessException {
        Object value = mGetter.invoke(storage);
        if (value != null) {
            switch (mJavaClass) {
                case Integer:
                    return (Integer) value;

                case Short:
                    return (int) (short) (Short) value;

                case Byte:
                    return (int) (byte) (Byte) value;
            }
        }
        return 0;
    }

    @Override
    public void setValue(Object value, Object storage) throws InvocationTargetException, IllegalAccessException {
        if (value != null) {
            switch (mJavaClass) {
                case Integer:
                    break;

                case Short:
                    value = Short.valueOf((short) ((Integer) value & 0xffff));
                    break;

                case Byte:
                    value = Byte.valueOf((byte) ((Integer) value & 0xff));
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
