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

package org.onehippo.cms7.resourcebundle.data;

import java.io.Serializable;
import java.util.Map;

/**
 * A resource represents a key + value-set of a resource bundle.
 */
public class Resource implements Comparable<Resource>, Serializable {

    private Bundle bundle; // Reference to parent bundle
    private String key; // Resource key
    private String description; // Description of the purpose of the resource
    private Map<String, String> valueMap; // map of {ValueSetName, Value}.

    Resource(Bundle bundle, String key, String description, Map<String, String> valueMap) {
        this.bundle = bundle;
        this.key = key;
        this.description = description;
        this.valueMap = valueMap;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, String> getValueMap() {
        return valueMap;
    }

    public String getValue(String valueSetName) {
        return valueMap.get(valueSetName);
    }

    public String deleteValue(String valueSetName) {
        return valueMap.remove(valueSetName);
    }

    public void setValue(String valueSet, String value) {
        valueMap.put(valueSet, value);
    }

    public int compareTo(Resource other) {
        return key.compareTo(other.getKey());
    }
}
