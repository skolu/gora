package org.db.gora;

public class TableLinkData {
	public Class<?> entityClass; 
	public Class<?> linkClass; 
	public FieldData linkField;
	public WhenLinkBroken whenBroken;
}
