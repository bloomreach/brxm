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
package org.onehippo.cm.impl.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.onehippo.cm.api.model.ConfigurationItem;
import org.onehippo.cm.api.model.ConfigurationNode;
import org.onehippo.cm.api.model.DefinitionItem;

public abstract class ConfigurationItemImpl implements ConfigurationItem {

    private String name;
    private String path;
    private ConfigurationNodeImpl parent;
    private final List<DefinitionItemImpl> modifiableDefinitions = new ArrayList<>();
    private final List<DefinitionItem> definitions = Collections.unmodifiableList(modifiableDefinitions);
    private boolean deleted;

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    @Override
    public ConfigurationNode getParent() {
        return parent;
    }

    public ConfigurationNodeImpl getModifiableParent() {
        return parent;
    }

    public void setParent(final ConfigurationNodeImpl parent) {
        this.parent = parent;
    }

    @Override
    public List<DefinitionItem> getDefinitions() {
        return definitions;
    }

    public void addDefinitionItem(final DefinitionItemImpl definitionItem) {
        modifiableDefinitions.add(definitionItem);
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }
}
