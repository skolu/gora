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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Integer property value accessor.
 * <p>Requires both getter and setter
 *
 * Supports the following Java types: Integer, Short, Byte
 * See {@link org.db.gora.ColumnAccessor}
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

final public class IntPropertyAccessor implements ColumnAccessor {

    final Method mGetter;
    final Method mSetter;
    final IntColumnClass mJavaClass;

    public IntPropertyAccessor(Method getter, Method setter) {
        this.mGetter = getter;
        this.mSetter = setter;
        mJavaClass = IntColumnClass.resolveType(getter.getReturnType());
    }

    @Override
    public Integer getValue(Object storage) throws InvocationTargetException, IllegalAccessException {
        Object value = mGetter.invoke(storage);
        if (value != null) {
            switch (mJavaClass) {
                case Integer:
                    return (Integer) value;

                case Short:
                    return (int) (short) (Short) value;

                case Byte:
                    return (int) (byte) (Byte) value;
            }
        }
        return 0;
    }

    @Override
    public void setValue(Object value, Object storage) throws InvocationTargetException, IllegalAccessException {
        if (value != null) {
            switch (mJavaClass) {
                case Integer:
                    break;

                case Short:
                    value = Short.valueOf((short) ((Integer) value & 0xffff));
                    break;

                case Byte:
                    value = Byte.valueOf((byte) ((Integer) value & 0xff));
                    break;
            }
        }
        mSetter.invoke(storage, value);
    }

    @Override
    public Class<?> getStorageClass() {
        return mGetter.getDeclaringClass();
    }
}
