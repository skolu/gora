package org.db.gora;

public class ChildTableData {
	public ChildDataType linkType;
	
	public Class<?> parent;
	public ChildValueAccess valueAccessor;

    public Class<?>[] children;
}
