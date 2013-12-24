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
 * Defines the methods used by child accessor
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

public interface ChildAccessor {
    /**
     * Appends an instance of child object to parent
     *
     * @param child  the instance of child table/class
     * @param storage the instance of parent table/class
     * @throws Exception
     */
	void appendChild(Object child, Object storage) throws Exception;

    /**
     * Returns a child object storage
     * <p>Container for 1-to-many relationship. A child object for 1to-1 relationship
     *
     * @param storage the instance of parent table/class
     * @return child container/object
     * @throws Exception
     */
	Object getChildren(Object storage) throws Exception;


    /**
     * Not sure it's needed
     *
     * @return storage/table class
     */
	Class<?> getStorageClass();
}
