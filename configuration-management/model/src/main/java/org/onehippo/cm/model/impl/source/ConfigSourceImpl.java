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
package org.onehippo.cm.model.impl.source;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.definition.AbstractDefinitionImpl;
import org.onehippo.cm.model.impl.definition.ConfigDefinitionImpl;
import org.onehippo.cm.model.impl.definition.NamespaceDefinitionImpl;
import org.onehippo.cm.model.impl.definition.WebFileBundleDefinitionImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.source.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cm.model.definition.DefinitionType.CONFIG;

public class ConfigSourceImpl extends SourceImpl {

    private static final Logger log = LoggerFactory.getLogger(ConfigSourceImpl.class);

    public ConfigSourceImpl(String path, ModuleImpl module) {
        super(path, module);
    }

    public final SourceType getType() {
        return SourceType.CONFIG;
    }

    public NamespaceDefinitionImpl addNamespaceDefinition(final String prefix, final URI uri, final ValueImpl cndPath) {
        final NamespaceDefinitionImpl definition = new NamespaceDefinitionImpl(this, prefix, uri, cndPath);
        if (cndPath != null) {
            cndPath.setDefinition(definition);
        }
        modifiableDefinitions.add(definition);
        markChanged();
        return definition;
    }

    public WebFileBundleDefinitionImpl addWebFileBundleDefinition(final String name) {
        final WebFileBundleDefinitionImpl definition = new WebFileBundleDefinitionImpl(this, name);
        modifiableDefinitions.add(definition);
        markChanged();
        return definition;
    }

    public ConfigDefinitionImpl addConfigDefinition() {
        final ConfigDefinitionImpl definition = new ConfigDefinitionImpl(this);
        modifiableDefinitions.add(definition);
        markChanged();
        return definition;
    }

    public void cleanEmptyDefinitions() {
        Set<AbstractDefinitionImpl> toRemove = new HashSet<>();
        for (AbstractDefinitionImpl def : getDefinitions()) {
            if (CONFIG.isOfType(def)
                    && ((ConfigDefinitionImpl)def).getNode().isEmpty()) {
                toRemove.add(def);
            }
        }
        toRemove.forEach(this::removeDefinition);
    }

    /**
     * Get or create a definition in configSource to contain data for jcrPath.
     * @param jcrPath the path for which we want a definition
     * @return a DefinitionNodeImpl corresponding to the jcrPath, which may or may not be a root
     */
    public DefinitionNodeImpl getOrCreateDefinitionFor(final String jcrPath) {
        return getOrCreateDefinitionFor(JcrPaths.getPath(jcrPath));
    }

    /**
     * Get or create a definition in configSource to contain data for jcrPath.
     * @param jcrPath the path for which we want a definition
     * @return a DefinitionNodeImpl corresponding to the jcrPath, which may or may not be a root
     */
    public DefinitionNodeImpl getOrCreateDefinitionFor(final JcrPath jcrPath) {
        // try to find an existing definition for defRoot
        for (AbstractDefinitionImpl def : getDefinitions()) {
            if (def.getType().equals(CONFIG)) {
                final ConfigDefinitionImpl configDef = (ConfigDefinitionImpl) def;
                if (configDef.getNode().getJcrPath().equals(jcrPath)) {
                    return configDef.getNode();
                }
            }
        }

        // if we haven't returned yet, we didn't find a matching def
        // build a new def
        return addConfigDefinition().withRoot(jcrPath.toString());
    }

}
