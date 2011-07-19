/*
 *  Copyright 2010 - 2011 Hippo.
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
package org.hippoecm.hst.configuration.components;

import java.beans.PropertyEditor;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hippoecm.hst.core.component.HstComponent;


/**
 * Annotation indicating that a getter method returns the value of an HST component parameter.
 *
 * @see {@link ParametersInfo}
 * @deprecated Since HST version 2.23.02 replaced by {@link org.hippoecm.hst.core.parameters.Parameter}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Deprecated
public @interface Parameter {
    
    /**
     * @return the name of the parameter used, also see {@link HstComponentConfiguration#getParameter(String)} . This element is mandatory. 
     */
    String name();
    
    /**
     * @return <code>true</code> if this is a required parameter for the {@link HstComponent} to work
     */
    boolean required() default false;
    
    /**
     * @return the default value of this parameter when it is not present in the backing {@link HstComponentConfiguration#getParameters()}. If there is
     * no defaultValue defined, it is an empty String <code>""</code>
     */
    String defaultValue() default "";
    
    /**
     * @return the displayName of this parameter. This can be the 'pretty' name for {@link #name()}. If missing, implementations can do
     * a fallback to {@link #name()} 
     */
    String displayName() default ""; 
    
    /**
     * @return the description for this {@link Parameter}
     */
    String description() default "";
    
    /**
     * This is still experimental.  
     * @return a PropertyEditor
     */
    Class<? extends PropertyEditor> customEditor() default EmptyPropertyEditor.class;

    /**
     * a String used to provide optional YUI tools with a hint about the type the parameter value should be of. For example 'COLOR', 'DATE', 'DOCUMENT'.
     * Note that this is a typeHint for for example the template composer. Thus it does influence the actual return type of the getter method that 
     * is used for the Parameter annotation.
     * @deprecated use the method return type for the storage type, add additional annotations
     *             like {@link org.hippoecm.hst.core.parameters.DocumentLink} or {@link org.hippoecm.hst.core.parameters.Color} for more detailed information
     */
    String typeHint() default "";

    /**
     * Specifies the node type of the document to be searched for.
     * @return the document type String
     *
     * @deprecated see {@link org.hippoecm.hst.core.parameters.DocumentLink}
     */
    String docType() default ""; //Document type is only used when a DOCUMENT type is used.

    /**
     * @return  specifies whether to show a link to create a new document of the type as specified by the docType
     *
     * @deprecated see {@link org.hippoecm.hst.core.parameters.DocumentLink}
     */
    boolean allowCreation() default false;

    /**
     * @return the relative path of the folder where the document is created
     *
     * @deprecated see {@link org.hippoecm.hst.core.parameters.DocumentLink}
     */
    String docLocation() default "";
    
}
