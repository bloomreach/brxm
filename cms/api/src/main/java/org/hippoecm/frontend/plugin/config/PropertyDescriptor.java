/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugin.config;

import java.io.Serializable;

/**
 * Describes property of a plugin. The class defines information on type of the property and its multiplicity.
 */
public class PropertyDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;

    private final int type;

    private final boolean multiple;

    public PropertyDescriptor(String name, int type, boolean multiple) {
        this.name = name;
        this.type = type;
        this.multiple = multiple;
    }

    /**
     * The name of the property.
     *
     * @return name of the property
     */
    public String getName() {
        return name;
    }

    /**
     * The type of the property as defined in {@link javax.jcr.PropertyType}.
     *
     * @return type of the property
     * @see javax.jcr.PropertyType
     */
    public int getType() {
        return type;
    }

    /**
     * The flag indicating that property is multiple.
     *
     * @return multiplicity flag
     */
    public boolean isMultiple() {
        return multiple;
    }

}
