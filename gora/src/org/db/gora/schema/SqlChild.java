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

package org.db.gora.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marks a field as a storage for child rows, as used by {@link SchemaBuilder}.
 *
 * <p>Three kinds of storage are currently supported:
 * <ul>
 * <li>Simple: defines 1-to-1 relationship</li>
 * <li>List: defines 1-to-many relationship. The field class must implement List interface</li>
 * <li>Set: defines 1-to-many relationship. The field class must implement Set interface</li>
 * </ul>
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface SqlChild {

    /**
     * Array of classes that the child can accept.
     */
    Class<?>[] classes() default {};

    /**
     * Defines the getter method in case of the child storage field is not public
     */
    String getter() default "";

    /**
     * Defines the setter method in case of the child storage field is not public.
     */
    String setter() default "";
}
