/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.ConfigurationItem;
import org.onehippo.cm.model.util.SnsUtils;

public abstract class ConfigurationItemImpl<D extends DefinitionItemImpl> extends ModelItemImpl
        implements ConfigurationItem<D> {

    private ConfigurationNodeImpl parent;
    private final List<D> modifiableDefinitions = new ArrayList<>();
    protected final List<D> definitions = Collections.unmodifiableList(modifiableDefinitions);
    private boolean deleted;

    @Override
    public String getPath() {
        if (isRoot()) {
            return "/";
        } else {
            final String base = StringUtils.appendIfMissing(parent.getPath(), "/");
            if (SnsUtils.hasSns(name, parent.getNodes().keySet())) {
                return base + name;
            } else {
                return base + SnsUtils.getUnindexedName(name);
            }
        }
    }

    @Override
    public ConfigurationNodeImpl getParent() {
        return parent;
    }

    public void setParent(final ConfigurationNodeImpl parent) {
        this.parent = parent;
    }

    @Override
    public List<D> getDefinitions() {
        return definitions;
    }

    public void addDefinition(final D definition) {
        if (modifiableDefinitions.isEmpty()) {
            modifiableDefinitions.add(definition);
        }
        else {
            DefinitionItemImpl lastDef = modifiableDefinitions.get(modifiableDefinitions.size() - 1);
            if (!lastDef.equals(definition)) {
                modifiableDefinitions.add(definition);
            }
        }
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public boolean isRoot() {
        return parent == null;
    }

    @Override
    public String getOrigin() {
        return getDefinitions()
                .stream()
                .map(d -> d.getDefinition().getOrigin())
                .collect(Collectors.toList())
                .toString();
    }
}
