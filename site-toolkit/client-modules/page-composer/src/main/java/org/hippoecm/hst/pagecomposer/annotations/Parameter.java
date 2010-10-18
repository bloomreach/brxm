package org.hippoecm.hst.pagecomposer.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Parameter {
    String name();

    ParameterType type() default ParameterType.STRING;

    boolean required() default false;

    String defaultValue() default "";

    String label() default ""; 

    String description() default "";

    
}
