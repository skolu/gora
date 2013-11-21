package org.db.gora.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
    
    boolean pk() default false;
    boolean fk() default false;

    String getter() default "";

    String setter() default "";
}
