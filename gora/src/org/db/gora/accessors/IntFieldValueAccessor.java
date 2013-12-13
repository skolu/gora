package org.db.gora.accessors;

import org.db.gora.DataAccessException;
import org.db.gora.ValueAccess;
import java.lang.reflect.Field;

final public class IntFieldValueAccessor implements ValueAccess {

    final Field mField;
    final IntValueClass mJavaClass;

    public IntFieldValueAccessor(Field field) {
        this.mField = field;
        mJavaClass = IntValueClass.resolveType(field.getType());
    }

    @Override
    public Integer getValue(Object storage) throws IllegalAccessException, DataAccessException {
        Object value = mField.get(storage);
        if (value == null) {
            return Integer.valueOf(0);
        }

        switch (mJavaClass) {
            case Integer:
                return (Integer) value;

            case Short:
                return (int) (short) (Short) value;

            case Byte:
                return (int) (byte) (Byte) value;
        }
        throw new DataAccessException("Unsupported integer type");
    }

    @Override
    public void setValue(Object value, Object storage) throws IllegalAccessException {
        if (value == null) {
            value = Integer.valueOf(0);
        }
        switch (mJavaClass) {
            case Integer:
                break;

            case Short:
                value = (short) (((Integer) value) & 0xffff);
                break;

            case Byte:
                value = (byte) (((Integer) value) & 0xff);
                break;
        }

        mField.set(storage, value);
    }

    @Override
    public Class<?> getStorageClass() {
        return mField.getDeclaringClass();
    }
}
