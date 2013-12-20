package org.db.gora;

public interface ChildValueAccess {
	void appendChild(Object child, Object storage) throws Exception;
	Object getChildren(Object storage) throws Exception;
	Class<?> getStorageClass();
}
