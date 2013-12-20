package org.db.gora.accessors;

import org.db.gora.ChildValueAccess;

import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.Set;

public final class SetFieldChildValue implements ChildValueAccess {

    final Field mField;
    public SetFieldChildValue(Field field) {
        this.mField = field;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public void appendChild(Object child, Object storage) throws IllegalAccessException, InstantiationException {
        if (child == null || storage == null) {
            throw new InvalidParameterException("appendChild: null parameter");
        }

        Set set = getChildren(storage);
        if (set == null) {
            set = (Set) mField.getType().newInstance();
            mField.set(storage, set);
        }
        set.add(child);
    }

    @Override
    public Set getChildren(Object storage) throws IllegalAccessException {
        return (Set) mField.get(storage);
    }

    @Override
    public Class<?> getStorageClass() {
        return mField.getDeclaringClass();
    }
}
