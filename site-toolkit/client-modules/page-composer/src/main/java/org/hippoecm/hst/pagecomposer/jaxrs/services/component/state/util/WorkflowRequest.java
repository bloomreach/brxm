/*
 * Copyright 2020 Bloomreach
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
 *
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;

import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_CREATION_DATE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_REASON;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_REQDATE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_TYPE;
import static org.hippoecm.repository.HippoStdPubWfNodeType.HIPPOSTDPUBWF_USERNAME;

public final class WorkflowRequest {

    private final String id;
    private final Calendar creationDate;
    private final Calendar requestDate;
    private final String reason;
    private final String type;
    private final String username;

    public WorkflowRequest(final Node requestNode) throws RepositoryException {
        id = requestNode.getIdentifier();
        creationDate = JcrUtils.getDateProperty(requestNode, HIPPOSTDPUBWF_CREATION_DATE, null);
        requestDate = JcrUtils.getDateProperty(requestNode, HIPPOSTDPUBWF_REQDATE, null);
        reason = JcrUtils.getStringProperty(requestNode, HIPPOSTDPUBWF_REASON, null);
        type = JcrUtils.getStringProperty(requestNode, HIPPOSTDPUBWF_TYPE, null);
        username = JcrUtils.getStringProperty(requestNode, HIPPOSTDPUBWF_USERNAME, null);
    }

    public String getId() {
        return id;
    }

    public String getReason() {
        return reason;
    }

    public String getType() {
        return type;
    }

    public String getUsername() {
        return username;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public Calendar getRequestDate() {
        return requestDate;
    }
}
