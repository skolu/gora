package org.db.gora;

import java.util.Iterator;

public interface DataManager {
//    <E, T> ClosableIterator<E> readLinks(Class<E> entityClass, Class<T> clazz, long id);
    <T> T read(Class<T> clazz, long id) throws DataAccessException;
    <T> void delete(Class<T> clazz, long id) throws DataAccessException;
    <T> void write(T entity) throws DataAccessException;
}
