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

package org.onehippo.cm.engine.autoexport.orderbeforeholder;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.path.JcrPath;
import org.onehippo.cm.model.impl.tree.DefinitionNodeImpl;

public class ContentOrderBeforeHolder extends OrderBeforeHolder {

    private final ContentDefinitionImpl contentDefinition;
    private final Map<JcrPath, String> contentOrderBefores;
    private final String originalOrderBefore;

    public ContentOrderBeforeHolder(final ContentDefinitionImpl contentDefinition, final Map<JcrPath, String> contentOrderBefores) {
        this.contentDefinition = contentDefinition;
        this.contentOrderBefores = contentOrderBefores;
        this.originalOrderBefore = contentDefinition.getNode().getOrderBefore();
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
            // compare using only the root path -- when applying content, the order before is also used to order the
            // definitions, but we are recalculating the order before, so assume they are all null

            final ContentOrderBeforeHolder other = (ContentOrderBeforeHolder) object;
            return this.getContentRoot().compareTo(other.getContentRoot());
        }
        return 1;  // assuming 'object' is config, which means this content object must be sorted later
    }

    public JcrPath getContentRoot() {
        return contentDefinition.getRootPath();
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
        if (!StringUtils.equals(contentOrderBefores.get(getContentRoot()), originalOrderBefore)) {
            contentDefinition.getSource().markChanged();
        }
    }
}
