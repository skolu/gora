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

package org.db.gora.accessors;

import org.db.gora.ColumnAccessor;
import org.db.gora.DataAccessException;

import java.lang.reflect.Field;

/**
 * Integer field value accessor.
 * Supports the following Java classes: Integer, Short, Byte
 * See {@link org.db.gora.ColumnAccessor}
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

final public class IntFieldAccessor implements ColumnAccessor {

    final Field mField;
    final IntColumnClass mJavaClass;

    public IntFieldAccessor(Field field) {
        this.mField = field;
        mJavaClass = IntColumnClass.resolveType(field.getType());
    }

    @Override
    public Integer getValue(Object storage) throws IllegalAccessException, DataAccessException {
        Object value = mField.get(storage);
        if (value == null) {
            return 0;
        }

        switch (mJavaClass) {
            case Integer:
                return (Integer) value;

            case Short:
                return (int) (short) (Short) value;

            case Byte:
                return (int) (byte) (Byte) value;
        }
        throw new DataAccessException("Unsupported integer type");
    }

    @Override
    public void setValue(Object value, Object storage) throws IllegalAccessException {
        if (value == null) {
            value = 0;
        }
        switch (mJavaClass) {
            case Integer:
                break;

            case Short:
                value = (short) (((Integer) value) & 0xffff);
                break;

            case Byte:
                value = (byte) (((Integer) value) & 0xff);
                break;
        }

        mField.set(storage, value);
    }
}
