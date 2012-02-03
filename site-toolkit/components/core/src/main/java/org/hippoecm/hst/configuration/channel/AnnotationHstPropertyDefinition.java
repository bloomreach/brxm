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
package org.hippoecm.hst.configuration.channel;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.hippoecm.hst.core.parameters.HstValueType;
import org.hippoecm.hst.core.parameters.Parameter;

public class AnnotationHstPropertyDefinition extends AbstractHstPropertyDefinition {

    private List<Annotation> annotations = new ArrayList<Annotation>();

    public AnnotationHstPropertyDefinition(Parameter propAnnotation, Class<?> returnType, Annotation[] annotations) {
        super(propAnnotation.name());
        init(propAnnotation, returnType, annotations, propAnnotation.required(), propAnnotation.defaultValue());
    }

    private void init(Annotation propAnnotation,Class<?> returnType, Annotation[] annotations, boolean required, String defaultValue) {
        type = getHstType(returnType);
        this.required = required;
        this.defaultValue = defaultValue;

        for (Annotation annotation : annotations) {
            if (annotation == propAnnotation) {
                continue;
            }
            this.annotations.add(annotation);
        }
    }

    @Override
    public List<Annotation> getAnnotations() {
        return annotations;
    }

    private static HstValueType getHstType(Class<?> type) {
        if (type == String.class) {
            return HstValueType.STRING;
        } else if (type == Boolean.class || type == boolean.class) {
            return HstValueType.BOOLEAN;
        } else if (type == Long.class || type == Integer.class || type == int.class || type == long.class) {
            return HstValueType.INTEGER;
        } else if (type == Calendar.class) {
            return HstValueType.DATE;
        } else if (type == Double.class || type == double.class || type == float.class) {
            return HstValueType.DOUBLE;
        }
        throw new ClassCastException("Could not cast " + type + " to any of the primitive types");
    }
}
