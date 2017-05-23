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

import org.onehippo.cm.model.ContentDefinition;
import org.onehippo.cm.model.DefinitionNode;
import org.onehippo.cm.model.DefinitionType;

public class ContentDefinitionImpl extends AbstractDefinitionImpl
        implements ContentDefinition {

    private DefinitionNodeImpl node = new DefinitionNodeImpl("/", "/", this);

    public ContentDefinitionImpl(final SourceImpl source) {
        super(source);
    }

    @Override
    public DefinitionType getType() {
        return DefinitionType.CONTENT;
    }

    @Override
    public DefinitionNode getNode() {
        return node;
    }

    public DefinitionNodeImpl getModifiableNode() {
        return node;
    }

    public void setNode(final DefinitionNodeImpl node) {
        this.node = node;
    }

    /**
     * Compare ContentDefinitions based on the lexical order of the root definition node paths.
     */
    @Override
    public int compareTo(final ContentDefinition o) {
        return this.getNode().getPath().compareTo(o.getNode().getPath());
    }
}
