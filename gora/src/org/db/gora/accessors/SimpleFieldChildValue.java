package org.db.gora.accessors;

import org.db.gora.ChildValueAccess;

import java.lang.reflect.Field;

public final class SimpleFieldChildValue implements ChildValueAccess {

    final Field mField;
    public SimpleFieldChildValue(Field field) {
        this.mField = field;
    }

    @Override
    public void appendChild(Object child, Object storage) throws IllegalAccessException {
        mField.set(storage, child);
    }

    @Override
    public Class<?> getStorageClass() {
        return mField.getDeclaringClass();
    }

    @Override
    public Object getChildren(Object storage) throws IllegalAccessException {
        return mField.get(storage);
    }
}

