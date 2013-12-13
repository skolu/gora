package org.db.gora;

import java.util.Arrays;

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

    boolean ensureIndexExists(String columnName) {
        FieldData column = null;
        for (FieldData fd: fields) {
            if (fd.columnName.equalsIgnoreCase(columnName)) {
                column = fd;
                break;
            }
        }
        if (column == null) {
            return false;
        }
        if (indice != null) {
            for (IndexData index: indice) {
                if (index.fields[0] == column) {
                    return true;
                }
            }
        }

        IndexData id = new IndexData();
        id.fields = new FieldData[] {column};
        if (indice != null) {
            indice = Arrays.copyOf(indice, indice.length + 1);
            indice[indice.length - 1] = id;
        } else {
            indice = new IndexData[] { id };
        }
        return true;
    }
}
