/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.search.binder;

import java.util.HashMap;
import java.util.Map;

public class MapPropertyValueProvider extends AbstractPropertyValueProvider {

    private Map<String, Object> propertyMap;

    public MapPropertyValueProvider(Map<String, Object> propMap) {
        this.propertyMap = new HashMap<String, Object>();

        if (propMap != null) {
            this.propertyMap.putAll(propMap);
        }
    }

    @Override
    public boolean hasProperty(String propertyName) {
        return propertyMap.containsKey(propertyName);
    }

    @Override
    public Object getValue(String propertyName) {
        return propertyMap.get(propertyName);
    }

}
