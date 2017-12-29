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

import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.path.JcrPaths;
import org.onehippo.cm.model.source.ContentSource;
import org.onehippo.cm.model.source.SourceType;

public class ContentSourceImpl extends SourceImpl implements ContentSource {

    public ContentSourceImpl(String path, ModuleImpl module) {
        super(path, module);
    }

    public final SourceType getType() {
        return SourceType.CONTENT;
    }

    public ContentDefinitionImpl getContentDefinition() {
        return (ContentDefinitionImpl) getDefinitions().get(0);
    }

    public ContentDefinitionImpl addContentDefinition() {
        if (modifiableDefinitions.size() > 0) {
            throw new IllegalStateException(String.format("Content source('%s') can contain only one root node", this));
        }
        final ContentDefinitionImpl definition = new ContentDefinitionImpl(this);
        modifiableDefinitions.add(definition);
        markChanged();
        return definition;
    }

    public ContentDefinitionImpl addContentDefinition(final String contentPath) {
        return addContentDefinition(JcrPaths.getPath(contentPath), null);
    }

    public ContentDefinitionImpl addContentDefinition(final String contentPath, final String orderBefore) {
        return addContentDefinition(JcrPaths.getPath(contentPath), orderBefore);
    }

    public ContentDefinitionImpl addContentDefinition(final JcrPath contentPath) {
        return addContentDefinition(contentPath, null);
    }

    public ContentDefinitionImpl addContentDefinition(final JcrPath contentPath, final String orderBefore) {
        final ContentDefinitionImpl cd = addContentDefinition();

        DefinitionNodeImpl defNode = new DefinitionNodeImpl(contentPath, contentPath.getLastSegment(), cd);
        defNode.setOrderBefore(orderBefore);
        cd.setNode(defNode);

        return cd;
    }
}
