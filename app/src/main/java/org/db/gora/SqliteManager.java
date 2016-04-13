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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

/**
 * Android SQLite implementation of {@link DataManager}
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

public class SqliteManager implements DataManager {
	final SQLiteDatabase mDb;
	final SqlSchema mSchema;

	private final ConcurrentLinkedQueue<ContentValues> mValues = new ConcurrentLinkedQueue<>();
    private final Map<Integer, SQLiteStatement> preparedStmts = new HashMap<>();

	public SqliteManager(SQLiteDatabase db, SqlSchema schema) {
		mDb = db;
		mSchema = schema;

        prepareStatements = false;
	}

    private boolean prepareStatements;
    public boolean getPrepareStatements() {
        return prepareStatements;
    }
    public void setPrepareStatements(boolean value) {
        prepareStatements = value;
        if (!prepareStatements) {
            synchronized (preparedStmts) {
                for (Map.Entry<Integer, SQLiteStatement> stmt: preparedStmts.entrySet()) {
                    stmt.getValue().close();
                }
                preparedStmts.clear();
            }
        }
    }

    public long[] queryLinks(Class<?> detailClazz, Class<?> masterClazz, long masterId) throws DataAccessException, DataIntegrityException {
        if (detailClazz == null) {
            throw new DataIntegrityException("SQLiteManager: gueryLinks: Null detail class");
        }
        if (masterClazz == null) {
            throw new DataIntegrityException("SQLiteManager: gueryLinks: Null master class");
        }

        TableData detailTable = mSchema.getTableData(detailClazz);
        if (detailTable == null) {
            throw new DataIntegrityException(String.format("SQLiteManager: Class %s is not registered", detailClazz.getName()));
        }

        TableData masterTable = mSchema.getTableData(masterClazz);
        if (masterTable == null) {
            throw new DataIntegrityException(String.format("SQLiteManager: Class %s is not registered", masterClazz.getName()));
        }

        List<TableLinkData> links = mSchema.getDetailLinks(masterClazz);
        if (links == null) return null;

        long[] ids = new long[256];
        int pos = 0;

        for (TableLinkData tld: links) {
            String query = null;
            if (tld.detailClass == detailClazz) {
                query = String.format("SELECT %s FROM %s WHERE %s=? ", detailTable.primaryKey.columnName, detailTable.tableName, tld.detailField.columnName);
            } else {
                Class<?> parent = mSchema.getParentClass(tld.detailClass);
                if (parent != null) {
                    if (parent == detailClazz) {
                        TableData queryTable = mSchema.getTableData(tld.detailClass);
                        if (queryTable == null) {
                            throw new DataIntegrityException(String.format("SQLiteManager: Class %s is not registered", tld.detailClass.getName()));
                        }
                        query = String.format("SELECT DISTINCT %s FROM %s WHERE %s=? ", queryTable.foreignKey.columnName, queryTable.tableName, tld.detailField.columnName);
                    } else {
                        while (parent != null) {
                            if (parent == detailClazz) {
                                break;
                            }
                            parent = mSchema.getParentClass(parent);
                        }
                        if (parent != null) {
                            StringBuilder builder = new StringBuilder();
                            builder.append(String.format("%s", detailTable.tableName));
                            TableData lastTable = detailTable;
                            parent = mSchema.getParentClass(tld.detailClass);
                            while (parent != detailClazz) {
                                TableData thisTable = mSchema.getTableData(parent);
                                if (thisTable == null) {
                                    throw new DataIntegrityException(String.format("SQLiteManager: Class %s is not registered", parent.getName()));
                                }

                                builder.append(String.format(" INNER JOIN %s ON %s.%s = %s.%s",
                                        thisTable.tableName,
                                        thisTable.tableName, thisTable.primaryKey.columnName,
                                        lastTable.tableName, lastTable.foreignKey.columnName));

                                lastTable = thisTable;
                                parent = mSchema.getParentClass(parent);
                                if (parent == null) {
                                    break;
                                }
                            }
                            if (parent == detailClazz) {
                                query = String.format("SELECT DISTINCT %s.%s FORM %s WHERE %s.%s=?",
                                        lastTable.tableName, lastTable.foreignKey.columnName,
                                        builder.toString(),
                                        detailTable.tableName, tld.detailField.columnName);
                            }
                        }
                    }
                }
            }

            if (query != null) {
                Cursor cursor = mDb.rawQuery(query, new String[] {Long.toString(masterId)});
                if (cursor != null) {
                    if (cursor.moveToNext()) {
                        ids[pos] = cursor.getLong(0);
                        ++pos;
                        if (pos >= ids.length) {
                            ids = Arrays.copyOf(ids, ids.length * 2);
                        }
                    }
                    cursor.close();
                    return Arrays.copyOf(ids, pos);
                }
            }

        }
        if (pos > 0) {
            pos = mergeIds(ids, pos);
            return Arrays.copyOf(ids, pos);
        }
        return null;
    }

    public <T> PredicateBuilder getPredicateBuilder(Class<T> clazz) throws DataAccessException {
        if (clazz == null) {
            throw new DataAccessException("SQLiteManager: Write: Null class");
        }
        TableData tableData = mSchema.getTableData(clazz);
        if (tableData == null) {
            throw new DataAccessException(String.format("SQLiteManager: Class %s is not registered", clazz.getName()));
        }
        return new PredicateBuilder(tableData);
    }

    @Override
    public long[] queryIds(Class<?> clazz, String where, String[] whereArgs, String orderBy) throws DataAccessException {
        if (clazz == null) {
            throw new DataAccessException("SQLiteManager: QueryIds: Null class");
        }
        if (mDb == null) {
            throw new DataAccessException("SQLiteManager: QueryIds: Sqlite database is null");
        }
        if (!mDb.isOpen()) {
            throw new DataAccessException("SQLiteManager: QueryIds: Sqlite database is not open");
        }

        TableData tableData = mSchema.getTableData(clazz);
        if (tableData == null) {
            throw new DataAccessException(String.format("SQLiteManager: QueryIds: class %s is not registered", clazz.getName()));
        }

        if (where == null) {
            where = "1";
        } else if (where.length() == 0) {
            where = "1";
        }

        Cursor cursor = mDb.query(tableData.tableName, new String[] {tableData.primaryKey.columnName}, where, whereArgs, null, null, orderBy);
        if (cursor != null) {
            long[] ids = new long[256];
            int pos = 0;
            if (cursor.moveToNext()) {
                ids[pos] = cursor.getLong(0);
                ++pos;
                if (pos >= ids.length) {
                    ids = Arrays.copyOf(ids, ids.length * 2);
                }
            }
            cursor.close();
            return Arrays.copyOf(ids, pos);
        }

        return null;
    }

    @Override
    public FieldCursor queryFields(Class<?> clazz, String where, String[] whereArgs, String... fields) throws DataAccessException, DataIntegrityException {
        if (clazz == null) {
            throw new DataAccessException("SQLiteManager: QueryIds: Null class");
        }
        if (mDb == null) {
            throw new DataAccessException("SQLiteManager: QueryIds: Sqlite database is null");
        }
        if (!mDb.isOpen()) {
            throw new DataAccessException("SQLiteManager: QueryIds: Sqlite database is not open");
        }

        TableData tableData = mSchema.getTableData(clazz);
        if (tableData == null) {
            throw new DataAccessException(String.format("SQLiteManager: QueryIds: class %s is not registered", clazz.getName()));
        }

        if (where == null) {
            where = "1";
        } else if (where.length() == 0) {
            where = "1";
        }


        final FieldData[] fieldData = new FieldData[fields.length + 1];
        for (int i = 0; i < fields.length; ++i) {
            FieldData fd = tableData.getFieldByName(fields[i]);
            if (fd == null) {
                throw new DataIntegrityException(String.format("Column %s has not been found in Table %s", fields[i], tableData.tableName));
            }

            fieldData[i] = fd;
        }
        fieldData[fields.length] = tableData.primaryKey;


        String[] columns = new String[fields.length + 1];
        for (int i = 0; i < fieldData.length; ++i) {
            FieldData fd = fieldData[i];
            columns[i] = fd.columnName;
        }

        final Cursor cursor = mDb.query(tableData.tableName, columns, where, whereArgs, null, null, null);
        if (cursor != null) {
            if (cursor.isBeforeFirst()) {
                cursor.moveToNext();
            }

            return new FieldCursor() {
                Cursor mCursor = cursor;
                FieldData[] fields = fieldData;

                @Override
                public long getId() throws DataAccessException {
                    if (mCursor == null) throw new DataAccessException("Cursor contains no data");
                    return mCursor.getLong(fields.length - 1);
                }

                @Override
                public Object getFieldValue(int fieldNo) throws DataAccessException {
                    if (mCursor == null) throw new DataAccessException("Cursor contains no data");
                    if (fieldNo >= 0 && fieldNo < fields.length) {
                        if (mCursor.isNull(fieldNo)) return null;

                        FieldData fd = fields[fieldNo];
                        switch (fd.dataType) {
                            case INT:
                                return mCursor.getString(fieldNo);
                            case DOUBLE:
                                return mCursor.getDouble(fieldNo);
                            case BOOLEAN:
                                return mCursor.getInt(fieldNo) != 0;
                            case DATE:
                                return new Date(mCursor.getLong(fieldNo));
                            case LONG:
                                return mCursor.getLong(fieldNo);
                            case STRING:
                                return mCursor.getString(fieldNo);
                            case BYTE_ARRAY:
                                return mCursor.getBlob(fieldNo);
                            default:
                                return null;
                        }
                    }
                    throw new IndexOutOfBoundsException();
                }

                @Override
                public FieldDataType getFieldType(int fieldNo) {
                    if (fieldNo >= 0 && fieldNo < fields.length) {
                        return fields[fieldNo].dataType;
                    }
                    throw new IndexOutOfBoundsException();
                }

                @Override
                public boolean eof() {
                    if (mCursor == null) return true;
                    if (mCursor.isClosed()) {
                        mCursor = null;
                        return true;
                    }

                    if (mCursor.isAfterLast()) {
                        close();
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean next() {
                    if (mCursor == null) return false;
                    boolean b = cursor.moveToNext();
                    if (!b) {
                        close();
                    }
                    return b;
                }

                @Override
                public void close() {
                    if (mCursor == null) return;
                    if (!mCursor.isClosed()) {
                        mCursor.close();
                    }
                    mCursor = null;
                }
            };
        }

        return null;
    }

    /**
     * Retrieves entities according to where clause
     */
    public <T> ClosableIterator<T> query (Class<T> clazz, String where, String[] whereArgs) throws DataAccessException {
        if (clazz == null) {
            throw new DataAccessException("SQLiteManager: Query: Null class");
        }
        if (mDb == null) {
            throw new DataAccessException("SQLiteManager: Query: Sqlite database is null");
        }
        if (!mDb.isOpen()) {
            throw new DataAccessException("SQLiteManager: Query: Sqlite database is not open");
        }

        TableQueryBuilder builder = mSchema.getQueryBuilder(clazz);
        if (builder == null) {
            throw new DataAccessException(String.format("SQLiteManager: Query: class %s is not registered", clazz.getName()));
        }

        if (where == null) {
            where = "1";
        } else if (where.length() == 0) {
            where = "1";
        }

        return query(builder, where, whereArgs);
    }

    private <T> ClosableIterator<T> query (TableQueryBuilder builder, String where, final String[] whereArgs) {
        final String query = builder.getSelectQuery() + " WHERE " + where;
        final TableData tableData = builder.tableData;
        final List<ChildTableData> children = mSchema.getChildren(tableData.tableClass);

        return new ClosableIterator<T>() {
            Cursor cursor = mDb.rawQuery(query, whereArgs);
            @Override
            public boolean hasNext() {
                if (cursor == null) return false;

                if (cursor.isLast() || cursor.isAfterLast()) {
                    close();
                    cursor = null;
                    return false;
                }

                return true;
            }

            @SuppressWarnings("unchecked")
            @Override
            public T next() {
                if (cursor == null) {
                    throw new NoSuchElementException();
                }

                if (cursor.moveToNext()) {
                    try {
                        T entity = (T) tableData.tableClass.newInstance();

                        populateStorage(entity, tableData.fields, cursor);

                        long id = (Long) tableData.primaryKey.valueAccessor.getValue(entity);

                        if (children != null) {
                            Object[] parents = new Object[] {entity};
                            for (ChildTableData child: children) {
                                readChildren(id, tableData.tableClass, parents, child);
                            }
                        }

                        if (EntityEvent.class.isAssignableFrom(entity.getClass())) {
                            ((EntityEvent) entity).onRead();
                        }

                        return entity;

                    } catch (Exception e) {
                        Log.e(TAG, "SqlManager: query", e);
                        throw new NoSuchElementException();
                    }
                } else {
                    close();
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void close() {
                if (cursor != null) {
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                    cursor = null;
                }
            }
        };
    }

	/**
	 * Writes (insert or update) an entity. 
	 */
	@Override
	public <T> boolean write(T entity) throws DataAccessException {
		if (entity == null) {
			throw new DataAccessException("SQLiteManager: Write: Null object");
		}
		if (mDb == null) {
			throw new DataAccessException("SQLiteManager: Write: Sqlite database is null");
		}
		if (!mDb.isOpen()) {
			throw new DataAccessException("SQLiteManager: Write: Sqlite database is not open");
		}
		if (mDb.isReadOnly()) {
			throw new DataAccessException("SQLiteManager: Write: Sqlite database is read-only");
		}

        if (EntityEvent.class.isAssignableFrom(entity.getClass())) {
            if (!((EntityEvent) entity).onWrite()) {
                return false;
            }
        }
		
		mDb.beginTransactionNonExclusive();
		try {

			long id = write(entity, 0, true);

            if (EntityKeyword.class.isAssignableFrom(entity.getClass())) {
                String keywords = ((EntityKeyword) entity).getKeywords();
                if (keywords == null) {
                    keywords = "";
                }

                TableData data = mSchema.getTableData(entity.getClass());
                if (data != null) {
                    String query = String.format("REPLACE INTO %s_KW (docid, content) VALUES (?, ?);", data.tableName);
                    try {
                        mDb.execSQL(query, new String[] {Long.toString(id), keywords.toLowerCase()});
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Update Keywords", e);
                    }
                }
            }

			mDb.setTransactionSuccessful();
            return true;
		} catch (Exception e) {
			throw new DataAccessException("SQLiteManager: Write: Internal exception", e);
		} finally {
			mDb.endTransaction();
		}
	}

    /**
	 * Deletes an entity
	 */
	@Override
    public void delete(Class<?> clazz, long id) throws DataAccessException {
        if (clazz == null) {
            throw new DataAccessException("SQLiteManager: Read: class is null");
        }
        if (mDb == null) {
            throw new DataAccessException("SQLiteManager: Read: Sqlite database is null");
        }
        if (!mDb.isOpen()) {
            throw new DataAccessException("SQLiteManager: Read: Sqlite database is not open");
        }
        if (mDb.isReadOnly()) {
            throw new DataAccessException("SQLiteManager: Read: Sqlite database is read-only");
        }

        mDb.beginTransactionNonExclusive();
        try {
            deleteChildren(id, clazz, clazz);

            List<TableLinkData> links = mSchema.getDetailLinks(clazz);
            if (links != null) {
                for (TableLinkData link: links) {
                    if (link.whenBroken == WhenLinkBroken.UNLINK) {
                        TableData linkData = mSchema.getTableData(link.detailClass);
                        if (linkData != null) {
                            ContentValues values = mValues.poll();
                            if (values == null) {
                                values = new ContentValues();
                            } else {
                                values.clear();
                            }
                            values.put(link.detailField.columnName, 0L);
                            mDb.update(linkData.tableName, values, link.detailField.columnName + " = ?", new String[] {Long.toString(id)});
                            mValues.add(values);
                        }
                    }
                }
            }

            if (EntityKeyword.class.isAssignableFrom(clazz)) {
                TableData data = mSchema.getTableData(clazz);
                if (data != null) {
                    String query = String.format("DELETE FROM %s_KW WHERE docid = ?", data.tableName);
                    try {
                        mDb.execSQL(query, new String[] {Long.toString(id)});
                    }
                    catch (Exception e) {
                        Log.e(TAG, "Delete Keywords", e);
                    }
                }
            }

            mDb.setTransactionSuccessful();
        } catch (Exception e) {
            throw new DataAccessException("SQLiteManager: Delete: Internal exception", e);
        } finally {
            mDb.endTransaction();
        }
    }

    private void deleteChildren(long id, Class<?> idClazz, Class<?> toDelete) throws DataIntegrityException {
        List<ChildTableData> children = mSchema.getChildren(toDelete);
        if (children != null) {
            for (ChildTableData child: children) {
                for (Class<?> childClass: child.children) {
                    deleteChildren(id, idClazz, childClass);
                }
            }
        }

        if (idClazz != toDelete) {
            TableQueryBuilder.LinkedQueryBuilder builder = mSchema.getLinkedQueryBuilder(toDelete, idClazz);
            if (builder == null) {
                throw new DataIntegrityException(
                        String.format("SQLiteManager: deleteChildren: classes %s and %s are unrelated.", idClazz.getName(), toDelete.getName()));
            }
            mDb.delete(builder.getTableData().tableName, builder.getDeleteByIdWhereClause(), new String[] {Long.toString(id)});
        } else {
            TableQueryBuilder builder = mSchema.getQueryBuilder(idClazz);
            if (builder == null) {
                throw new DataIntegrityException(
                        String.format("SQLiteManager: deleteChildren: classes %s is not registered.", idClazz.getName()));
            }
            mDb.delete(builder.tableData.tableName, builder.getDeleteByIdWhereClause(), new String[] {Long.toString(id)});
        }
    }

    static class KeywordRecord {
        public KeywordRecord(long id, double weight) {
            this.id = id;
            this.weight = weight;
        }
        long id;
        double weight;
    }

    static Comparator<KeywordRecord> sKeywordWeightComparator = new Comparator<KeywordRecord>() {
        @Override
        public int compare(KeywordRecord rec1, KeywordRecord rec2) {
            if (rec1.weight < rec2.weight) {
                return 1;
            }
            else if (rec1.weight > rec2.weight) {
                return -1;
            }
            return 0;
        }
    };

    static Comparator<KeywordRecord> sKeywordIdComparator = new Comparator<KeywordRecord>() {
        @Override
        public int compare(KeywordRecord rec1, KeywordRecord rec2) {
            if (rec1.id < rec2.id) {
                return -1;
            }
            else if (rec1.id > rec2.id) {
                return 1;
            }
            return 0;
        }
    };

    public long[] queryKeywords(Class<?> clazz, String criteria) throws DataAccessException, DataIntegrityException {
        if (clazz == null) {
            throw new DataAccessException("SQLiteManager: Query Keywords: class is null");
        }
        if (mDb == null) {
            throw new DataAccessException("SQLiteManager: Query Keywords: Sqlite database is null");
        }
        if (!mDb.isOpen()) {
            throw new DataAccessException("SQLiteManager: Query Keywords: Sqlite database is not open");
        }

        TableData tableData = mSchema.getTableData(clazz);
        if (tableData == null) {
            throw new DataAccessException(String.format("SQLiteManager: Query Keywords: class %s is not registered", clazz.getName()));
        }

        if (!tableData.hasKeywords) {
            throw new DataAccessException(String.format("SQLiteManager: Query Keywords: class %s has no keywords", clazz.getName()));
        }

        List<KeywordRecord> keywordRecords = new ArrayList<KeywordRecord>();

        String query = String.format(Locale.getDefault(),
                "SELECT docid, matchinfo(%s, 'pcx') FROM %<s WHERE content MATCH :1",
                String.format("%s_KW", tableData.tableName));
        Cursor cursor = mDb.rawQuery(query, new String[]{ criteria.toLowerCase() });
        if (cursor != null) {
            try {
                if (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    byte[] matchInfo = cursor.getBlob(1);
                    if (matchInfo != null) {
                        IntBuffer match = ByteBuffer.wrap(matchInfo).order(ByteOrder.nativeOrder()).asIntBuffer();
                        int phrases = match.get();
                        int columns = match.get();
                        double weight = 0.0;
                        for (int phrase = 0; phrase < phrases; ++phrase) {
                            for (int column = 0; column < columns; ++column) {
                                if (match.remaining() >= 3) {
                                    int this_row_hits = match.get();
                                    int all_row_hits = match.get();
                                    int docs_with_hits = match.get();

                                    if (this_row_hits > 0) {
                                        double rank = (double) this_row_hits / ((double) all_row_hits / (double) docs_with_hits);
                                        rank *= 1.2 - ((double) phrase * 0.1);

                                        weight += rank;
                                    }
                                }
                            }
                        }
                        keywordRecords.add(new KeywordRecord(id, weight));
                    }
                }
            } finally {
                cursor.close();
            }
        }

        if (keywordRecords.size() > 0) {
            if (keywordRecords.size() > 1) {
                Collections.sort(keywordRecords, sKeywordWeightComparator);
            }
            int records = keywordRecords.size();
            if (records > 1024) {
                records = 1024;
            }
            long[] ids = new long[records];
            for (int i = 0; i < records; ++i) {
                ids[i] = keywordRecords.get(i).id;
            }
            return ids;
        }

        return new long[0];
    }

    /**
     * Reads an entity by id
     * @throws DataAccessException
     */
    @Override
    public <T> T read(Class<T> clazz, long id) throws DataAccessException {
        return read(clazz, id, false);
    }

    private <T> T read(Class<T> clazz, long id, boolean skipChildren) throws DataAccessException {
		if (clazz == null) {
			throw new DataAccessException("SQLiteManager: Read: class is null");
		}
		if (mDb == null) {
			throw new DataAccessException("SQLiteManager: Read: Sqlite database is null");
		}
		if (!mDb.isOpen()) {
			throw new DataAccessException("SQLiteManager: Read: Sqlite database is not open");
		}

		TableQueryBuilder builder = mSchema.getQueryBuilder(clazz);
		if (builder == null) {
			throw new DataAccessException(String.format("SQLiteManager: Read: class %s is not registered", clazz.getName()));
		}

		T entity = null;
		try {
			Cursor c = mDb.rawQuery(builder.getSelectByIdQuery(), new String[]{Long.toString(id)});
			if (c != null) {
				try {
					if (c.moveToNext()) {
						entity = clazz.newInstance();
						populateStorage(entity, builder.tableData.fields, c);
                        if (EntityEvent.class.isAssignableFrom(entity.getClass())) {
                            ((EntityEvent) entity).onRead();
                        }

					} 
				} finally {
					c.close();
				}
			}
            if (entity != null && !skipChildren) {
                List<ChildTableData> children = mSchema.getChildren(clazz);
                if (children != null) {
                    Object[] parents = new Object[] {entity};
                    for (ChildTableData child: children) {
                        readChildren(id, clazz, parents, child);
                    }
                }
            }
        } catch (Exception e) {
			throw new DataAccessException("SQLiteManager: Read: Internal exception", e);
		}

		return entity;
	}

    void readChildren(long id, Class<?> idClazz, Object[] parents, ChildTableData childData) throws Exception {
        for (Class<?> childClazz: childData.children) {
            TableQueryBuilder.LinkedQueryBuilder builder = mSchema.getLinkedQueryBuilder(childClazz, idClazz);
            if (builder == null) {
                throw new DataIntegrityException(
                        String.format("SQLiteManager: readChildren: classes %s and %s are not related.",
                                idClazz.getName(), childClazz.getName()));
            }
            Object[] rows = new Object[256];
            int pos = 0;

            Cursor cc = mDb.rawQuery(builder.getSelectByIdQuery(), new String[]{Long.toString(id)});
            if (cc != null) {
                try {
                    while (cc.moveToNext()) {
                        if (pos > rows.length) {
                            rows = Arrays.copyOf(rows, rows.length + 256);
                        }
                        rows[pos] = builder.getTableData().tableClass.newInstance();
                        populateStorage(rows[pos], builder.getTableData().fields, cc);
                        pos++;
                    }
                } finally {
                    cc.close();
                }
            }

            if (pos > 0) {
                rows = Arrays.copyOf(rows, pos);

                ColumnAccessor childAccessor = builder.getTableData().foreignKey.valueAccessor;
                Arrays.sort(rows, new LongValueComparator(childAccessor));
                ColumnAccessor parentAccessor = builder.getParentTableData().primaryKey.valueAccessor;
                Arrays.sort(parents, new LongValueComparator(parentAccessor));

                int parentPos = 0;
                int childPos = 0;
                long parentId = (Long) parentAccessor.getValue(parents[parentPos]);
                while (childPos < rows.length) {
                    long childId = (Long) childAccessor.getValue(rows[childPos]);
                    if (parentId > childId) { // error
                        Log.e(TAG, "Datamanager.readChildren: Scope merge: algorithm error 1");
                    } else {
                        while (parentId < childId) {
                            parentPos++;
                            if (parentPos >= parents.length) {
                                Log.e(TAG, "Datamanager.readChildren: Scope merge: algorithm error 2");
                                break;
                            }
                            parentId = (Long) parentAccessor.getValue(parents[parentPos]);
                        }
                        if (parentId == childId) {
                            childData.valueAccessor.appendChild(rows[childPos], parents[parentPos]);
                        }
                    }
                    childPos++;
                }

                List<ChildTableData> children = mSchema.getChildren(childClazz);
                if (children != null) {
                    for (ChildTableData c: children) {
                        readChildren(id, idClazz, rows, c);
                    }
                }
            }
        }
    }

    static class GlobalId {
        public GlobalId(Class<?> tableClass, long recordId) {
            this.tableClass = tableClass;
            this.recordId = recordId;
        }
        Class<?> tableClass;
        long recordId;
    }

    static Comparator<GlobalId> sGlobalIdComparator = new Comparator<GlobalId>() {
        @Override
        public int compare(GlobalId obj1, GlobalId obj2) {
            if (obj1.tableClass == obj2.tableClass) {
                if (obj1.recordId == obj2.recordId) {
                    return 0;
                }
                else if (obj1.recordId < obj2.recordId) {
                    return -1;
                } else {
                    return 1;
                }

            }
            else if (obj1.tableClass.hashCode() < obj2.tableClass.hashCode()) {
                return -1;
            } else {
                return -1;
            }
        }
    };

	private long write(Object scope, long parentId, boolean withChildren) throws Exception {
		Class<?> clazz = scope.getClass();
		TableQueryBuilder builder = mSchema.getQueryBuilder(clazz);
		if (builder == null) {
			throw new DataAccessException(String.format("SQLiteManager: Write: class %s is not registered", clazz.getName()));
		}
		TableData tableData = builder.tableData;

		boolean isInsert;
		long id;
		{
            id = (Long) tableData.primaryKey.valueAccessor.getValue(scope);
            if (tableData.foreignKey != null) {
                tableData.foreignKey.valueAccessor.setValue(parentId, scope);
            }

            isInsert = id == 0;
            SQLiteStatement stmt = null;
            int stmtKey = 0;
            try {
                if (prepareStatements) {
                    stmtKey = tableData.tableNo + (isInsert ? 1000 : 2000);
                    synchronized (preparedStmts) {
                        stmt = preparedStmts.remove(stmtKey);
                    }
                }
                if (isInsert) {
                    if (stmt == null) {
                        String strStmt = builder.getInsertClause();
                        stmt = mDb.compileStatement(strStmt);
                    }
                    bindValues(scope, tableData, stmt);

                    id = stmt.executeInsert();

                    if (id == -1) {
                        throw new DataAccessException(String.format("SQLiteManager: Insert: constraint violation on table %s", tableData.tableName));
                    }
                    tableData.primaryKey.valueAccessor.setValue(id, scope);
                } else {
                    if (stmt == null) {
                        String strStmt = builder.getUpdateClause();
                        stmt = mDb.compileStatement(strStmt);
                    }
                    bindValues(scope, tableData, stmt);
                    stmt.bindLong(tableData.fields.length, id);

                    int affected = stmt.executeUpdateDelete();
                    if (affected != 1) {
                        throw new DataAccessException(String.format("SQLiteManager: UPDATE: constraint violation on table %s", tableData.tableName));
                    }
                }
                if (prepareStatements) {
                    synchronized (preparedStmts) {
                        if (preparedStmts.containsKey(stmtKey)) {
                            stmt.close();
                        } else {
                            preparedStmts.put(stmtKey, stmt);
                        }
                    }
                } else {
                    stmt.close();
                }
                stmt = null;
            }
            finally {
                if (stmt != null) {
                    stmt.close();
                }
            }
		}

		if (withChildren) {
			List<ChildTableData> children = mSchema.getChildren(clazz);
            if (children != null) {
                for (ChildTableData child: children) {
                    Set<GlobalId> globalIds = new TreeSet<>(sGlobalIdComparator);
                    if (!isInsert) {
                        for (Class<?> childClass: child.children) {
                            TableQueryBuilder.LinkedQueryBuilder childBuilder = mSchema.getLinkedQueryBuilder(childClass, clazz);
                            Cursor cc = mDb.rawQuery(childBuilder.getSelectIdByLinkedIdQuery(), new String[]{Long.toString(id)});
                            if (cc != null) {
                                try {
                                    while (cc.moveToNext()) {
                                        globalIds.add(new GlobalId(childClass, cc.getLong(0)));
                                    }
                                } finally {
                                    cc.close();
                                }
                            }
                        }
                    }

                    Object childObject = child.valueAccessor.getChildren(scope);
                    if (childObject != null) {
                        switch(child.linkType) {
                            case SINGLE: {
                                long childId = write(childObject, id, true);
                                globalIds.remove(new GlobalId(childObject.getClass(), childId));
                            }
                            break;

                            case LIST: {
                                List<?> list = (List<?>) childObject;
                                for (Object lo: list) {
                                    long childId = write(lo, id, true);
                                    globalIds.remove(new GlobalId(lo.getClass(), childId));
                                }
                            }
                            break;

                            case SET: {
                                Set<?> set = (Set<?>) childObject;
                                for (Object so: set) {
                                    long childId = write(so, id, true);
                                    globalIds.remove(new GlobalId(so.getClass(), childId));
                                }
                            }
                            break;
                        }
                    }
                    if (!isInsert) {
                        for (GlobalId gId: globalIds) {
                            delete(gId.tableClass, gId.recordId);
                        }
                    }
                }
            }
		}
		
		return id;
	}


	static void populateStorage(Object storage, FieldData[] fields, Cursor from) throws Exception {
		if (storage == null || fields == null || from == null) return;

		for (int i = 0; i < fields.length; ++i) {
			FieldData field = fields[i];
			Object value;
			switch (field.dataType) {
			case BOOLEAN: {
				boolean b = from.isNull(i) ? false : from.getInt(i) != 0;
				value = b;
			}
			break;

			case INT: {
				int ii = from.isNull(i) ? 0 : from.getInt(i);
				value = ii;
			}
			break;

			case LONG: {
				long l = from.isNull(i) ? 0L : from.getLong(i);
				value = l;
			}
			break;

			case DOUBLE: {
				double d = from.isNull(i) ? 0.0 : from.getDouble(i);
				value = d;
			}
			break;

			case STRING: {
				String s = from.isNull(i) ? null : from.getString(i);
				value = s;
			}
			break;

			case DATE: {
				Date dt = null;
				if (!from.isNull(i)) {
					long l = from.getLong(i);
					if (l > 0) {
						dt = new Date(l);
					}
				}
				value = dt;
			}
			break;

			case BYTE_ARRAY: {
				byte[] ba = from.isNull(i) ? null : from.getBlob(i);
				value = ba;
			}
			break;

			default:
				Log.w(TAG, String.format("Unsupported data type: \"%s\"", field.dataType.toString()));
				continue;
			}
			field.valueAccessor.setValue(value, storage);
		}
	}

    static void bindValues(Object storage, TableData table, SQLiteStatement stmt) throws Exception {
        if (storage == null || stmt == null) return;
        int paramNo = 1;
        for (FieldData field: table.fields) {
            if (field == table.primaryKey) {
                continue;
            }
            Object o = field.valueAccessor.getValue(storage);
            if (o != null) {
                switch (field.dataType) {
                    case BOOLEAN:
                        boolean b = (Boolean)o ;
                        stmt.bindLong(paramNo, b ? 1 : 0);
                        break;

                    case INT:
                        int i = (Integer) o;
                        stmt.bindLong(paramNo, i);
                        break;

                    case LONG:
                        long l = (Long) o;
                        stmt.bindLong(paramNo, l);
                        break;

                    case DOUBLE:
                        double d = (Double) o;
                        stmt.bindDouble(paramNo, d);
                        break;

                    case STRING:
                        stmt.bindString(paramNo, (String) o);
                        break;

                    case DATE: {
                        Date dt = (Date) o;
                        long ts = dt.getTime();
                        stmt.bindLong(paramNo, ts);
                    }
                    break;

                    case BYTE_ARRAY:
                        byte[] ba = (byte[]) o;
                        stmt.bindBlob(paramNo, ba);
                        break;

                    default:
                        Log.w(TAG, String.format("Unsupported data type: \"%s\"", field.dataType.toString()));
                        break;
                }
            } else {
                stmt.bindNull(paramNo);
            }
            paramNo += 1;
        }
    }

    static int mergeIds(long[] ids, int pos) {
        if (pos > 1) {
            Arrays.sort(ids, 0, pos);
            int newPos = 1;
            for (int i = newPos; i < pos; ++i) {
                if (ids[i] != ids[newPos - 1]) {
                    if (i != newPos) {
                        ids[newPos] = ids[i];
                    }
                    ++newPos;
                }
            }
            return newPos;
        } else {
            return pos;
        }
    }

    private static final String TAG = "GORA";
}
