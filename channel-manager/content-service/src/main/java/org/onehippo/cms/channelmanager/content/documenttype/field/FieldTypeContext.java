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

package org.onehippo.cms.channelmanager.content.documenttype.field;

import javax.jcr.Node;

import org.onehippo.cms7.services.contenttype.ContentTypeItem;

/**
 * FieldTypeContext groups and wraps sources of information about a document type field.
 */
public class FieldTypeContext {
    private final Node editorConfigNode;
    private final ContentTypeItem contentTypeItem;

    public FieldTypeContext(final Node editorConfigNode, final ContentTypeItem contentTypeItem) {
        this.editorConfigNode = editorConfigNode;
        this.contentTypeItem = contentTypeItem;
    }

    public Node getEditorConfigNode() {
        return editorConfigNode;
    }

    public ContentTypeItem getContentTypeItem() {
        return contentTypeItem;
    }
}
