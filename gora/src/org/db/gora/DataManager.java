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

/**
 * Defines the basic dataccess methods
 * See {@link org.db.gora.SQLiteManager}
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 *
 * */

public interface DataManager {
    /**
     * Queries record IDs according to Where and OrderBy clauses.
     *
     * @param clazz     Storage class registered with {@link org.db.gora.SQLiteSchema}
     * @param where     Where clause
     * @param whereArgs Where clause arguments
     * @param orderBy   OrberBy clause
     * @return          array of record IDs
     * @throws          DataAccessException
     */
    long[] queryIds(Class<?> clazz, String where, String[] whereArgs, String orderBy) throws DataAccessException;

    /**
     * Queries record IDs according to Where and OrderBy clauses.
     *
     * @param clazz     Storage class registered with {@link org.db.gora.SQLiteSchema}
     * @param where     Where clause
     * @param whereArgs Where clause arguments
     * @return          {@link org.db.gora.ClosableIterator} instance.
     * @throws          DataAccessException
     */
    <T> ClosableIterator<T> query(Class<T> clazz, String where, String[] whereArgs) throws DataAccessException;

    /**
     * Reads an object with children by ID
     *
     * @param clazz     Storage class registered with {@link org.db.gora.SQLiteSchema}
     * @param id        Record ID
     * @return          Instance of object or null
     * @throws          DataAccessException
     */
    <T> T read(Class<T> clazz, long id) throws DataAccessException;

    /**
     * Stores an object with children
     *
     * @param entity    Object to store
     * @return
     * @throws          DataAccessException
     */
    <T> boolean write(T entity) throws DataAccessException;

    /**
     * Deletes an object with children by ID
     *
     * @param clazz     Storage class registered with {@link org.db.gora.SQLiteSchema}
     * @param id        Record ID
     * @throws          DataAccessException
     */
    void delete(Class<?> clazz, long id) throws DataAccessException;

    /**
     * Queries record IDs of detail class that are linked to master record ID.
     *
     * @param detailClazz   Detail class
     * @param masterClazz   Master class
     * @param masterId      Master record ID
     * @return              array of record IDs
     * @throws              DataAccessException
     * @throws              DataIntegrityException if master and detail classes have no links
     */
    long[] queryLinks(Class<?> detailClazz, Class<?> masterClazz, long masterId) throws DataAccessException, DataIntegrityException;


    public static interface FieldCursor {
        long getId() throws DataAccessException;
        Object getFieldValue(int fieldNo) throws DataAccessException;
        FieldDataType getFieldType(int fieldNo);
        boolean eof();
        boolean next();
        void close();
    }
    FieldCursor queryFields(Class<?> clazz, String where, String[] whereArgs, String ...fields) throws DataAccessException, DataIntegrityException;
}
