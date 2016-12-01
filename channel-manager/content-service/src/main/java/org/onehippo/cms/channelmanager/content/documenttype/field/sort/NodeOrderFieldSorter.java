/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms.channelmanager.content.documenttype.field.sort;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.util.NamespaceUtils;

/**
 * Sorts fields of a content type in the order of the editor template nodes.
 */
public class NodeOrderFieldSorter implements FieldSorter {

    @Override
    public List<FieldTypeContext> sortFields(final ContentTypeContext context) {
        final List<FieldTypeContext> sortedFields = new ArrayList<>();
        final List<Node> editorConfigFieldNodes = NamespaceUtils.getEditorFieldConfigNodes(context.getContentTypeRoot());

        for (final Node editorConfigFieldNode : editorConfigFieldNodes) {
            FieldTypeContext.create(editorConfigFieldNode, context).ifPresent(sortedFields::add);
        }

        return sortedFields;
    }
}
