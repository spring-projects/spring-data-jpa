package org.springframework.data.jpa.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.persistence.QueryHint;


/**
 * Wrapper annotation to allow {@link QueryHint} annotations to be bound to
 * methods. It will be evaluated when using {@link Query} on a query method or
 * if you derive the query from the method name. If you rely on named queries
 * either use the XML or annotation based way to declare {@link QueryHint}s in
 * combination with the actual named query declaration.
 * 
 * @author Oliver Gierke
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryHints {

    QueryHint[] value() default {};
}
