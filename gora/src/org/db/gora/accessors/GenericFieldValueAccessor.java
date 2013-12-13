package org.db.gora.accessors;

import org.db.gora.ValueAccess;

import java.lang.reflect.Field;

public class GenericFieldValueAccessor implements ValueAccess {
    final Field mField;

    public GenericFieldValueAccessor(Field field) {
  			this.mField = field;
  		}

  		@Override
  		public Object getValue(Object storage) throws IllegalAccessException {
  			return mField.get(storage);
  		}

  		@Override
  		public void setValue(Object value, Object storage) throws IllegalAccessException {
  			mField.set(storage, value);
  		}

  		@Override
  		public Class<?> getStorageClass() {
  			return mField.getDeclaringClass();
  		}
}
