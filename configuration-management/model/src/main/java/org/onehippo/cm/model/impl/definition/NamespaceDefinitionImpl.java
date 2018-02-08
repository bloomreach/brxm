/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.impl.definition;

import java.net.URI;

import org.onehippo.cm.model.definition.DefinitionType;
import org.onehippo.cm.model.definition.NamespaceDefinition;
import org.onehippo.cm.model.impl.source.ConfigSourceImpl;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.ValueImpl;

public class NamespaceDefinitionImpl extends AbstractDefinitionImpl<ConfigSourceImpl>
        implements NamespaceDefinition<ConfigSourceImpl> {

    private final String prefix;
    private final URI uri;
    private final ValueImpl cndPath;

    public NamespaceDefinitionImpl(final ConfigSourceImpl source, final String prefix, final URI uri, final ValueImpl cndPath) {
        super(source);
        this.prefix = prefix;
        this.uri = uri;
        this.cndPath = cndPath;

        if (cndPath != null && !cndPath.isResource()) {
            throw new IllegalArgumentException("CND path must be a resource reference!");
        }
    }

    @Override
    public DefinitionType getType() {
        return DefinitionType.NAMESPACE;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public ValueImpl getCndPath() {
        return cndPath;
    }

    public String toString() {
        return getClass().getSimpleName()+"{prefix='"+getPrefix()+", origin="+getOrigin()+"'}";
    }
}
