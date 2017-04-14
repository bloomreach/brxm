/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.onehippo.cms.channelmanager.content.document.model.FieldValue;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.error.ErrorWithPayloadException;

/**
 * A field that stores its value(s) in a node.
 */
public interface NodeFieldType extends FieldType {

    /**
     * Used by provider-based choice fields.
     */
    void init(final FieldTypeContext fieldContext, final String choiceId);

    /**
     * Used by list-based choice fields.
     */
    void init(final ContentTypeContext parentContext, final String choiceId);

    FieldValue readValue(final Node node);

    void writeValue(final Node node, final FieldValue fieldValue) throws ErrorWithPayloadException, RepositoryException;

    boolean validateValue(final FieldValue value);



}
