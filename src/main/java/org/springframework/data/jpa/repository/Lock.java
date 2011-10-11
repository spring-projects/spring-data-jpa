package org.springframework.data.jpa.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.persistence.LockModeType;

/**
 * Annotation used to specify the {@link LockModeType} to be used when executing the query. It will be evaluated when
 * using {@link Query} on a query method or if you derive the query from the method name.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lock {
	LockModeType value();
}
