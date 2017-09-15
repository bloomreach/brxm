/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cm.engine.autoexport.orderbeforeholder;

import java.util.Map;

import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.impl.path.JcrPath;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;

public class ContentOrderBeforeHolder extends OrderBeforeHolder {

    private final ContentDefinitionImpl contentDefinition;
    private final Map<JcrPath, String> contentOrderBefores;

    public ContentOrderBeforeHolder(final ContentDefinitionImpl contentDefinition, final Map<JcrPath, String> contentOrderBefores) {
        this.contentDefinition = contentDefinition;
        this.contentOrderBefores = contentOrderBefores;
    }

    @Override
    public int compareTo(final Object object) {
        if (object == null) {
            return -1;
        }
        if (object == this) {
            return 0;
        }
        if (object instanceof ContentOrderBeforeHolder) {
            final ContentOrderBeforeHolder other = (ContentOrderBeforeHolder) object;
            return this.getContentRoot().compareTo(other.getContentRoot());
        }
        return 1;
    }

    JcrPath getContentRoot() {
        return JcrPath.get(contentDefinition.getRootPath());
    }

    @Override
    DefinitionNodeImpl getDefinitionNode() {
        return contentDefinition.getNode();
    }

    @Override
    void setOrderBefore(final String orderBefore) {
        contentOrderBefores.put(getContentRoot(), orderBefore);
    }

    @Override
    public void finish() {
        // Unfortunately, it is not yet possible to detect a changed order-before. Always mark the source as
        // changed, triggering a re-export.
        contentDefinition.getSource().markChanged();
    }
}
