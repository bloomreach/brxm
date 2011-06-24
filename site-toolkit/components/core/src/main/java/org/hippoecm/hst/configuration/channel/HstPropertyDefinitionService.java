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

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

public final class HstPropertyDefinitionService implements HstPropertyDefinition {

    private final HstPropertyType type;
    private final String name;
    private final boolean multiValued;
    private final Object defaultValue;

    public HstPropertyDefinitionService(Property prop, boolean isPrototype) throws RepositoryException {
        PropertyDefinition pd = prop.getDefinition();

        name = prop.getName();

        type = getHstType(prop.getType());
        multiValued = pd.isMultiple();
        if (isPrototype) {
            if (multiValued) {
                Value[] values = prop.getValues();
                if (values.length > 0) {
                    defaultValue = ChannelPropertyMapper.jcrToJava(values[0], type);
                } else {
                    defaultValue = null;
                }
            } else {
                Value value = prop.getValue();
                defaultValue = ChannelPropertyMapper.jcrToJava(value, type);
            }
        } else {
            defaultValue = null;
        }
    }

    private static HstPropertyType getHstType(int jcrType) throws RepositoryException {
        switch (jcrType) {
            case PropertyType.STRING:
                return HstPropertyType.STRING;
            case PropertyType.BOOLEAN:
                return HstPropertyType.BOOLEAN;
            case PropertyType.DATE:
                return HstPropertyType.DATE;
            case PropertyType.LONG:
                return HstPropertyType.INTEGER;
            case PropertyType.DOUBLE:
                return HstPropertyType.DOUBLE;
            default:
                throw new RepositoryException();
        }
    }

    @Override
    public HstPropertyType getType() {
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

    @Override
    public boolean isMultiValued() {
        return multiValued;
    }

    @Override
    public boolean isValid(final Object value) {
        // FIXME: use validators
        return true;
    }

}
