/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.channelmanager.content.documenttype.field.type;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeConfig;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class InternalLinkFieldType extends LinkFieldType {

    private static final Logger log = LoggerFactory.getLogger(InternalLinkFieldType.class);

    private static final String[] DOCUMENT_PICKER_STRING_PROPERTIES = {
            "base.path",
            "base.uuid",
            "cluster.name",
            "last.visited.enabled",
            "last.visited.key",
            "last.visited.nodetypes",
            "language.context.aware",
    };

    private ObjectNode config;

    public ObjectNode getConfig() {
        return config;
    }

    public InternalLinkFieldType() {
        setType(Type.INTERNAL_LINK);
    }

    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        config = Json.object();

        final ObjectNode documentPickerConfig = new FieldTypeConfig(fieldContext)
                .strings(DOCUMENT_PICKER_STRING_PROPERTIES)
                .multipleStrings(PICKER_MULTIPLE_STRING_PROPERTIES)
                .build();
        config.set("linkpicker", documentPickerConfig);

        return super.init(fieldContext);
    }

    @Override
    protected String createUrl(final String uuid, final Session session) {
        if (StringUtils.isNotEmpty(uuid)) {
            try {
                final Node node = session.getNodeByIdentifier(uuid);
                return node.getName();
            } catch (ItemNotFoundException e) {
                log.warn("Unable to find item: {} : {} ", uuid, e);
            } catch (RepositoryException e) {
                log.warn("Error while trying to get node name for: {}", uuid, e);
            }
        }
        return "";
    }
}
