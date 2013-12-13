package org.db.gora;

public interface DataManager {
    <T> long[] queryIds(Class<T> clazz, String where, String[] whereArgs, String orderBy) throws DataAccessException;

    public static interface FieldCursor {
        long getId() throws DataAccessException;
        Object getFieldValue(int fieldNo) throws DataAccessException;
        FieldDataType getFieldType(int fieldNo);
        boolean eof();
        boolean next();
        void close();
    }
    <T> FieldCursor queryFields(Class<T> clazz, String where, String[] whereArgs, String ...fields) throws DataAccessException, DataIntegrityException;

    <T> ClosableIterator<T> query(Class<T> clazz, String where, String[] whereArgs) throws DataAccessException;
    <T> T read(Class<T> clazz, long id) throws DataAccessException;
    <T> void delete(Class<T> clazz, long id) throws DataAccessException;
    <T> boolean write(T entity) throws DataAccessException;
}
