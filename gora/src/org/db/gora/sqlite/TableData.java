package org.db.gora.sqlite;

public class TableData {
    public Class<?> tableClass;
    public String tableName;
    public int tableNo;

    public FieldData primaryKey;
    public FieldData foreignKey;
    public FieldData[] fields;
    public IndexData[] indice;

}
