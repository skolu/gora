package org.db.gora;

public interface DataManager {
    <T> long[] queryIds(Class<T> clazz, String where, String[] whereArgs) throws DataAccessException;
    <T> ClosableIterator<T> query(Class<T> clazz, String where, String[] whereArgs) throws DataAccessException;
    <T> T read(Class<T> clazz, long id) throws DataAccessException;
    <T> void delete(Class<T> clazz, long id) throws DataAccessException;
    <T> void write(T entity) throws DataAccessException;
}
