/*
 *  Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.lang.String;import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated use {@link JcrPath} instead
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Deprecated
public @interface DocumentLink {

    /**
     * Specifies the node type of the document to be searched for.
     * @return the document type String
     */
    String docType() default ""; //Document type is only used when a DOCUMENT type is used.

    /**
     * @return  specifies whether to show a link to create a new document of the type as specified by the docType
     */
    boolean allowCreation() default false;

    /**
     * @return the relative path of the folder where the document is created
     */
    String docLocation() default "";

}
