/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.parameters;

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
     * @return <code>true</code> if the parameter should not be shown in the channel manager UI
     */
    boolean hideInChannelManager() default false;

    /**
     * This is still experimental.
     * @return a PropertyEditor
     */
    Class<? extends PropertyEditor> customEditor() default EmptyPropertyEditor.class;

}
