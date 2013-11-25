package org.db.gora;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Set;

import android.util.Log;

public interface ChildValueAccess {
	void appendChild(Object child, Object storage) throws Exception;
	Object getChildren(Object storage) throws Exception;
	Class<?> getStorageClass();

	final class SimpleFieldChildValue implements ChildValueAccess {
		public SimpleFieldChildValue(Field field) {
			this.field = field;
		}

		@Override
		public void appendChild(Object child, Object storage) throws Exception  {
			field.set(storage, child);
		}

		@Override
		public Class<?> getStorageClass() {
			return field.getDeclaringClass();
		}
		
		@Override
		public Object getChildren(Object storage) throws Exception {
			return field.get(storage);
		}
		
		private final Field field;
	}

	final class SimpleMethodChildValue implements ChildValueAccess {
		public SimpleMethodChildValue(Method method) {
			this.method = method;
		}

		@Override
		public void appendChild(Object child, Object storage) throws Exception  {
		}

		@Override
		public Class<?> getStorageClass() {
			return method.getDeclaringClass();
		}
		
		@Override
		public Object getChildren(Object storage) throws Exception {
			return method.invoke(storage, (Object[]) null);
		}
		
		private final Method method;
	}
	
	abstract class SetChildValueAccess implements ChildValueAccess {
		protected SetChildValueAccess(Class<?> storageClass) {
			this.storageClass = storageClass;
		}
		
		abstract Set<?> createChildStorage(Object storage) throws Exception;
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void appendChild(Object child, Object storage) throws Exception 
		{
			if (child == null || storage == null) {
				throw new InvalidParameterException("appendChild: null parameter");
			}
			if (!storageClass.isAssignableFrom(storage.getClass())) {
				throw new InvalidParameterException(String.format("Invalid storage class: %s, expected %s", storageClass.getName(), storage.getClass().getName()));
			}

			Set set = (Set) getChildren(storage);
			if (set == null) {
				set = createChildStorage(storage);
				
			}
			if (set != null) {
				set.add(child);
			}
		}
		protected final Class<?> storageClass; 
	}
	
	final class SetFieldChildValue extends SetChildValueAccess {
		public SetFieldChildValue(Field field) {
			super(field.getDeclaringClass());
			this.field = field;
		}

		@Override
		public Object getChildren(Object storage) throws Exception {
			return field.get(storage);
		}

		@Override
		Set<?> createChildStorage(Object storage) throws Exception	{
			Set<?> set = (Set<?>) field.getType().newInstance();
			field.set(storage, set);

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

		@Override
		Set<?> createChildStorage(Object storage) throws Exception	{
			return null;
		}

		@Override
		public Object getChildren(Object storage) throws Exception  {
			return getter.invoke(storage, (Object[]) null);
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
		
		abstract List<?> createChildStorage(Object storage) throws Exception;
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void appendChild(Object child, Object storage) throws Exception	{
			if (child == null || storage == null) {
				throw new InvalidParameterException("appendChild: null parameter");
			}
			if (!storageClass.isAssignableFrom(storage.getClass())) {
				throw new InvalidParameterException(String.format("Invalid storage class: %s, expected %s", storageClass.getName(), storage.getClass().getName()));
			}
			List list = (List) getChildren(storage);
			if (list == null) {
				list = createChildStorage(storage);
			}
			if (list != null) {
				list.add(child);
			}
		}
		
		protected final Class<?> storageClass; 
	}
	
	final class ListFieldChildValue extends ListChildValueAccess {
		public ListFieldChildValue(Field field) {
			super(field.getDeclaringClass());
			
			Class<?> listClazz = field.getType();
			if (!List.class.isAssignableFrom(listClazz)) {
				Log.e(Settings.TAG, String.format("List child value: invalid child class: %s", listClazz.getName()));
			}
			this.field = field;
		}

		@Override
		public Object getChildren(Object storage) throws Exception {
			return field.get(storage);
		}

		@Override
		List<?> createChildStorage(Object storage) throws Exception {
			List<?> list = (List<?>) field.getType().newInstance();
			field.set(storage, list);
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

		@Override
		List<?> createChildStorage(Object storage) throws Exception {
			return null;
		}

		@Override
		public Object getChildren(Object storage) throws Exception {
			return getter.invoke(storage, (Object[]) null);
		}
		
		@Override
		public Class<?> getStorageClass() {
			return getter.getDeclaringClass();
		}

		private final Method getter;
	}
}
