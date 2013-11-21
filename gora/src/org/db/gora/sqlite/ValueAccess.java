package org.db.gora.sqlite;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public interface ValueAccess {
	Object getValue(Object storage) throws Exception;
	void setValue(Object value, Object storage) throws Exception;
	Class<?> getStorageClass();

	final public class ClassFieldValueAccess implements ValueAccess {
		public ClassFieldValueAccess(Field field) {
			this.field = field;
		}
		
		@Override
		public Object getValue(Object storage) throws IllegalArgumentException, IllegalAccessException {
			return field.get(storage);
		}

		@Override
		public void setValue(Object value, Object storage) throws IllegalArgumentException, IllegalAccessException {
			field.set(storage, value);
		}
		
		@Override
		public Class<?> getStorageClass() {
			return field.getDeclaringClass();
		}
		
		private final Field field;
	}

	final public class ClassPropertyValueAccess implements ValueAccess {
		public ClassPropertyValueAccess(Method getter, Method setter) {
			this.getter = getter;
			this.setter = setter;
		} 
		
		@Override
		public Object getValue(Object storage) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
			return getter.invoke(storage, (Object[]) null);
		}

		@Override
		public void setValue(Object value, Object storage) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException  {
			setter.invoke(storage, value);
		}
		
		@Override
		public Class<?> getStorageClass() {
			return getter.getDeclaringClass();
		}
		
		private final Method getter;
		private final Method setter;
	}

}
