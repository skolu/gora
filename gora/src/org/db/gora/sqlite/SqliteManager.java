package org.db.gora.sqlite;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.db.gora.sqlite.SqliteSchema.TableRelation;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class SqliteManager implements DataManager {
	final SQLiteDatabase mDb;
	final SqliteSchema mSchema;

	public SqliteManager(SQLiteDatabase db, SqliteSchema schema) {
		mDb = db;
		mSchema = schema;
	}

	@Override
	public <T> boolean delete(Class<T> clazz, long id) throws DataAccessException {
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
		return true;
	}

	private void deleteChildren(long id, Class<?> idClazz, Class<?> toDelete) {
		TableQueryBuilder builder = mSchema.getRelationQueryBuilder(new TableRelation(idClazz, toDelete));
		if (builder == null) {
			String.format("SqliteManager: deleteChildren: classes %s and %s are unrelated.", idClazz.getName(), toDelete.getName());
		}
		List<TableLinkData> children = mSchema.getChildren(toDelete);
		if (children != null) {
			for (TableLinkData child: children) {
				deleteChildren(id, idClazz, child.childClass);
			}
		}
		mDb.delete(builder.tableData.tableName, builder.getDeleteByIdWhereClause(), new String[] {Long.toString(id)});
	}

	/**
	 * Reads an entity by id
	 * @throws DataAccessException 
	 */
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

		TableQueryBuilder builder = mSchema.getRelationQueryBuilder(new TableRelation(clazz, clazz));
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
					} 
				} finally {
					c.close();
				}
			}
			if (entity != null && !skipChildren) {
				List<TableLinkData> children = mSchema.getChildren(clazz);
				if (children != null) {
					for (TableLinkData child: children) {
						Object[] childRows = readChildren(id, clazz, child);
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

	private Object[] readChildren(long id, Class<?> clazz, TableLinkData childLink) throws Exception 
	{
		TableQueryBuilder builder = mSchema.getRelationQueryBuilder(new TableRelation(clazz, childLink.childClass));
		if (builder == null) {
			throw new DataIntegrityException(
					String.format("SqliteManager: readChildren: classes %s and %s are unrelated.", clazz.getName(), childLink.childClass.getName()));
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
					Object chobj = builder.tableData.tableClass.newInstance();
					populateStorage(chobj, builder.tableData.fields, cc);
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

		List<TableLinkData> children = mSchema.getChildren(childLink.childClass);

		if (children != null) {
			ValueAccess valueAccessor = builder.tableData.primaryKey.valueAccessor;
			Arrays.sort(result, new LongValueComparator(valueAccessor));
			for (TableLinkData childSchema: children) {
				Object[] childRows = readChildren(id, clazz, childSchema);
				if (childRows != null) {
					ValueAccess childAccessor = childSchema.foreignKeyField.valueAccessor;
					if (childRows.length > 1) {
						Arrays.sort(childRows, new LongValueComparator(childAccessor));
					}
					int resultIdx = 0;
					int childIdx = 0;
					long resultId = (long) (Long) valueAccessor.getValue(result[resultIdx]);
					while (childIdx < childRows.length) {
						long childId = (long) (Long) childAccessor.getValue(childRows[childIdx]);
						if (resultId > childId) { // error
							Log.e(Settings.TAG, "Datamanager.readChildren: Scope merge: algorithm error 1");
						} else {
							while (resultId < childId) {
								resultIdx++;
								if (resultIdx >= result.length) break;
								resultId = (long) (Long) valueAccessor.getValue(result[resultIdx]);
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

	@Override
	public <T> boolean write(T entity) {
		return false;
	}

	final static void populateStorage(Object storage, FieldData[] fields, Cursor from) throws Exception {
		if (storage == null || fields == null || from == null) return;

		for (int i = 0; i < fields.length; ++i) {
			FieldData field = fields[i];
			Object value = null;
			switch (field.dataType) {
			case BOOLEAN: {
				boolean b = from.isNull(i) ? false : from.getInt(i) != 0;
				value = Boolean.valueOf(b);
			}
			break;

			case BYTE: {
				byte b = from.isNull(i) ? (byte) 0 : (byte) from.getInt(i);
				value = Byte.valueOf(b);
			}
			break;

			case SHORT: {
				short s = from.isNull(i) ? (short) 0 : (short) from.getInt(i);
				value = Short.valueOf(s);
			}
			break;

			case INT: {
				int ii = from.isNull(i) ? 0 : from.getInt(i);
				value = Integer.valueOf(ii);
			}
			break;

			case LONG: {
				long l = from.isNull(i) ? 0L : from.getLong(i);
				value = Long.valueOf(l);
			}
			break;

			case FLOAT: {
				float f = from.isNull(i) ? 0.0f : from.getFloat(i);
				value = Float.valueOf(f);
			}
			break;

			case DOUBLE: {
				double d = from.isNull(i) ? 0.0 : from.getDouble(i);
				value = Double.valueOf(d);
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
	
    final static void populateValues(Object storage, FieldData[] fields, Object[] values) throws Exception {
        if (storage == null || values == null) return;

        for (int i = 0; i < fields.length; ++i) {
        	if (i >= values.length) break;
        	FieldData field = fields[i];
            switch (field.dataType) {
                case BOOLEAN: 
                	values[i] = (Boolean) field.valueAccessor.getValue(storage);
                	break;

                case BYTE:
                    values[i] = (Byte) field.valueAccessor.getValue(storage);
                    break;

                case SHORT: 
                    values[i] = (Short) field.valueAccessor.getValue(storage);
                break;

                case INT:
                    values[i] = (Integer) field.valueAccessor.getValue(storage);
                    break;

                case LONG:
                    values[i] = (Long) field.valueAccessor.getValue(storage);
                    break;

                case FLOAT:
                    values[i] = (Float)field.valueAccessor.getValue(storage);
                    break;

                case DOUBLE:
                    values[i] = (Double) field.valueAccessor.getValue(storage);
                break;

                case STRING:
                    values[i] = (String) field.valueAccessor.getValue(storage);
                    break;

                case DATE: {
                    Date dt = (Date) field.valueAccessor.getValue(storage);
                    long l = dt != null ? dt.getTime() : 0L;
                    values[i] = l;
                }
                break;

                case BYTEARRAY: 
                    values[i] = (byte[]) field.valueAccessor.getValue(storage);
                    break;

                default:
                    Log.w(Settings.TAG, String.format("Unsupported data type: \"%s\"", field.dataType.toString()));
                    break;
            }
        }
    }

}
