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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Extends {@link SQLiteOpenHelper} to support SQLite database schema modification
 * according to {@link SqlSchema}
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    /**
     * Creates a new {@link DatabaseHelper} class instance.
     *
     * @param context       Android context
     * @param databaseName  Database name. null if in-memory database.
     * @param schema        Schema
     */
    public DatabaseHelper(Context context, String databaseName, SqlSchema schema) {
        super(context, databaseName, null, schema.getDatabaseVersion());

        mSchema = schema;
    }

    final SqlSchema mSchema;

    public SqlSchema getSchema() { return mSchema; }

    @Override
    public void onCreate(SQLiteDatabase db) {
        adjustDatabaseSchema(db, mSchema);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        adjustDatabaseSchema(db, mSchema);
    }

    static String getTableSyntax(DbTableInfo info) {
        StringBuilder builder = new StringBuilder();

        builder.append("Create Table ");
        builder.append(info.tableName);
        builder.append("(");
        builder.append(info.pkName);
        builder.append(" Integer Primary Key Autoincrement");
        for (int i = 0; i < info.columns.size(); ++i) {
            DbColumnInfo ci = info.columns.get(i);
            builder.append(", ");
            builder.append(ci.columnName);
            builder.append(" ");
            switch (ci.columnType) {
                case INT:
                    builder.append("Integer");
                    break;
                case REAL:
                    builder.append("Real");
                    break;
                case TEXT:
                    builder.append("Text");
                    break;
                case BLOB:
                    builder.append("Blob");
                    break;
            }

            if (!ci.isNull) {
                builder.append(" Not Null");
            }
        }
        builder.append(");\n");

        return builder.toString();
    }

    static char[] sCamelCase = new char[1024];
    static synchronized String toCamelCase(String str) {
        if (str == null) return null;
        int pos = 0;
        boolean doCapitalize = true;
        for (int i = 0; i < str.length(); ++i) {
            char ch = str.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                if (doCapitalize && Character.isLetter(ch)) {
                    ch = Character.toUpperCase(ch);
                }
                sCamelCase[pos] = ch;
                ++pos;
                doCapitalize = false;
            }
            else if (ch == '_') {
                doCapitalize = true;
            }
        }

        return new String(sCamelCase, 0, pos);
    }

    static String getColumnSyntax(String tableName, DbColumnInfo columnInfo) {
        StringBuilder builder = new StringBuilder();

        builder.append("Alter Table ");
        builder.append(tableName);
        builder.append(" Add Column ");
        builder.append(columnInfo.columnName);
        builder.append(" ");
        switch (columnInfo.columnType) {
            case INT:
                builder.append("Integer");
                break;
            case REAL:
                builder.append("Real");
                break;
            case TEXT:
                builder.append("Text");
                break;
            case BLOB:
                builder.append("Blob");
                break;
        }
        builder.append(";\n");

        return builder.toString();
    }

    static String getIndexSyntax(DbIndexInfo indexInfo) {
        StringBuilder builder = new StringBuilder();

        builder.append("Create Index ");
        if (indexInfo.indexName == null) {
            builder.append(toCamelCase(indexInfo.tableName));
            for (String columnName: indexInfo.columns) {
                builder.append(toCamelCase(columnName));
            }
            builder.append("Idx");
        }
        builder.append(" On ");
        builder.append(indexInfo.tableName);
        builder.append("(");
        for (int i = 0; i < indexInfo.columns.size(); ++i) {
            String columnName = indexInfo.columns.get(i);
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(columnName);
        }
        builder.append(");\n");

        return builder.toString();
    }

    static DbColumnInfo getColumnInfo(FieldData fieldData) {
        DbColumnInfo columnInfo = new DbColumnInfo();
        columnInfo.columnName = fieldData.columnName;
        switch (fieldData.dataType) {
            case BOOLEAN:
            case INT:
            case LONG:
            case DATE:
                columnInfo.columnType = DbColumnType.INT;
                break;

            case DOUBLE:
                columnInfo.columnType = DbColumnType.REAL;
                break;

            case BYTE_ARRAY:
                columnInfo.columnType = DbColumnType.BLOB;
                break;

            case STRING:
                columnInfo.columnType = DbColumnType.TEXT;
                break;

            default:
                Log.w(TAG, String.format("Unsupported data type: \"%s\"", fieldData.dataType.toString()));
                columnInfo.columnType = DbColumnType.TEXT;
                break;
        }
        columnInfo.isNull = fieldData.nullable;

        return columnInfo;
    }

    protected static void adjustDatabaseSchema(SQLiteDatabase db, SqlSchema sqlSchema) {

        Map<String, DbColumnType> dbColumns = new HashMap<>();
        List<DbIndexInfo> dbIndice = new ArrayList<>();

        String pkColumn;
        for (TableData tableData: sqlSchema.tableMap.values()) {
            pkColumn = null;
            dbColumns.clear();

            DbTableInfo tableInfo = new DbTableInfo(tableData.tableName, tableData.primaryKey.columnName);
            for (FieldData field: tableData.fields) {
                if (field != tableData.primaryKey) {
                    DbColumnInfo column = getColumnInfo(field);
                    tableInfo.columns.add(column);
                }
            }

            Cursor fieldCursor = db.rawQuery(String.format("Pragma table_info('%s');", tableData.tableName), null);
            while (fieldCursor.moveToNext()) {
                String fName = fieldCursor.getString(1);
                if (fName != null) {
                    fName = fName.toUpperCase();
                } else {
                    Log.e(TAG, "Pragma table_info; see docs");
                    continue;
                }
                String fType = fieldCursor.getString(2);
                if (fType != null) {
                    fType = fType.toUpperCase();
                } else {
                    Log.e(TAG, "Pragma table_info; see docs");
                    fType = "TEXT";
                }
                if (pkColumn == null) {
                    pkColumn = fName;
                    if (!fType.startsWith("INT")) {
                        Log.e(TAG, String.format("Primary key %s in table %s is expected to be integer", pkColumn, tableInfo.tableName));
                    }
                } else {
                    DbColumnType dct = DbColumnType.TEXT;
                    if (fType.startsWith("INT")) {
                        dct = DbColumnType.INT;
                    } else if (fType.equals("REAL")) {
                        dct = DbColumnType.REAL;
                    } else if (fType.equals("BLOB")) {
                        dct = DbColumnType.BLOB;
                    }

                    dbColumns.put(fName.toUpperCase(), dct);
                }
            }
            fieldCursor.close();

            if (dbColumns.size() > 0) { //table exists
                if (tableInfo.pkName.equalsIgnoreCase(pkColumn)) {
                    Log.e(TAG, String.format("Primary key field name for table %s are different: Database: %s,  Schema: %s", tableInfo.tableName, pkColumn, tableInfo.pkName));
                }
                for (DbColumnInfo cInfo : tableInfo.columns) {
                    DbColumnType dbInfo = dbColumns.get(cInfo.columnName.toUpperCase());
                    if (dbInfo == null) {
                        String colSyntax = getColumnSyntax(tableInfo.tableName, cInfo);
                        Log.i(TAG, colSyntax);
                        db.execSQL(colSyntax);
                    }
                }
            } else {
                String tblSyntax = getTableSyntax(tableInfo);
                Log.i(TAG, tblSyntax);
                db.execSQL(tblSyntax);
            }

            if (tableData.indice != null) {
                dbIndice.clear();
                Cursor indexCursor = db.rawQuery(String.format("Pragma index_list('%s');", tableData.tableName), null);
                while (indexCursor.moveToNext()) {
                    DbIndexInfo indexInfo = new DbIndexInfo();
                    indexInfo.tableName = tableData.tableName;
                    indexInfo.indexName = indexCursor.getString(1);
                    indexInfo.isUnique = indexCursor.getInt(2) != 0;
                    dbIndice.add(indexInfo);
                }
                indexCursor.close();

                for (DbIndexInfo indexInfo: dbIndice) {
                    if (indexInfo.columns == null) {
                        indexInfo.columns = new ArrayList<>();
                    } else {
                        indexInfo.columns.clear();
                    }
                    indexCursor = db.rawQuery(String.format("Pragma index_info('%s');", indexInfo.indexName), null);
                    while (indexCursor.moveToNext()) {
//                      int rank = indexCursor.getInt(1);
                        String idxColumn = indexCursor.getString(2);
                        indexInfo.columns.add(idxColumn);
                    }
                    indexCursor.close();
                }

                for (IndexData indexData: tableData.indice) {
                    DbIndexInfo foundIndex = null;
                    for (DbIndexInfo indexInfo: dbIndice) {
                        if (indexData.fields.length == indexInfo.columns.size()) {
                            boolean ok = true;
                            for (int i = 0; i < indexData.fields.length; ++i) {
                                if (!indexData.fields[i].columnName.equalsIgnoreCase(indexInfo.columns.get(i))) {
                                    ok = false;
                                    break;
                                }
                            }
                            if (ok) {
                                foundIndex = indexInfo;
                                break;
                            }
                        }
                    }
                    if (foundIndex == null) {
                        foundIndex = new DbIndexInfo();
                        foundIndex.tableName = tableData.tableName;
                        foundIndex.columns = new ArrayList<>();
                        for (FieldData fd :indexData.fields) {
                            foundIndex.columns.add(fd.columnName);
                        }

                        String idxSyntax = getIndexSyntax(foundIndex);
                        Log.i(TAG, idxSyntax);
                        db.execSQL(idxSyntax);
                    }
                }

                if (tableData.hasKeywords) {
                    String ftsSyntax = String.format("CREATE VIRTUAL TABLE IF NOT EXISTS %s_KW USING FTS4(tokenize=porter);", tableData.tableName);
                    Log.i(TAG, ftsSyntax);
                    try {
                        db.execSQL(ftsSyntax);
                    }
                    catch (Exception e) {
                        Log.i(TAG, "FTS", e);
                    }
                }
            }
        }
    }

    private static final String TAG = "GORA";
}
