package org.db.gora;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SqliteManager implements DataManager {
	final SQLiteDatabase mDb;
	final SqliteSchema mSchema;
	final ConcurrentLinkedQueue<ContentValues> mValues;

	public SqliteManager(SQLiteDatabase db, SqliteSchema schema) {
		mDb = db;
		mSchema = schema;
		mValues = new ConcurrentLinkedQueue<ContentValues>();
	}

    public <T> PredicateBuilder getPredicateBuilder(Class<T> clazz) throws DataAccessException {
        if (clazz == null) {
            throw new DataAccessException("SqliteManager: Write: Null class");
        }
        TableData tableData = mSchema.getTableData(clazz);
        if (tableData == null) {
            throw new DataAccessException(String.format("SqliteManager: Class %s is not registered", clazz.getName()));
        }
        return new PredicateBuilder(tableData);
    }

    public <T> long[] queryIds(Class<T> clazz, String where, String[] whereArgs) throws DataAccessException {
        if (clazz == null) {
            throw new DataAccessException("SqliteManager: QueryIds: Null class");
        }
        if (mDb == null) {
            throw new DataAccessException("SqliteManager: QueryIds: Sqlite database is null");
        }
        if (!mDb.isOpen()) {
            throw new DataAccessException("SqliteManager: QueryIds: Sqlite database is not open");
        }

        TableData tableData = mSchema.getTableData(clazz);
        if (tableData == null) {
            throw new DataAccessException(String.format("SqliteManager: QueryIds: class %s is not registered", clazz.getName()));
        }

        if (where == null) {
            where = "1";
        } else if (where.length() == 0) {
            where = "1";
        }

        String query = String.format("SELECT %s FROM %s WHERE %s", tableData.primaryKey.columnName, tableData.tableName, where);
        Cursor cursor = mDb.rawQuery(query, whereArgs);
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


    /**
     * Retrieves entities according to where clause
     */
    public <T> ClosableIterator<T> query (Class<T> clazz, String where, String[] whereArgs) throws DataAccessException {
        if (clazz == null) {
            throw new DataAccessException("SqliteManager: Query: Null class");
        }
        if (mDb == null) {
            throw new DataAccessException("SqliteManager: Query: Sqlite database is null");
        }
        if (!mDb.isOpen()) {
            throw new DataAccessException("SqliteManager: Query: Sqlite database is not open");
        }

        TableQueryBuilder builder = mSchema.getQueryBuilder(clazz);
        if (builder == null) {
            throw new DataAccessException(String.format("SqliteManager: Query: class %s is not registered", clazz.getName()));
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
                            for (ChildTableData child: children) {
                                Object[] childRows = readChildren(id, tableData.tableClass, child.childClass);
                                if (childRows != null) {
                                    for (Object row: childRows) {
                                        child.valueAccessor.appendChild(row, entity);
                                    }
                                }
                            }
                        }

                        if (SqliteEvent.class.isAssignableFrom(entity.getClass())) {
                            ((SqliteEvent) entity).onRead();
                        }

                        return entity;

                    } catch (Exception e) {
                        Log.e(Settings.TAG, "SqlManager: query", e);
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
			throw new DataAccessException("SqliteManager: Write: Null object");
		}
		if (mDb == null) {
			throw new DataAccessException("SqliteManager: Write: Sqlite database is null");
		}
		if (!mDb.isOpen()) {
			throw new DataAccessException("SqliteManager: Write: Sqlite database is not open");
		}
		if (mDb.isReadOnly()) {
			throw new DataAccessException("SqliteManager: Write: Sqlite database is read-only");
		}

        if (SqliteEvent.class.isAssignableFrom(entity.getClass())) {
            if (!((SqliteEvent) entity).onWrite()) {
                return false;
            }
        }
		
		mDb.beginTransactionNonExclusive();
		try {
			write(entity, 0, true);
			mDb.setTransactionSuccessful();
            return true;
		} catch (Exception e) {
			throw new DataAccessException("SqliteManager: Write: Internal exception", e);
		} finally {
			mDb.endTransaction();
		}
	}

    /**
	 * Deletes an entity
	 */
	@Override
	public <T> void delete(Class<T> clazz, long id) throws DataAccessException {
		if (clazz == null) {
			throw new DataAccessException("SqliteManager: Read: class is null");
		}
		if (mDb == null) {
			throw new DataAccessException("SqliteManager: Read: Sqlite database is null");
		}
		if (!mDb.isOpen()) {
			throw new DataAccessException("SqliteManager: Read: Sqlite database is not open");
		}
		if (mDb.isReadOnly()) {
			throw new DataAccessException("SqliteManager: Read: Sqlite database is read-only");
		}

		mDb.beginTransactionNonExclusive();
		try {
			deleteChildren(id, clazz, clazz);
			mDb.setTransactionSuccessful();
		} catch (Exception e) {
			throw new DataAccessException("SqliteManager: Delete: Internal exception", e); 
		} finally {
			mDb.endTransaction();
		}
	}

	private void deleteChildren(long id, Class<?> idClazz, Class<?> toDelete) throws DataIntegrityException {
		TableQueryBuilder.LinkedQueryBuilder builder = mSchema.getLinkedQueryBuilder(toDelete, idClazz);
		if (builder == null) {
            throw new DataIntegrityException(
                    String.format("SqliteManager: deleteChildren: classes %s and %s are unrelated.", idClazz.getName(), toDelete.getName()));
		}
		List<ChildTableData> children = mSchema.getChildren(toDelete);
		if (children != null) {
			for (ChildTableData child: children) {
				deleteChildren(id, idClazz, child.childClass);
			}
		}
		mDb.delete(builder.getTableData().tableName, builder.getDeleteByIdWhereClause(), new String[] {Long.toString(id)});
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
			throw new DataAccessException("SqliteManager: Read: class is null");
		}
		if (mDb == null) {
			throw new DataAccessException("SqliteManager: Read: Sqlite database is null");
		}
		if (!mDb.isOpen()) {
			throw new DataAccessException("SqliteManager: Read: Sqlite database is not open");
		}

		TableQueryBuilder builder = mSchema.getQueryBuilder(clazz);
		if (builder == null) {
			throw new DataAccessException(String.format("SqliteManager: Read: class %s is not registered", clazz.getName()));
		}

		T entity = null;
		try {
			Cursor c = mDb.rawQuery(builder.getSelectByIdQuery(), new String[]{Long.toString(id)});
			if (c != null) {
				try {
					if (c.moveToNext()) {
						entity = clazz.newInstance();
						populateStorage(entity, builder.tableData.fields, c);
                        if (SqliteEvent.class.isAssignableFrom(entity.getClass())) {
                            ((SqliteEvent) entity).onRead();
                        }

					} 
				} finally {
					c.close();
				}
			}
			if (entity != null && !skipChildren) {
				List<ChildTableData> children = mSchema.getChildren(clazz);
				if (children != null) {
					for (ChildTableData child: children) {
						Object[] childRows = readChildren(id, clazz, child.childClass);
						if (childRows != null) {
							for (Object row: childRows) {
								child.valueAccessor.appendChild(row, entity);
							}
						}
					}
				}
			} 
		} catch (Exception e) {
			throw new DataAccessException("SqliteManager: Read: Internal exception", e);
		}

		return entity;
	}

	private Object[] readChildren(long id, Class<?> idClazz, Class<?> childClazz) throws Exception 
	{
		TableQueryBuilder.LinkedQueryBuilder builder = mSchema.getLinkedQueryBuilder(childClazz, idClazz);
		if (builder == null) {
			throw new DataIntegrityException(
					String.format("SqliteManager: readChildren: classes %s and %s are unrelated.", idClazz.getName(), childClazz.getName()));
		}

		Object[] rows = new Object[256];
		int childCount = 0;

		Cursor cc = mDb.rawQuery(builder.getSelectByIdQuery(), new String[]{Long.toString(id)});
		if (cc != null) {
			try {
				while (cc.moveToNext()) {
					if (childCount > rows.length) {
						Object[] newArray = new Object[rows.length + 256];
						System.arraycopy(rows, 0, newArray, 0, rows.length);
						rows = newArray;
					}
					Object chobj = builder.getTableData().tableClass.newInstance();
					populateStorage(chobj, builder.getTableData().fields, cc);

                    rows[childCount] = chobj;
					childCount++;
				}
			} finally {
				cc.close();
			}
		}
		if (childCount == 0) return null;

		Object[] result = new Object[childCount];
		System.arraycopy(rows, 0, result, 0, childCount);

		List<ChildTableData> children = mSchema.getChildren(childClazz);

		if (children != null) {
			ValueAccess valueAccessor = builder.getTableData().primaryKey.valueAccessor;
			Arrays.sort(result, new LongValueComparator(valueAccessor));
			for (ChildTableData childSchema: children) {
				Object[] childRows = readChildren(id, idClazz, childSchema.childClass);
				if (childRows != null) {
					ValueAccess childAccessor = childSchema.foreignKeyField.valueAccessor;
					if (childRows.length > 1) {
						Arrays.sort(childRows, new LongValueComparator(childAccessor));
					}
					int resultIdx = 0;
					int childIdx = 0;
					long resultId = (Long) valueAccessor.getValue(result[resultIdx]);
					while (childIdx < childRows.length) {
						long childId = (Long) childAccessor.getValue(childRows[childIdx]);
						if (resultId > childId) { // error
							Log.e(Settings.TAG, "Datamanager.readChildren: Scope merge: algorithm error 1");
						} else {
							while (resultId < childId) {
								resultIdx++;
								if (resultIdx >= result.length) break;
								resultId = (Long) valueAccessor.getValue(result[resultIdx]);
							}
							if (resultId == childId) {
								childSchema.valueAccessor.appendChild(childRows[childIdx], result[resultIdx]);
							}
						}
						if (resultIdx >= result.length) break;
						childIdx++;
					}

					if (childIdx < childRows.length) {
						Log.e(Settings.TAG, "Datamanager.readChildren: Scope merge: algorithm error 2");
					} 
				}
			}
		}

		return result;
	}

	private void clearIdInArray(long id, long[] array) {
		if (array != null) {
			for (int i = 0; i < array.length; ++i) {
				if (array[i] == id) {
					array[i] = 0;
					break;
				}
			}
		}
	}
	
	private long write(Object scope, long parentId, boolean withChildren) throws Exception {
		Class<?> clazz = scope.getClass();
		TableQueryBuilder builder = mSchema.getQueryBuilder(clazz);
		if (builder == null) {
			throw new DataAccessException(String.format("SqliteManager: Write: class %s is not registered", clazz.getName()));
		}
		TableData tableData = builder.tableData;

		boolean isInsert;
		long id;
		{
			ContentValues values = mValues.poll();
			if (values == null) {
				values = new ContentValues();
			}
			populateValues(scope, tableData.fields, values);
			if (tableData.foreignKey != null) {
                Long aLong = values.getAsLong(tableData.foreignKey.columnName);
				long fkId = aLong != null ? aLong : 0L;
				if (fkId != parentId) {
					values.put(tableData.foreignKey.columnName, parentId);
					tableData.foreignKey.valueAccessor.setValue(parentId, scope);
				}
			}
            Long aLong = values.getAsLong(tableData.primaryKey.columnName);
			id = aLong != null ? aLong : 0L;
			values.remove(tableData.primaryKey.columnName);
			isInsert = id == 0;
			if (isInsert) { 
				id = mDb.insert(tableData.tableName, null, values);
				tableData.primaryKey.valueAccessor.setValue(id, scope);
			} else {
				mDb.update(tableData.tableName, values, builder.getUpdateWhereClause(), new String[] {Long.toString(id)});
			}
			
			mValues.add(values);
		}
		
		
		if (withChildren) {
			List<ChildTableData> children = mSchema.getChildren(clazz);
            if (children != null) {
                for (ChildTableData child: children) {
                    long[] ids = null;
                    if (!isInsert) {
                        TableQueryBuilder.LinkedQueryBuilder childBuilder = mSchema.getLinkedQueryBuilder(child.childClass, clazz);
                        Cursor cc = mDb.rawQuery(childBuilder.getSelectIdByLinkedIdQuery(), new String[]{Long.toString(id)});
                        if (cc != null) {
                            ids = new long[256];
                            int pos = 0;
                            try {
                                while (cc.moveToNext()) {
                                    ids[pos] = cc.getLong(0);
                                    ++pos;
                                    if (pos >= ids.length) {
                                        ids = Arrays.copyOf(ids, ids.length + 256);
                                    }
                                }
                                long[] newIds = new long[pos];
                                System.arraycopy(ids, 0, newIds, 0, pos);
                                ids = newIds;
                            } finally {
                                cc.close();
                            }
                        }
                    }

                    Object childObject = child.valueAccessor.getChildren(scope);
                    if (childObject != null) {
                        switch(child.linkType) {
                            case SINGLE: {
                                long childId = write(childObject, id, true);
                                clearIdInArray(childId, ids);
                            }
                            break;

                            case LIST: {
                                List<?> list = (List<?>) childObject;
                                for (Object lo: list) {
                                    long childId = write(lo, id, true);
                                    clearIdInArray(childId, ids);
                                }
                            }
                            break;

                            case SET: {
                                Set<?> list = (Set<?>) childObject;
                                for (Object lo: list) {
                                    long childId = write(lo, id, true);
                                    clearIdInArray(childId, ids);
                                }
                            }
                            break;
                        }
                    }
                    if (!isInsert && ids != null) {
                        for (long chid: ids) {
                            if (chid != 0) {
                                delete(child.childClass, chid);
                            }
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

			case BYTE: {
				byte b = from.isNull(i) ? (byte) 0 : (byte) from.getInt(i);
				value = b;
			}
			break;

			case SHORT: {
				short s = from.isNull(i) ? (short) 0 : (short) from.getInt(i);
				value = s;
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

			case FLOAT: {
				float f = from.isNull(i) ? 0.0f : from.getFloat(i);
				value = f;
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

			case BYTEARRAY: {
				byte[] ba = from.isNull(i) ? null : from.getBlob(i);
				value = ba;
			}
			break;

			default:
				Log.w(Settings.TAG, String.format("Unsupported data type: \"%s\"", field.dataType.toString()));
				continue;
			}
			field.valueAccessor.setValue(value, storage);
		}
	}
	
    static void populateValues(Object storage, FieldData[] fields, ContentValues values) throws Exception {
        if (storage == null || values == null) return;
        values.clear();

        for (FieldData field: fields) {
            switch (field.dataType) {
                case BOOLEAN: 
                	values.put(field.columnName, (Boolean) field.valueAccessor.getValue(storage));
                	break;

                case BYTE:
                	values.put(field.columnName, (Byte) field.valueAccessor.getValue(storage));
                    break;

                case SHORT: 
                	values.put(field.columnName, (Short) field.valueAccessor.getValue(storage));
                break;

                case INT:
                	values.put(field.columnName, (Integer) field.valueAccessor.getValue(storage));
                    break;

                case LONG:
                	values.put(field.columnName, (Long) field.valueAccessor.getValue(storage));
                    break;

                case FLOAT:
                	values.put(field.columnName, (Float)field.valueAccessor.getValue(storage));
                    break;

                case DOUBLE:
                	values.put(field.columnName, (Double) field.valueAccessor.getValue(storage));
                break;

                case STRING:
                	values.put(field.columnName, (String) field.valueAccessor.getValue(storage));
                    break;

                case DATE: {
                    Date dt = (Date) field.valueAccessor.getValue(storage);
                    long l = dt != null ? dt.getTime() : 0L;
                    values.put(field.columnName, l);
                }
                break;

                case BYTEARRAY: 
                	values.put(field.columnName, (byte[]) field.valueAccessor.getValue(storage));
                    break;

                default:
                    Log.w(Settings.TAG, String.format("Unsupported data type: \"%s\"", field.dataType.toString()));
                    break;
            }
        }
    }
}
