package org.db.gora.accessors;

import org.db.gora.ValueAccess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GenericPropertyValueAccessor implements ValueAccess {

    final Method mGetter;
  	final Method mSetter;

    public GenericPropertyValueAccessor(Method getter, Method setter) {
  			this.mGetter = getter;
  			this.mSetter = setter;
  		}

  		@Override
  		public Object getValue(Object storage) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
  			return mGetter.invoke(storage);
  		}

  		@Override
  		public void setValue(Object value, Object storage) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException  {
  			mSetter.invoke(storage, value);
  		}

  		@Override
  		public Class<?> getStorageClass() {
  			return mGetter.getDeclaringClass();
  		}
}
