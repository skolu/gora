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
 * Generic property value accessor. Does not do any type checks.
 * <p>Requires both getter and setter
 *
 * See {@link org.db.gora.ColumnAccessor}
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

final public class GenericPropertyAccessor implements ColumnAccessor {

    final Method mGetter;
  	final Method mSetter;

    public GenericPropertyAccessor(Method getter, Method setter) {
  			this.mGetter = getter;
  			this.mSetter = setter;
  		}

  		@Override
  		public Object getValue(Object storage) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
  			return mGetter.invoke(storage);
  		}

  		@Override
  		public void setValue(Object value, Object storage) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException  {
  			mSetter.invoke(storage, value);
  		}
}
