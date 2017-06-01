package org.hippoecm.hst.pagecomposer.jaxrs.api.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * If added to a method, then the method is allowed when the hst configuration is locked, regardless whether the
 * method is PUT, DELETE or POST
 */
@Documented
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface IgnoreLock {
}
