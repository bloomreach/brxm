/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.model;

import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestModel extends LoadableDetachableModel<Request> {

    static final Logger log = LoggerFactory.getLogger(RequestModel.class);

    private final String id;
    private final Map<String, ?> info;

    public RequestModel(final String id, final Map<String, ?> info) {
        this.id = id;
        this.info = info;
    }

    @Override
    protected Request load() {
        try {
            Session session = UserSession.get().getJcrSession();
            Node node = session.getNodeByIdentifier(id);

            String state = "unknown";
            Date schedule = null;
            if (node.isNodeType(HippoStdPubWfNodeType.NT_HIPPOSTDPUBWF_REQUEST)) {
                state = "request-"+ node.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_TYPE).getString();
                if (node.hasProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_REQDATE)) {
                    schedule = new Date(node.getProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_REQDATE).getLong());
                }
            }
            else if (node.isNodeType("hipposched:workflowjob")) {
                state = "scheduled" + getAttributeValue(node, "hipposched:methodName", state);
                if (node.hasProperty("hipposched:triggers/default/hipposched:nextFireTime")) {
                    schedule = node.getProperty("hipposched:triggers/default/hipposched:nextFireTime").getDate().getTime();
                }
            }

            return new Request(id, schedule, state, info);
        } catch (RepositoryException ex) {
            // status unknown, maybe there are legit reasons for this, so don't emit a warning
            log.info(ex.getClass().getName() + ": " + ex.getMessage());
        }
        return null;
    }

    private String getAttributeValue(final Node node, final String attributeName, final String defaultValue) throws RepositoryException {
        final Property attributeNamesProperty = JcrUtils.getPropertyIfExists(node, "hipposched:attributeNames");
        int i = 0, index = -1;
        if (attributeNamesProperty != null) {
            for (Value value : attributeNamesProperty.getValues()) {
                if (value.getString().equals(attributeName)) {
                    index = i;
                    break;
                }
                i++;
            }
        }
        if (index != -1) {
            final Property attributeValuesProperty = JcrUtils.getPropertyIfExists(node, "hipposched:attributeValues");
            i = 0;
            for (Value value : attributeValuesProperty.getValues()) {
                if (i == index) {
                    return value.getString();
                }
                i++;
            }
        }
        return defaultValue;
    }
}
