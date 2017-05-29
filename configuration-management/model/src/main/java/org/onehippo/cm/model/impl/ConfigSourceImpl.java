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

import org.onehippo.cm.model.SourceType;

public class ConfigSourceImpl extends SourceImpl {

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
        return definition;
    }

    public WebFileBundleDefinitionImpl addWebFileBundleDefinition(final String name) {
        final WebFileBundleDefinitionImpl definition = new WebFileBundleDefinitionImpl(this, name);
        modifiableDefinitions.add(definition);
        return definition;
    }

    public ConfigDefinitionImpl addConfigDefinition() {
        final ConfigDefinitionImpl definition = new ConfigDefinitionImpl(this);
        modifiableDefinitions.add(definition);
        return definition;
    }

}
