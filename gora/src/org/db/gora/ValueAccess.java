package org.db.gora;

public interface ValueAccess {
	Object getValue(Object storage) throws Exception;
	void setValue(Object value, Object storage) throws Exception;
	Class<?> getStorageClass();
}
