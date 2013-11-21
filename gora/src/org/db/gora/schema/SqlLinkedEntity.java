package org.db.gora.schema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SqlLinkedEntity {
    Class<?> clazz();
    WhenLinkBroken whenBroken() default WhenLinkBroken.NONE;
}
