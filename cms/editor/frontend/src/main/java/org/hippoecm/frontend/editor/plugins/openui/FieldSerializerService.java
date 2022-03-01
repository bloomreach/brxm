/*
 *  Copyright 2020-2022 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.editor.plugins.openui;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.collections4.IteratorUtils;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;
import org.onehippo.cms7.services.contenttype.ContentTypeService;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

class FieldSerializerService {
    private static final Set<String> SUPPORTED_PROPERTIES = ImmutableSet.of(
        "String",
        "Text",
        "Html",
        "Long",
        "Double",
        "DynamicDropdown",
        "StaticDropdown",
        "Boolean",
        "Date",
        "CalendarDate",
        "selection:RadioGroup",
        "selection:BooleanRadioGroup",
        "OpenUiString"
    );

    private static final Set<Integer> SUPPORTED_JCR_TYPES = ImmutableSet.of(
        PropertyType.BOOLEAN,
        PropertyType.DATE,
        PropertyType.DECIMAL,
        PropertyType.DOUBLE,
        PropertyType.LONG,
        PropertyType.STRING
    );

    private static final Map<String, String> PRIMITIVE_NODES = ImmutableMap.of(
        "hippogallerypicker:imagelink", HippoNodeType.HIPPO_DOCBASE,
        HippoNodeType.NT_MIRROR, HippoNodeType.HIPPO_DOCBASE,
        HippoStdNodeType.NT_HTML, HippoStdNodeType.HIPPOSTD_CONTENT
    );

    static boolean isSupportedType(final ContentTypeProperty type) {
        if (SUPPORTED_PROPERTIES.contains(type.getItemType()) && !type.isDerivedItem()) {
            return true;
        }

        if (SUPPORTED_JCR_TYPES.contains(type.getEffectiveNodeTypeItem().getRequiredType())) {
            return true;
        }

        return false;
    }

    static boolean isPrimitiveType(ContentType type) {
        return PRIMITIVE_NODES.containsKey(type.getName());
    }

    private final ContentTypeService contentTypeService;

    public FieldSerializerService(Optional<ContentTypeService> contentTypeService) {
        this.contentTypeService = contentTypeService.orElse(HippoServiceRegistry.getService(ContentTypeService.class));
    }

    Object serialize(Node[] nodes) {
        Object[] serialized = Stream.of(nodes)
            .map(this::serialize)
            .filter(object -> !JSONObject.NULL.equals(object))
            .toArray();

        return serialized.length > 0 ? new JSONArray(serialized) : JSONObject.NULL;
    }

    Object serialize(Node node) {
        final ContentType contentType;
        try {
            contentType = contentTypeService.getContentTypes().getContentTypeForNode(node);
            if (isPrimitiveType(contentType)) {
                return serialize(node.getProperty(PRIMITIVE_NODES.get(contentType.getName())));
            }
        } catch (RepositoryException e) {
            return JSONObject.NULL;
        }

        final JSONObject result = new JSONObject();

        contentType.getProperties().forEach((key, property) -> {
            if (!isSupportedType(property)) {
                return;
            }

            try {
                result.put(key, serialize(node.getProperty(key)));
            } catch (RepositoryException e) {}
        });

        contentType.getChildren().forEach((key, child) -> {
            try {
                result.put(key, child.isMultiple()
                        ? serialize(IteratorUtils.toArray(node.getNodes(key), Node.class))
                        : serialize(node.getNode(key)));
            } catch (RepositoryException e) {}
        });

        return result;
    }

    String serialize(Property property, Integer index) throws RepositoryException, IndexOutOfBoundsException {
        final Value[] values = property.getValues();

        return serialize(values[index]);
    }

    Object serialize(Property property) throws RepositoryException {
        if (!property.isMultiple()) {
            return serialize(property.getValue());
        }

        final JSONArray result = new JSONArray();
        for (Value value : property.getValues()) {
            result.put(serialize(value));
        }

        return result;
    }

    String serialize(Value value) throws RepositoryException {
        return value.getString();
    }
}
