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

import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

public class EffectiveNodeTypeChildImpl extends EffectiveNodeTypeItemImpl implements EffectiveNodeTypeChild {

    private String defaultPrimaryType;
    private String type;
    private SortedSet<String> requiredPrimaryTypes = new TreeSet<String>();

    @Override
    protected void doSeal() {
        super.doSeal();
        requiredPrimaryTypes = Collections.unmodifiableSortedSet(requiredPrimaryTypes);
    }

    public EffectiveNodeTypeChildImpl(String name, String definingType) {
        super(name, definingType, true);
    }

    @Override
    public String getDefaultPrimaryType() {
        return defaultPrimaryType;
    }

    public void setDefaultPrimaryType(final String defaultPrimaryType) {
        this.defaultPrimaryType = defaultPrimaryType;
    }

    @Override
    public SortedSet<String> getRequiredPrimaryTypes() {
        return requiredPrimaryTypes;
    }

    @Override
    public String getType() {
        if (type == null) {
            Iterator<String> iterator = requiredPrimaryTypes.iterator();
            StringBuilder sb = new StringBuilder(iterator.next());
            while (iterator.hasNext()) {
                sb.append(',');
                sb.append(iterator.next());
            }
            type = sb.toString();
        }
        return type;
    }
}