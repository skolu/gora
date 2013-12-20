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

//TODO 1.Full text search, 2. Transaction and bulk operation
/**
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 * */

public interface DataManager {
    long[] queryIds(Class<?> clazz, String where, String[] whereArgs, String orderBy) throws DataAccessException;

    <T> ClosableIterator<T> query(Class<T> clazz, String where, String[] whereArgs) throws DataAccessException;
    <T> T read(Class<T> clazz, long id) throws DataAccessException;
    <T> boolean write(T entity) throws DataAccessException;
    void delete(Class<?> clazz, long id) throws DataAccessException;

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
