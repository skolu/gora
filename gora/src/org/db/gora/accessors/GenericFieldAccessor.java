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

import java.lang.reflect.Field;

/**
 * Generic field value accessor. Does not do any type checks.
 * See {@link org.db.gora.ColumnAccessor}
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

final public class GenericFieldAccessor implements ColumnAccessor {
    final Field mField;

    public GenericFieldAccessor(Field field) {
  			this.mField = field;
  		}

  		@Override
  		public Object getValue(Object storage) throws IllegalAccessException {
  			return mField.get(storage);
  		}

  		@Override
  		public void setValue(Object value, Object storage) throws IllegalAccessException {
  			mField.set(storage, value);
  		}

  		@Override
  		public Class<?> getStorageClass() {
  			return mField.getDeclaringClass();
  		}
}
