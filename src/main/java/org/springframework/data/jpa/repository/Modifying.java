package org.springframework.data.jpa.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Indicates a method should be regarded as modifying query.
 * 
 * @author Oliver Gierke
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Modifying {

    /**
     * Defines whether we should clear the underlying persistence context after
     * excuting the modifying query.
     * 
     * @return
     */
    boolean clearAutomatically() default true;
}
