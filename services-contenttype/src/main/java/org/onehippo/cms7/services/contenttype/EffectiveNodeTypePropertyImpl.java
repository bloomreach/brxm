/*
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

package org.onehippo.cms7.services.contenttype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.PropertyType;

public class EffectiveNodeTypePropertyImpl extends EffectiveNodeTypeItemImpl implements EffectiveNodeTypeProperty {
    private final int requiredType;
    private List<String> valueConstraints = new ArrayList<String>();
    private List<String> defaultValues = new ArrayList<String>();

    @Override
    protected void doSeal() {
        super.doSeal();
        valueConstraints = Collections.unmodifiableList(valueConstraints);
        defaultValues = Collections.unmodifiableList(defaultValues);
    }

    public EffectiveNodeTypePropertyImpl(String name, String definingType, int requiredType) {
        super(name, definingType, false);
        this.requiredType = requiredType;
    }

    @Override
    public int getRequiredType() {
        return requiredType;
    }

    @Override
    public String getType() {
        return PropertyType.nameFromValue(requiredType);
    }

    @Override
    public List<String> getValueConstraints() {
        return valueConstraints;
    }

    @Override
    public List<String> getDefaultValues() {
        return defaultValues;
    }
}
