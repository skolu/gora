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

/**
 * Simple method child accessor. See {@link org.db.gora.ChildAccessor}
 * <p> 1-to-1 relationship
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

final public class SimpleMethodChildAccessor implements ChildAccessor {
    final Method mGetter;
    final Method mSetter;

    public SimpleMethodChildAccessor(Method getter, Method setter) {
        this.mGetter = getter;
        this.mSetter = setter;
    }

    @Override
    public void appendChild(Object child, Object storage) throws InvocationTargetException, IllegalAccessException {
        mSetter.invoke(storage, child);
    }

    @Override
    public Object getChildren(Object storage) throws InvocationTargetException, IllegalAccessException {
        return mGetter.invoke(storage);
    }
}

