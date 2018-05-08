/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.document.util;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.junit.Test;
import org.onehippo.cms.channelmanager.content.error.ErrorInfo;
import org.onehippo.repository.mock.MockNode;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;

import static java.util.Collections.emptyMap;
import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

public class HintsInspectorImplTest {

    private final HintsInspector hintsInspector = new HintsInspectorImpl();

    @Test
    public void canCreateDraft() throws Exception {
        final Map<String, Serializable> hints = new HashMap<>();

        assertFalse(hintsInspector.canObtainEditableDocument(hints));

        hints.put("obtainEditableInstance", Boolean.FALSE);
        assertFalse(hintsInspector.canObtainEditableDocument(hints));

        hints.put("obtainEditableInstance", Boolean.TRUE);
        assertTrue(hintsInspector.canObtainEditableDocument(hints));
    }

    @Test
    public void canUpdateDocument() throws Exception {
        final Map<String, Serializable> hints = new HashMap<>();

        assertFalse(hintsInspector.canUpdateDocument(hints));

        hints.put("commitEditableInstance", Boolean.FALSE);
        assertFalse(hintsInspector.canUpdateDocument(hints));

        hints.put("commitEditableInstance", Boolean.TRUE);
        assertTrue(hintsInspector.canUpdateDocument(hints));
    }

    @Test
    public void canUpdateDocumentWithException() throws Exception {
        assertFalse(hintsInspector.canUpdateDocument(emptyMap()));
    }

    @Test
    public void canDeleteDraft() throws Exception {
        final Map<String, Serializable> hints = new HashMap<>();

        assertFalse(hintsInspector.canDisposeEditableDocument(hints));

        hints.put("disposeEditableInstance", Boolean.FALSE);
        assertFalse(hintsInspector.canDisposeEditableDocument(hints));

        hints.put("disposeEditableInstance", Boolean.TRUE);
        assertTrue(hintsInspector.canDisposeEditableDocument(hints));
    }

    @Test
    public void determineEditingFailureWithException() throws Exception {
        final Session session = createMock(Session.class);
        assertFalse(hintsInspector.determineEditingFailure(emptyMap(), session).isPresent());
    }

    @Test
    public void determineEditingFailureUnknown() throws Exception {
        final Session session = createMock(Session.class);
        final Map<String, Serializable> hints = new HashMap<>();

        assertFalse(hintsInspector.determineEditingFailure(hints, session).isPresent());

    }

    @Test
    public void determineEditingFailureRequestPending() throws Exception {
        final Session session = createMock(Session.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("requests", (Serializable) Collections.EMPTY_MAP);

        final Optional<ErrorInfo> errorInfoOptional = hintsInspector.determineEditingFailure(hints, session);
        assertThat("Errorinfo should be present", errorInfoOptional.isPresent());
        if (errorInfoOptional.isPresent()) {
            final ErrorInfo errorInfo = errorInfoOptional.get();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.REQUEST_PENDING));
            assertNull(errorInfo.getParams());
        }
    }

    @Test
    public void determineEditingFailureCancelRequestPublicationPending() throws Exception {
        final Session session = createMock(Session.class);
        final MockNode requestNode = new MockNode("requestNode");
        requestNode.setProperty(HippoStdPubWfNodeType.HIPPOSTDPUBWF_TYPE, HippoStdPubWfNodeType.PUBLISH);
        expect(session.getNodeByIdentifier("request-node-id")).andReturn(requestNode);
        replayAll();

        final Map<String, Serializable> hints = new HashMap<>();
        final Map<String, Serializable> requests = new HashMap<>();
        final Map<String, Boolean> request = new HashMap<>();
        hints.put("requests", (Serializable) requests);
        requests.put("request-node-id", (Serializable) request);
        request.put("cancelRequest", Boolean.TRUE);

        final Optional<ErrorInfo> errorInfoOptional = hintsInspector.determineEditingFailure(hints, session);
        assertThat("Errorinfo should be present", errorInfoOptional.isPresent());
        if (errorInfoOptional.isPresent()) {
            final ErrorInfo errorInfo = errorInfoOptional.get();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.CANCELABLE_PUBLICATION_REQUEST_PENDING));
            assertNull(errorInfo.getParams());
        }

        verifyAll();
    }

    @Test
    public void determineEditingFailureCancelRequestPublicationPendingWithoutRequestNode() throws Exception {
        final Session session = createMock(Session.class);
        expect(session.getNodeByIdentifier("request-node-id")).andThrow(new RepositoryException("Node with identifier 'request-node-id' does not exist"));
        replayAll();

        final Map<String, Serializable> hints = new HashMap<>();
        final Map<String, Serializable> requests = new HashMap<>();
        final Map<String, Boolean> request = new HashMap<>();
        hints.put("requests", (Serializable) requests);
        requests.put("request-node-id", (Serializable) request);
        request.put("cancelRequest", Boolean.TRUE);

        final Optional<ErrorInfo> errorInfoOptional = hintsInspector.determineEditingFailure(hints, session);
        assertThat("Errorinfo should be present", errorInfoOptional.isPresent());
        if (errorInfoOptional.isPresent()) {
            final ErrorInfo errorInfo = errorInfoOptional.get();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.REQUEST_PENDING));
            assertNull(errorInfo.getParams());
        }

        verifyAll();
    }

    @Test
    public void determineEditingFailureInUseByWithName() throws Exception {
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService securityService = createMock(SecurityService.class);
        final User user = createMock(User.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("inUseBy", "admin");

        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andReturn(securityService);
        expect(securityService.getUser("admin")).andReturn(user);
        expect(user.getFirstName()).andReturn(" John ");
        expect(user.getLastName()).andReturn(" Doe ");
        replayAll();

        final Optional<ErrorInfo> errorInfoOptional = hintsInspector.determineEditingFailure(hints, session);
        assertThat("Errorinfo should be present", errorInfoOptional.isPresent());
        if (errorInfoOptional.isPresent()) {
            final ErrorInfo errorInfo = errorInfoOptional.get();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.OTHER_HOLDER));
            assertThat(errorInfo.getParams().get("userId"), equalTo("admin"));
            assertThat(errorInfo.getParams().get("userName"), equalTo("John Doe"));
        }


        verifyAll();
    }

    @Test
    public void determineEditingFailureInUseByWithoutName() throws Exception {
        final Session session = createMock(Session.class);
        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final Map<String, Serializable> hints = new HashMap<>();
        hints.put("inUseBy", "admin");

        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andThrow(new RepositoryException());
        replayAll();

        final Optional<ErrorInfo> errorInfoOptional = hintsInspector.determineEditingFailure(hints, session);
        assertThat("Errorinfo should be present", errorInfoOptional.isPresent());
        if (errorInfoOptional.isPresent()) {
            final ErrorInfo errorInfo = errorInfoOptional.get();
            assertThat(errorInfo.getReason(), equalTo(ErrorInfo.Reason.OTHER_HOLDER));
            assertThat(errorInfo.getParams().get("userId"), equalTo("admin"));
            assertNull(errorInfo.getParams().get("userName"));
        }

        verifyAll();
    }

}
