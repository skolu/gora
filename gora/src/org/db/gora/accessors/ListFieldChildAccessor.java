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

import org.db.gora.ChildAccessor;

import java.lang.reflect.Field;
import java.security.InvalidParameterException;
import java.util.List;

/**
 * List field child accessor. See {@link org.db.gora.ChildAccessor}
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

final public class ListFieldChildAccessor implements ChildAccessor {

    final Field mField;
    public ListFieldChildAccessor(Field field) {
        this.mField = field;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public void appendChild(Object child, Object storage) throws IllegalAccessException, InstantiationException {
        if (child == null || storage == null) {
            throw new InvalidParameterException("appendChild: null parameter");
        }

        List list = getChildren(storage);
        if (list == null) {
            list = (List) mField.getType().newInstance();
            mField.set(storage, list);
        }
        list.add(child);
    }

    @Override
    public List getChildren(Object storage) throws IllegalAccessException {
        return (List) mField.get(storage);
    }

    @Override
    public Class<?> getStorageClass() {
        return mField.getDeclaringClass();
    }
}
