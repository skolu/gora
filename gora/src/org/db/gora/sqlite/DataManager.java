package org.db.gora.sqlite;

public interface DataManager {
    <T> T read(Class<T> clazz, long id) throws DataAccessException;

    <T> boolean delete(Class<T> clazz, long id) throws DataAccessException;
    <T> boolean write(T entity);
}
