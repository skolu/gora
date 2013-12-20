package org.db.gora.accessors;

import org.db.gora.ChildValueAccess;

import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.List;

public final class ListFieldChildValue implements ChildValueAccess {

    final Field mField;
    public ListFieldChildValue(Field field) {
        this.mField = field;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public void appendChild(Object child, Object storage) throws IllegalAccessException, InstantiationException {
        if (child == null || storage == null) {
            throw new InvalidParameterException("appendChild: null parameter");
        }

        List list = getChildren(storage);
        if (list == null) {
            list = (List) mField.getType().newInstance();
            mField.set(storage, list);
        }
        list.add(child);
    }

    @Override
    public List getChildren(Object storage) throws IllegalAccessException {
        return (List) mField.get(storage);
    }

    @Override
    public Class<?> getStorageClass() {
        return mField.getDeclaringClass();
    }
}
