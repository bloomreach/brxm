/**
 * Copyright 2013-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.container.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an annotated component can be ordered by other container component.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Orderable {

    /**
     * Returns the name of this component.
     * If not specified, then container may use the FQCN of role type (see {@link #role()} instead
     * for the orderable component name.
     * If neither name nor role are not specified, then the container may use the FQCN of the component class having
     * this annotation instead for the orderable component name.
     * @return the name of this component.
     * @return
     */
    String name() default "";

    /**
     * Returns the role as type of this component.
     * This role type can optionally be used by the container when the name (see {@link #name()}) is not specified.
     * Therefore, component developer may specify either name or role for the component name used by the container.
     * @return
     */
    Class<?> role() default Object.class;

    /**
     * Postrequisite component names that should follow this component.
     * This can have multiple component names, separated by ' ', ',', '\t', '\r' or '\n'
     * @return
     */
    String before() default "";

    /**
     * Prerequisite component names that should follow this component.
     * This can have multiple component names, separated by ' ', ',', '\t', '\r' or '\n'
     * @return
     */
    String after() default "";

}
