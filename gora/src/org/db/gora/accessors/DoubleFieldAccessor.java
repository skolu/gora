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
import java.math.BigDecimal;

/**
 * Double field value accessor.
 * Supports the following Java classes: Double, Float, BigDecimal
 * See {@link org.db.gora.ColumnAccessor}
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

final public class DoubleFieldAccessor implements ColumnAccessor {

    final Field mField;
    final DoubleColumnClass mJavaClass;

    public DoubleFieldAccessor(Field field) {
        this.mField = field;
        mJavaClass = DoubleColumnClass.resolveType(field.getType());
    }

    @Override
    public Double getValue(Object storage) throws IllegalAccessException, DataAccessException {
        Object value = mField.get(storage);
        if (value == null) {
            return 0.0;
        }
        switch (mJavaClass) {
            case Double:
                return (Double) value;

            case Float:
                return (double) (float) (Float) value;

            case BigDecimal:
                return  ((BigDecimal) value).doubleValue();
        }
        throw new DataAccessException("Unsupported double type");
    }

    @Override
    public void setValue(Object value, Object storage) throws IllegalAccessException {
        if (value == null) {
            mField.set(storage, null);
        } else {
            switch (mJavaClass) {
                case Double:
                    break;

                case Float:
                    value = Float.valueOf((float) (double) (Double) value);
                    break;

                case BigDecimal:
                    value = BigDecimal.valueOf((Double) value);
                    break;
            }
        }
        mField.set(storage, value);
    }

    @Override
    public Class<?> getStorageClass() {
        return mField.getDeclaringClass();
    }
}
