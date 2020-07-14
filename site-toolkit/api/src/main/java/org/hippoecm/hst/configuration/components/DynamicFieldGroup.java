/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.components;

import org.hippoecm.hst.core.parameters.FieldGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Field Group representation
 */
public class DynamicFieldGroup {

    private final String titleKey;

    private final List<String> parameters = new ArrayList<>();

    public DynamicFieldGroup(final FieldGroup fieldGroupAnnotation) {
        titleKey = fieldGroupAnnotation.titleKey();
        parameters.addAll(Arrays.asList(fieldGroupAnnotation.value()));
    }

    public DynamicFieldGroup(final String titleKey, final Collection<String> parameters) {
        this.titleKey = titleKey;
        this.parameters.addAll(parameters);
    }

    public String getTitleKey() {
        return titleKey;
    }

    public List<String> getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicFieldGroup that = (DynamicFieldGroup) o;
        return titleKey.equals(that.titleKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(titleKey);
    }

    @Override
    public String toString() {
        return "DynamicFieldGroup{" +
                "titleKey='" + titleKey + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
