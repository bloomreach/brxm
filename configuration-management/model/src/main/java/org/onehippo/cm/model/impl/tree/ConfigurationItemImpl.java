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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPathSegment;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.tree.ConfigurationItem;
import org.onehippo.cm.model.util.SnsUtils;

public abstract class ConfigurationItemImpl<D extends DefinitionItemImpl> extends ModelItemImpl
        implements ConfigurationItem {

    private ConfigurationNodeImpl parent;
    private final List<D> modifiableDefinitions = new ArrayList<>();
    protected final List<D> definitions = Collections.unmodifiableList(modifiableDefinitions);
    private boolean deleted;

    @Override
    public String getPath() {
        return getJcrPath().toString();
    }

    @Override
    public JcrPath getJcrPath() {
        // todo: store path instead of recreating it on each call
        if (isRoot()) {
            return JcrPaths.ROOT;
        } else {
            if (SnsUtils.hasSns(getName(), parent.getNodeNames())) {
                return parent.getJcrPath().resolve(name);
            } else {
                return parent.getJcrPath().resolve(name.withIndex(0));
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

    void addDefinition(final D definition) {
        if (modifiableDefinitions.isEmpty()) {
            modifiableDefinitions.add(definition);
        }
        else {
            // suppress duplicate entries
            DefinitionItemImpl lastDef = modifiableDefinitions.get(modifiableDefinitions.size() - 1);
            if (!lastDef.equals(definition)) {
                modifiableDefinitions.add(definition);
            }
        }
    }

    /**
     * Remove a single definition from the list of definition back-references for this item. This is intended to be used
     * only in the special-case situation of restoring a deleted item during auto-export.
     * @param definition the definition to remove from the back-references
     */
    public void removeDefinition(final D definition) {
        modifiableDefinitions.remove(definition);
    }

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

    /**
     * Produce an output equivalent to {@link #getOrigin()}, but excluding one definition -- most likely because that
     * definition is currently being processed and being contrasted with preceding definitions.
     * @param except the definition to exclude
     * @return a String representation of definitions other than "except"
     */
    public String getOrigin(D except) {
        return getDefinitions()
                .stream()
                .filter(Predicate.isEqual(except).negate())
                .map(d -> d.getDefinition().getOrigin())
                .collect(Collectors.toList())
                .toString();
    }
}
