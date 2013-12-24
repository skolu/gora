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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Set method child accessor. See {@link org.db.gora.ChildAccessor}
 * <p> The getter method is expected to create an instance of Set interface
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

public final class SetMethodChildAccessor implements ChildAccessor {
    final Method mGetter;

    public SetMethodChildAccessor(Method getter) {
        this.mGetter = getter;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public void appendChild(Object child, Object storage) throws InvocationTargetException, IllegalAccessException {
        Set set = getChildren(storage);
        if (set != null) {
            set.add(child);
        }
    }

    @Override
    public Set getChildren(Object storage) throws InvocationTargetException, IllegalAccessException {
        return (Set) mGetter.invoke(storage);
    }

    @Override
    public Class<?> getStorageClass() {
        return mGetter.getDeclaringClass();
    }
}
