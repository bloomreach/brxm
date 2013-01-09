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

package org.hippoecm.hst.configuration.channel;

import java.lang.annotation.Annotation;
import java.util.List;

import org.hippoecm.hst.core.parameters.HstValueType;

/**
 * Definition of a Channel Property.
 */
public interface HstPropertyDefinition {

    public HstValueType getValueType();

    public String getName();

    public Object getDefaultValue();

    public boolean isRequired();

    public List<Annotation> getAnnotations();

    /**
     * @param annotationClass the annotationClass to check
     * @return Returns the annotation T if present on the {@link HstPropertyDefinition} and returns <code>null</code> if not present
     */
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    public <T extends Annotation> List<Annotation> getAnnotations(List<Class<? extends Annotation>> annotationClasses);

}
