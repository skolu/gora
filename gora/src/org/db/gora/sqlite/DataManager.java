package org.db.gora.sqlite;

public interface DataManager {
    <T> T read(Class<T> clazz, long id) throws DataAccessException;
    <T> void delete(Class<T> clazz, long id) throws DataAccessException;
    <T> void write(T entity) throws DataAccessException;
}
