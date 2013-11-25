package org.db.gora;

public class ChildTableData {
	public ChildDataType linkType;
	
	public Class<?> parentClass;
	public ChildValueAccess valueAccessor;
	
	public Class<?> childClass;
	public FieldData foreignKeyField;
}
