package org.db.gora;

public class TableData {
    public Class<?> tableClass;
    public String tableName;
    public int tableNo;

    public FieldData primaryKey;
    public FieldData foreignKey;
    public FieldData[] fields;
    public IndexData[] indice;
}
