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

import java.lang.reflect.Method;
import java.math.BigDecimal;

/**
 * Double property value accessor.
 * <p>Requires both getter and setter
 *
 * Supports the following Java classes: Double, Float, BigDecimal
 * See {@link org.db.gora.ColumnAccessor}
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

final public class DoublePropertyAccessor implements ColumnAccessor {
    final Method mGetter;
    final Method mSetter;
    final DoubleColumnClass mJavaClass;

    public DoublePropertyAccessor(Method getter, Method setter) {
        this.mGetter = getter;
        this.mSetter = setter;
        mJavaClass = DoubleColumnClass.resolveType(getter.getReturnType());
    }


    @Override
    public Double getValue(Object storage) throws Exception {
        Object value = mGetter.invoke(storage);
        if (value != null) {
            switch (mJavaClass) {
                case Double:
                    return (Double) value;

                case Float:
                    return (double) (float) (Float) value;

                case BigDecimal:
                    return ((BigDecimal) value).doubleValue();
            }
        }
        return 0.0;
    }

    @Override
    public void setValue(Object value, Object storage) throws Exception {
        if (value != null) {
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
        mSetter.invoke(storage, value);
    }
}
