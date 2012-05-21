/*
 *  Copyright 2011 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.service.restproxy.test.annotations;

import java.beans.PropertyEditor;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that a getter method returns the value of an HST parameter.
 *
 * @see {@link ParametersInfo}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Parameter {
    
    /**
     * This is a special name that can be used for {@link Parameter#name()}. Typically, the CMS UI might also show this property differently
     * There is only one single @Parameter allowed in an interface with the name equal to "HIDE_NAME", thus for example:
     * 
     *  <blockquote>
     *  <pre>
     *   @Parameter(name = Parameter.HIDE_NAME, displayName = "Hide")
     *   public boolean isHidden();
     *   </pre>
     *   </blockquote>
     */
    static final String HIDE_NAME =  "org.hippoecm.hst.core.parameters.Parameter.hide"; 
    
    /**
     * @return the name of the parameter used, also see {@link org.hippoecm.hst.configuration.components.HstComponentConfiguration#getParameter(String)} . This element is mandatory.
     */
    String name();

    /**
     * @return <code>true</code> if this is a required parameter for the {@link org.hippoecm.hst.core.component.HstComponent} to work
     */
    boolean required() default false;

    /**
     * @return the default value of this parameter when it is not present in the backing {@link org.hippoecm.hst.configuration.components.HstComponentConfiguration#getParameters()}. If there is
     * no defaultValue defined, it is an empty String <code>""</code>
     */
    String defaultValue() default "";

    /**
     * @return the displayName of this parameter. This can be the 'pretty' name for {@link #name()}. If missing, implementations can do
     * a fallback to {@link #name()}
     */
    String displayName() default "";

    /**
     * @return the description for this {@link org.hippoecm.hst.core.parameters.Parameter}
     */
    String description() default "";

    /**
     * This is still experimental.
     * @return a PropertyEditor
     */
    Class<? extends PropertyEditor> customEditor() default EmptyPropertyEditor.class;

}
