package org.db.gora;

public class TableLinkData {
	public Class<?> masterClass;
	public Class<?> detailClass;
	public FieldData detailField;
	public WhenLinkBroken whenBroken;
}
