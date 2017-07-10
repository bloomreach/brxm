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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onehippo.cm.model.DefinitionType;
import org.onehippo.cm.model.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        for (AbstractDefinitionImpl def : getModifiableDefinitions()) {
            if (DefinitionType.CONFIG.isOfType(def)
                    && ((ConfigDefinitionImpl)def).getModifiableNode().isEmpty()) {
                toRemove.add(def);
            }
        }
        toRemove.forEach(this::removeDefinition);
    }


    /**
     * Get or create a definition in configSource to contain data for jcrPath. This may need to create a definition
     * for an ancestor of jcrPath in order to comply with the requirement that indexed paths may not be used as a
     * definition root.
     * @param jcrPath the path for which we want a definition
     * @return a DefinitionNodeImpl corresponding to the jcrPath, which may or may not be a root
     */
    public DefinitionNodeImpl getOrCreateDefinitionFor(final String jcrPath) {
        final String[] pathSegments = jcrPath.substring(1).split("/");

        // default to the full path, unless we find a SNS index that we need to deal with
        String defRoot = jcrPath;
        List<String> remainder = Collections.emptyList();

        // scan the path segments for a SNS index
        for (int idx = 0; idx < pathSegments.length; idx++) {
            final String name = pathSegments[idx];

            if (name.contains("[")) {
                // if we find one, back up to the parent and use that as the def root
                defRoot = "/" + String.join("/", Arrays.asList(pathSegments).subList(0, idx));
                remainder = Arrays.asList(pathSegments).subList(idx, pathSegments.length);
                break;
            }
        }

        // try to find an existing definition for defRoot
        for (AbstractDefinitionImpl def : getModifiableDefinitions()) {
            if (def.getType().equals(DefinitionType.CONFIG)) {
                final ConfigDefinitionImpl configDef = (ConfigDefinitionImpl) def;
                if (configDef.getModifiableNode().getPath().equals(defRoot)) {
                    // if we find one, then walk down the remainder to the node we really want
                    return buildRemainderNodes(jcrPath, configDef.getModifiableNode(), remainder);
                }
            }
        }

        // if we haven't returned yet, we didn't find a matching def for defRoot
        // build a new def and any required descendant nodes
        final DefinitionNodeImpl defNode = addConfigDefinition().withRoot(defRoot);
        return buildRemainderNodes(jcrPath, defNode, remainder);
    }

    /**
     * Helper for {@link #getOrCreateDefinitionFor(String)} -- builds out nodes under configDef as
     * necessary.
     * @param jcrPath
     * @param node starting node (typically a configDef root)
     * @param remainder possibly-empty list of path segments needed below node
     * @return
     */
    private DefinitionNodeImpl buildRemainderNodes(final String jcrPath, DefinitionNodeImpl node, final List<String> remainder) {
        for (final String segment : remainder) {
            if (node.getNodes().containsKey(segment)) {
                node = node.getNode(segment);
            }
            else {
                node = node.addNode(segment);
            }
        }
        if (node == null) {
            log.error("Produced a null result for path: {}!", jcrPath, new IllegalStateException());
        }
        return node;
    }

}
