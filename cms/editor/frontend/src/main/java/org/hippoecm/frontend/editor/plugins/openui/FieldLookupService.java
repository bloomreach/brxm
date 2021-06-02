/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.wicket.ajax.json.JSONObject;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FieldLookupService {
    private final ContentTypeService contentTypeService;

    private final FieldSerializerService fieldSerializerService;

    private final Logger logger;

    public FieldLookupService(
        FieldSerializerService fieldSerializerService,
        Optional<ContentTypeService> contentTypeService,
        Optional<Logger> logger
    ) {
        this.contentTypeService = contentTypeService.orElse(HippoServiceRegistry.getService(ContentTypeService.class));
        this.fieldSerializerService = fieldSerializerService;
        this.logger = logger.orElse(LoggerFactory.getLogger(FieldLookupService.class));
    }

    ContentType getContentTypeForNode(final Node node) throws RepositoryException {
        return contentTypeService.getContentTypes().getContentTypeForNode(node);
    }

    ContentTypeProperty getContentTypePropertyByKey(final ContentType contentType, final String key) {
        final Map<String, ContentTypeProperty> properties = contentType.getProperties();
        if (properties.containsKey(key)) {
            return properties.get(key);
        }
        return null;
    }

    ContentTypeChild getContentTypeChildByKey(final ContentType contentType, final String key) {
        final Map<String, ContentTypeChild> children = contentType.getChildren();
        if (children.containsKey(key)) {
            return children.get(key);
        }
        return null;
    }

    Object lookup(Node root, String[] path) {
        if (path.length == 0) {
            return fieldSerializerService.serialize(root);
        }

        final String key = path[0];
        final String[] tail = Arrays.copyOfRange(path, 1, path.length);

        try {
            final ContentType contentType = getContentTypeForNode(root);

            if (FieldSerializerService.isPrimitiveType(contentType)) {
                return fieldSerializerService.serialize(root);
            }

            final ContentTypeProperty contentTypeProperty = getContentTypePropertyByKey(contentType, key);
            if (contentTypeProperty != null) {
                return lookup(contentTypeProperty, root.getProperty(key), tail);
            }

            final ContentTypeChild contentTypeChild = getContentTypeChildByKey(contentType, key);
            if (contentTypeChild != null) {
                return lookup(contentTypeChild, IteratorUtils.toArray(root.getNodes(key), Node.class), tail);
            }
        } catch (RepositoryException e) {}

        logger.warn("Failed to resolve '{}'.", key);

        return JSONObject.NULL;
    }

    private Object lookup(ContentTypeChild type, Node[] nodes, String[] path) throws RepositoryException {
        if (nodes.length == 0) {
            return JSONObject.NULL;
        }

        if (!type.isMultiple()) {
            return lookup(nodes[0], path);
        }

        if (path.length == 0) {
            return fieldSerializerService.serialize(nodes);
        }

        try {
            return lookup(
                    nodes[Integer.parseUnsignedInt(path[0])],
                    Arrays.copyOfRange(path, 1, path.length)
            );
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            logger.warn("There is no value for index '{}'.", path[0]);

            throw new RepositoryException(e);
        }
    }

    private Object lookup(ContentTypeProperty type, Property property, String[] path) throws RepositoryException {
        if (!FieldSerializerService.isSupportedType(type)) {
            logger.warn("Property '{}' is not supported.", type.getName());

            throw new RepositoryException();
        }

        if (path.length == 0) {
            return fieldSerializerService.serialize(property);
        }

        try {
            return fieldSerializerService.serialize(property, Integer.parseUnsignedInt(path[0]));
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            logger.warn("There is no value for index '{}'.", path[0]);

            throw new RepositoryException(e);
        }
    }
}
