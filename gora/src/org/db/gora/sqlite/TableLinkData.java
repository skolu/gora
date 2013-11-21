package org.db.gora.sqlite;

public class TableLinkData {
	public ChildDataType linkType;
	
	public Class<?> parentClass;
	public ChildValueAccess valueAccessor;
	
	public Class<?> childClass;
	public FieldData foreignKeyField;
}
