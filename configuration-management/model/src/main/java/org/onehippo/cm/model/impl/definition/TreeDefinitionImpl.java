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
package org.onehippo.cm.model.impl.definition;

import org.onehippo.cm.model.definition.TreeDefinition;
import org.onehippo.cm.model.impl.source.SourceImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPaths;

public abstract class TreeDefinitionImpl<S extends SourceImpl> extends AbstractDefinitionImpl<S>
        implements TreeDefinition<S> {

    private DefinitionNodeImpl node = new DefinitionNodeImpl("/", "/", this);

    // todo: convert to NodePath for storage
    private String rootPath;

    TreeDefinitionImpl(final S source) {
        super(source);
    }

    @Override
    public DefinitionNodeImpl getNode() {
        return node;
    }

    public void setNode(final DefinitionNodeImpl node) {
        this.node = node;
    }

    public DefinitionNodeImpl withRoot(final JcrPath path) {
        return withRoot(path.toString());
    }

    public DefinitionNodeImpl withRoot(final String path) {
        return node = new DefinitionNodeImpl(path, this);
    }

    @Override
    public String getRootPath() {
        return rootPath != null ? rootPath : node.getJcrPath().toString();
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * Compare ContentDefinitions based on the lexical order of the root definition node paths.
     */
    @Override
    public int compareTo(final TreeDefinition o) {
        return this.getNode().getJcrPath().compareTo(JcrPaths.getPath(o.getNode().getPath()));
    }

    public String toString() {
        return getClass().getSimpleName()+"{node.path='"+node.getJcrPath()+", origin="+getOrigin()+"'}";
    }
}
