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
 * <p>Marks a field as a column in database, as used by {@link SchemaBuilder}.
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SqlColumn {
	/**
     * The name of the column in the database. If not set then the name is taken from the field name.
     */
    String name();

    /**
     * Whether the database column can have no value. Default is true. No checks are done at runtime.
     * Used in "CREATE TABLE" statement.
     */
    boolean nullable() default true;

    /**
     * Whether the database column is indexed.
     * Used in "CREATE INDEX" statement.
     */
    boolean index() default false;

    /**
     * Whether the database column is uniquely indexed.
     * Used in "CREATE UNIQUE INDEX" statement.
     */
    boolean unique() default false;

    /**
     * Whether the column is a primary key
     */
    boolean pk() default false;
    /**
     * Whether the column is a foregn key
     */
    boolean fk() default false;

    /**
     * Defines the getter method in case of the field is not public
     */
    String getter() default "";
    /**
     * Defines the setter method in case of the field is not public
     */
    String setter() default "";
}
