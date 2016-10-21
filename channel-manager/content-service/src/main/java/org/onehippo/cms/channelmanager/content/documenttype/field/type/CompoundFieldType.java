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

package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.documenttype.model.DocumentType;
import org.onehippo.cms.channelmanager.content.documenttype.ContentTypeContext;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompoundFieldType extends FieldType {
    private static final Logger log = LoggerFactory.getLogger(CompoundFieldType.class);

    public CompoundFieldType() {
        setType(Type.COMPOUND);
        setFields(new ArrayList<>());
    }

    @Override
    public Optional<FieldType> init(final FieldTypeContext context,
                                    final ContentTypeContext contentTypeContext,
                                    final DocumentType docType) {
        return super.init(context, contentTypeContext, docType)
                .map(fieldType -> {
                    DocumentTypesService.get().populateFieldsForCompoundType(context.getContentTypeItem().getItemType(),
                                                                             fieldType.getFields(), contentTypeContext, docType);
                    return fieldType.getFields().isEmpty() ? null : fieldType;
                });
    }

    @Override
    public Optional<Object> readFrom(Node node) {
        final String nodeName = getId();
        final List<Map<String, Object>> values = new ArrayList<>();
        try {
            for (Node child : new NodeIterable(node.getNodes(nodeName))) {
                Map<String, Object> valueMap = new HashMap<>();
                for (FieldType fieldType : getFields()) {
                    fieldType.readFrom(child).ifPresent(value -> valueMap.put(fieldType.getId(), value));
                }
                if (!valueMap.isEmpty()) {
                    values.add(valueMap);
                }
            }
            if (!values.isEmpty()) {
                return Optional.of(isMultiple() ? values : values.get(0));
            }
        } catch (RepositoryException e) {
            log.warn("Failed to read nodes for compound type '{}'", getId(), e);
        }
        return Optional.empty();
    }
}
