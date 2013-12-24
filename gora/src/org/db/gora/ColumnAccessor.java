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

package org.db.gora;

/**
 * Defines the methods used by column value accessor
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

public interface ColumnAccessor {
    /**
     * Returns column value
     *
     * @param storage Java class mapped to SQL table
     * @return
     * @throws Exception
     */
	Object getValue(Object storage) throws Exception;

    /**
     * Sets column value
     * @param value column value
     * @param storage Java class mapped to SQL table
     * @throws Exception
     */
	void setValue(Object value, Object storage) throws Exception;

    /**
     * Not sure it's needed
     *
     * @return storage/table class
     */
	Class<?> getStorageClass();
}
