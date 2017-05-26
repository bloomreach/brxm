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

import org.onehippo.cm.model.ConfigurationItemCategory;
import org.onehippo.cm.model.DefinitionItem;

public abstract class DefinitionItemImpl implements DefinitionItem {

    private String path;
    private String name;
    private DefinitionNodeImpl parent;
    private AbstractDefinitionImpl definition;
    private boolean delete;
    private SourceLocationImpl sourceLocation;
    private ConfigurationItemCategory category;

    public DefinitionItemImpl(final String path, final String name, final AbstractDefinitionImpl definition) {
        this.path = path;
        this.name = name;
        this.parent = null;
        this.definition = definition;
        this.sourceLocation = new SourceLocationImpl();
        this.category = null;
    }

    public DefinitionItemImpl(final String name, final DefinitionNodeImpl parent) {
        this.name = name;
        this.parent = parent;
        this.definition = parent.getDefinition();
        this.sourceLocation = new SourceLocationImpl();

        final String parentPath = parent.getPath();
        path = parentPath + (parentPath.endsWith("/") ? "" : "/") + name;
        this.category = null;
    }

    @Override
    public SourceLocationImpl getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public DefinitionNodeImpl getParent() {
        if (parent == null) {
            throw new IllegalStateException("Root node does not have a parent");
        }
        return parent;
    }

    @Override
    public boolean isRoot() {
        return parent == null;
    }

    @Override
    public AbstractDefinitionImpl getDefinition() {
        return definition;
    }

    @Override
    public ConfigurationItemCategory getCategory() {
        return category;
    }

    public void setCategory(final ConfigurationItemCategory category) {
        this.category = category;
    }

    @Override
    public String getOrigin() {
        return getDefinition().getOrigin();
    }


}
