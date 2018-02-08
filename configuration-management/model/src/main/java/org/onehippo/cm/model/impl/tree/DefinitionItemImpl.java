/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl.tree;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.impl.definition.TreeDefinitionImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.ConfigurationItemCategory;
import org.onehippo.cm.model.tree.DefinitionItem;

public abstract class DefinitionItemImpl extends ModelItemImpl implements DefinitionItem {

    private JcrPath path;
    private DefinitionNodeImpl parent;
    private TreeDefinitionImpl definition;
    private SourceLocationImpl sourceLocation;
    private ConfigurationItemCategory category;

    public DefinitionItemImpl(final JcrPath path, final JcrPathSegment name, final TreeDefinitionImpl definition) {
        if (path == null) {
            throw new IllegalArgumentException("Item path must not be null!");
        }

        setName(name);
        this.path = path;
        this.parent = null;
        this.definition = definition;
        this.sourceLocation = new SourceLocationImpl();
    }

    public DefinitionItemImpl(final String path, final String name, final TreeDefinitionImpl definition) {
        if (StringUtils.isBlank(path)) {
            throw new IllegalArgumentException("Item path must not be blank! name="+name);
        }

        // The global root is a special case
        if (StringUtils.isBlank(name) && !path.equals("/")) {
            throw new IllegalArgumentException("Item name must not be blank! path="+path);
        }

        setName(name);
        this.path = JcrPaths.getPath(path);
        this.parent = null;
        this.definition = definition;
        this.sourceLocation = new SourceLocationImpl();
    }

    public DefinitionItemImpl(final String path, final TreeDefinitionImpl definition) {
        this(path, StringUtils.substringAfterLast(path, "/"), definition);
    }

    public DefinitionItemImpl(final String name, final DefinitionNodeImpl parent) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Item name must not be blank! path="+path);
        }

        if (parent == null) {
            throw new IllegalArgumentException("Item parent can be null only if a Definition is provided!");
        }

        setName(name);
        this.parent = parent;
        this.definition = parent.getDefinition();
        this.sourceLocation = new SourceLocationImpl();

        path = parent.getJcrPath().resolve(name);
    }

    @Override
    public SourceLocationImpl getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public String getPath() {
        return path.toString();
    }

    @Override
    public JcrPath getJcrPath() {
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
    public TreeDefinitionImpl<?> getDefinition() {
        return definition;
    }

    @Override
    public String getOrigin() {
        return getDefinition().getOrigin();
    }

    @Override
    public ConfigurationItemCategory getCategory() {
        return category;
    }

    public void setCategory(final ConfigurationItemCategory category) {
        this.category = category;
    }
}
