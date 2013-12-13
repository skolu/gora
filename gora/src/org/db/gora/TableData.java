package org.db.gora;

public class TableData {
    public Class<?> tableClass;
    public String tableName;
    public int tableNo;

    public FieldData primaryKey;
    public FieldData foreignKey;
    public FieldData[] fields;
    public IndexData[] indice;

    public FieldData getFieldByName(String name) {
        if (name == null) return null;

        if (primaryKey.columnName.equalsIgnoreCase(name)) {
            return primaryKey;
        }
        if (primaryKey.fieldName != null) {
            if (primaryKey.fieldName.equalsIgnoreCase(name)) {
                return primaryKey;
            }
        }

        if (fields != null) {
            for (FieldData fd: fields) {
                if (fd.columnName.equalsIgnoreCase((name))) {
                    return fd;
                }
                if (fd.fieldName != null) {
                    if (fd.fieldName.equalsIgnoreCase(name)) {
                        return primaryKey;
                    }
                }

            }
        }

        return null;

    }
}
