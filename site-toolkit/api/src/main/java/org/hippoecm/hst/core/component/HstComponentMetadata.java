/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.core.component;

/**
 * Interface that defines abstract metadata of a specific HstComponent class
 */
public interface HstComponentMetadata {

    /**
     * Return the name of the underlying class.
     */
    String getClassName();

    /**
     * Determine whether the underlying operation method has an annotation
     * of the given type defined.
     * @param annotationType the annotation type to look for
     * @return whether a matching annotation is defined
     */
    boolean hasMethodAnnotatedBy(String annotationType, String methodName);

}
