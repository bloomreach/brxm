/*
 *  Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNode;
import org.onehippo.cms.channelmanager.content.documenttype.field.FieldTypeContext;
import org.onehippo.cms.channelmanager.content.picker.NodePicker;
import org.onehippo.cms.json.Json;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class NodeLinkFieldType extends LinkFieldType {

    private ObjectNode config;

    public ObjectNode getConfig() {
        return config;
    }

    public NodeLinkFieldType() {
        setType(Type.NODE_LINK);
    }

    @Override
    public FieldsInformation init(final FieldTypeContext fieldContext) {
        config = Json.object();
        config.set("linkpicker", NodePicker.build(fieldContext));

        return super.init(fieldContext);
    }

    @Override
    protected Map<String, Object> createMetadata(final String uuid, final Node node, final Session session) throws RepositoryException {
        return Collections.singletonMap("displayName", getDisplayName(uuid, session));
    }

    private String getDisplayName(final String uuid, final Session session) throws RepositoryException {
        if (StringUtils.isNotEmpty(uuid)) {
            final Node node = session.getNodeByIdentifier(uuid);
            if (node instanceof HippoNode) {
                return ((HippoNode) node).getDisplayName();
            }
            return node.getName();
        }
        return StringUtils.EMPTY;
    }
}
