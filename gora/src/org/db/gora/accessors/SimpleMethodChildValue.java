package org.db.gora.accessors;

import org.db.gora.ChildValueAccess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class SimpleMethodChildValue implements ChildValueAccess {
    final Method mGetter;
    final Method mSetter;

    public SimpleMethodChildValue(Method getter, Method setter) {
        this.mGetter = getter;
        this.mSetter = setter;
    }

    @Override
    public void appendChild(Object child, Object storage) throws InvocationTargetException, IllegalAccessException {
        mSetter.invoke(storage, child);
    }

    @Override
    public Class<?> getStorageClass() {
        return mGetter.getDeclaringClass();
    }

    @Override
    public Object getChildren(Object storage) throws InvocationTargetException, IllegalAccessException {
        return mGetter.invoke(storage);
    }
}

