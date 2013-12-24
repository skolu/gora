/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.db.gora;

import java.util.Arrays;

/**
  * Defines ORM table schema
  * See {@link SQLSchema}
  *
  * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

public class TableData {
    public Class<?> tableClass;
    public String tableName;
    public int tableNo;

    public FieldData primaryKey;
    public FieldData foreignKey;
    public FieldData[] fields;
    public IndexData[] indice;
    public boolean hasKeywords;

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
