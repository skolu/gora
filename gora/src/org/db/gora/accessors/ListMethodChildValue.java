package org.db.gora.accessors;

import org.db.gora.ChildValueAccess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public final class ListMethodChildValue implements ChildValueAccess {
    final Method mGetter;

    public ListMethodChildValue(Method getter) {
        this.mGetter = getter;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public void appendChild(Object child, Object storage) throws InvocationTargetException, IllegalAccessException {
        List list = getChildren(storage);
        if (list != null) {
            list.add(child);
        }
    }

    @Override
    public List getChildren(Object storage) throws InvocationTargetException, IllegalAccessException {
        return (List) mGetter.invoke(storage);
    }

    @Override
    public Class<?> getStorageClass() {
        return mGetter.getDeclaringClass();
    }
}
