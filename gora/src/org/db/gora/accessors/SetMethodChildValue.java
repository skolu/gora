package org.db.gora.accessors;

import org.db.gora.ChildValueAccess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public final class SetMethodChildValue implements ChildValueAccess {
    final Method mGetter;

    public SetMethodChildValue(Method getter) {
        this.mGetter = getter;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public void appendChild(Object child, Object storage) throws InvocationTargetException, IllegalAccessException {
        Set set = getChildren(storage);
        if (set != null) {
            set.add(child);
        }
    }

    @Override
    public Set getChildren(Object storage) throws InvocationTargetException, IllegalAccessException {
        return (Set) mGetter.invoke(storage);
    }

    @Override
    public Class<?> getStorageClass() {
        return mGetter.getDeclaringClass();
    }
}
