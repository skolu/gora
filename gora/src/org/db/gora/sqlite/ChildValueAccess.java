package org.db.gora.sqlite;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Set;

import android.util.Log;

public interface ChildValueAccess {
	void appendChild(Object child, Object storage) throws Exception;
	Class<?> getStorageClass();

	final class SimpleChildValue implements ChildValueAccess {
		public SimpleChildValue(Field field) {
			this.field = field;
		}

		@Override
		public void appendChild(Object child, Object storage) throws IllegalArgumentException, IllegalAccessException  {
			field.set(storage, child);
		}

		@Override
		public Class<?> getStorageClass() {
			return field.getDeclaringClass();
		}
		
		private final Field field;
	}
	
	abstract class SetChildValueAccess implements ChildValueAccess {
		protected SetChildValueAccess(Class<?> storageClass) {
			this.storageClass = storageClass;
		}
		
		@SuppressWarnings("rawtypes")
		public abstract Set getChildFromStorage(Object storage) 
				throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException ;
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void appendChild(Object child, Object storage) 
				throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException 
		{
			if (child == null || storage == null) {
				throw new InvalidParameterException("appendChild: null parameter");
			}
			if (!storageClass.isAssignableFrom(storage.getClass())) {
				throw new InvalidParameterException(String.format("Invalid storage class: %s, expected %s", storageClass.getName(), storage.getClass().getName()));
			}
			Set set = getChildFromStorage(storage);
			if (set != null) {
				set.add(child);
			}
		}
		protected final Class<?> storageClass; 
	}
	
	final class SetFieldChildValueAccess extends SetChildValueAccess {
		public SetFieldChildValueAccess(Field field) {
			super(field.getDeclaringClass());
			this.field = field;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Set getChildFromStorage(Object storage) 
				throws IllegalArgumentException, IllegalAccessException, InstantiationException 
		{
			Set set = (Set) field.get(storage);
			if (set == null) {
				set = (Set) field.getType().newInstance();
				field.set(storage, set);
			}
			return set;
		}
		
		@Override
		public Class<?> getStorageClass() {
			return field.getDeclaringClass();
		}
		
		private final Field field;
	}

	final class SetMethodChildValue extends SetChildValueAccess {
		public SetMethodChildValue(Method getter) {
			super(getter.getDeclaringClass());
			this.getter = getter;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Set getChildFromStorage(Object storage) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException  {
			return (Set) getter.invoke(storage, (Object[]) null);
		}
		
		@Override
		public Class<?> getStorageClass() {
			return getter.getDeclaringClass();
		}

		private final Method getter;
	}
	
	abstract class ListChildValueAccess implements ChildValueAccess {
		protected ListChildValueAccess(Class<?> storageClass) {
			this.storageClass = storageClass;
		}
		
		@SuppressWarnings("rawtypes")
		public abstract List getChildFromStorage(Object storage) 
				throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException ;
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void appendChild(Object child, Object storage) 
				throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException 
		{
			if (child == null || storage == null) {
				throw new InvalidParameterException("appendChild: null parameter");
			}
			if (!storageClass.isAssignableFrom(storage.getClass())) {
				throw new InvalidParameterException(String.format("Invalid storage class: %s, expected %s", storageClass.getName(), storage.getClass().getName()));
			}
			List list = getChildFromStorage(storage);
			if (list != null) {
				list.add(child);
			}
		}
		
		protected final Class<?> storageClass; 
	}
	
	final class ListFieldChildValueAccess extends ListChildValueAccess {
		public ListFieldChildValueAccess(Field field) {
			super(field.getDeclaringClass());
			
			Class<?> listClazz = field.getType();
			if (!List.class.isAssignableFrom(listClazz)) {
				Log.e(Settings.TAG, String.format("List child value: invalid child class: %s", listClazz.getName()));
			}
			this.field = field;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public List getChildFromStorage(Object storage) 
				throws IllegalArgumentException, IllegalAccessException, InstantiationException 
		{
			List list = (List) field.get(storage);
			if (list == null) {
				list = (List) field.getType().newInstance();
				field.set(storage, list);
			}
			return list;
		}
		
		@Override
		public Class<?> getStorageClass() {
			return field.getDeclaringClass();
		}
		
		private final Field field;
	}
	
	final class ListMethodChildValue extends ListChildValueAccess {
		public ListMethodChildValue(Method getter) {
			super(getter.getDeclaringClass());
			Class<?> listClazz = getter.getReturnType();
			if (!List.class.isAssignableFrom(listClazz)) {
				Log.e(Settings.TAG, String.format("List child value: invalid child class: %s", listClazz.getName()));
			}
			this.getter = getter;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public List getChildFromStorage(Object storage) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException  {
			return (List) getter.invoke(storage, (Object[]) null);
		}
		
		@Override
		public Class<?> getStorageClass() {
			return getter.getDeclaringClass();
		}

		private final Method getter;
	}
}
