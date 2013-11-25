/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.documentworkflow.model;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Variant
 */
public class Variant {

    private String stateSummary;
    private String holder;
    private Set<String> availabilities = new LinkedHashSet<>();

    private Map<String, Object> properties;

    public String getStateSummary() {
        return stateSummary;
    }

    public void setStateSummary(String stateSummary) {
        this.stateSummary = stateSummary;
    }

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public Set<String> getAvailabilities() {
        return availabilities;
    }

    public void setAvailabilities(Set<String> availabilities) {
        this.availabilities = availabilities;
    }

    public void setAvailabilitiesByArray(String [] availabilities) {
        this.availabilities = new LinkedHashSet<String>(Arrays.asList(availabilities));
    }

    public void addAvailabilities(String ... availabilities) {
        this.availabilities.addAll(Arrays.asList(availabilities));
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
