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

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.hst.core.parameters.HstValueType;

public final class JcrHstPropertyDefinition extends AbstractHstPropertyDefinition {

    public JcrHstPropertyDefinition(Property prop, boolean isPrototype) throws RepositoryException {
        super(prop.getName());
        type = getHstType(prop.getType());
        if (isPrototype) {
            Value value = prop.getValue();
            defaultValue = ChannelPropertyMapper.jcrToJava(value, type);
        } else {
            defaultValue = null;
        }
    }

    private static HstValueType getHstType(int jcrType) throws RepositoryException {
        switch (jcrType) {
            case PropertyType.STRING:
                return HstValueType.STRING;
            case PropertyType.BOOLEAN:
                return HstValueType.BOOLEAN;
            case PropertyType.DATE:
                return HstValueType.DATE;
            case PropertyType.LONG:
                return HstValueType.INTEGER;
            case PropertyType.DOUBLE:
                return HstValueType.DOUBLE;
            default:
                throw new RepositoryException();
        }
    }
    
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return null;
    }

}
