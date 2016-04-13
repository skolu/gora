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
import java.util.Set;

/**
 * Set field child accessor. See {@link org.db.gora.ChildAccessor}
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

final public class SetFieldChildAccessor implements ChildAccessor {

    final Field mField;
    public SetFieldChildAccessor(Field field) {
        this.mField = field;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public void appendChild(Object child, Object storage) throws IllegalAccessException, InstantiationException {
        if (child == null || storage == null) {
            throw new InvalidParameterException("appendChild: null parameter");
        }

        Set set = getChildren(storage);
        if (set == null) {
            set = (Set) mField.getType().newInstance();
            mField.set(storage, set);
        }
        set.add(child);
    }

    @Override
    public Set getChildren(Object storage) throws IllegalAccessException {
        return (Set) mField.get(storage);
    }
}
