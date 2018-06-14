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
package org.hippoecm.hst.platform.configuration.channel;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.configuration.channel.HstPropertyDefinition;
import org.hippoecm.hst.core.parameters.HstValueType;

public abstract class AbstractHstPropertyDefinition implements HstPropertyDefinition {
    protected final String name;

    protected HstValueType type;
    protected boolean required;
    protected Object defaultValue;
    protected boolean hiddenInChannelManager = false;

    public AbstractHstPropertyDefinition(String name) {
        this.name = name;
    }

    @Override
    public HstValueType getValueType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }
    void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public boolean isHiddenInChannelManager() {
        return hiddenInChannelManager;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        for (Annotation annotation : getAnnotations()) {
            if (annotation.annotationType().equals(annotationClass)) {
                return (T)annotation;
            }
        }
        return null;
    }

    @Override
    public <T extends Annotation> List<Annotation> getAnnotations(List<Class<? extends Annotation>> annotationClasses) {
        List<Annotation> annotations = new ArrayList<Annotation>();

        for (Class<? extends Annotation> annotationClass : annotationClasses) {
            Annotation annotation = getAnnotation(annotationClass);
            if (annotation != null) {
                annotations.add(annotation);
            }
        }

        return annotations;
    }

}
