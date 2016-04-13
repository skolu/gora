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

/**
 * String property value accessor.
 * <p>Requires both getter and setter
 *
 * Supports the following Java classes: String, Enum
 * See {@link org.db.gora.ColumnAccessor}
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

final public class StringPropertyAccessor implements ColumnAccessor {

    final Method mGetter;
    final Method mSetter;
    final StringColumnClass mJavaClass;
    final Object[] mValues;

    public StringPropertyAccessor(Method getter, Method setter) {
        this.mGetter = getter;
        this.mSetter = setter;
        this.mJavaClass = mGetter.getReturnType().isEnum() ? StringColumnClass.Enum : StringColumnClass.String;
        if (mJavaClass == StringColumnClass.Enum) {
            mValues = mGetter.getReturnType().getEnumConstants();
        } else {
            mValues = null;
        }
    }

    @Override
    public String getValue(Object storage) throws Exception {
        Object value = mGetter.invoke(storage);
        if (value == null) {
            return null;
        }
        if (mJavaClass == StringColumnClass.Enum) {
            return ((Enum) value).name();
        }
        return (String) value;
    }

    @Override
    public void setValue(Object value, Object storage) throws Exception {
        if (value != null) {
            if (mJavaClass == StringColumnClass.Enum) {
                String name = (String) value;
                value = null;
                for (Object o: mValues) {
                    Enum e = (Enum) o;
                    if (name.equals(e.name())) {
                        value = e;
                        break;
                    }
                }
            }
        }

        mSetter.invoke(storage, value);
    }
}
