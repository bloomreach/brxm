/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.logging;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hippoecm.repository.api.DocumentWorkflowAction;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.impl.WorkflowLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.*;

public class WorkflowLoggerTest {

    private HippoEventBus eventBus;
    private WorkflowLogger workflowLogger;
    private Session session;
    private Capture<HippoWorkflowEvent> captured;

    @Before
    public void createService() throws RepositoryException {
        eventBus = createNiceMock(HippoEventBus.class);
        HippoServiceRegistry.registerService(eventBus, HippoEventBus.class);
        session = createNiceMock(Session.class);
        workflowLogger = new WorkflowLogger(session);

        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService securityService = createMock(SecurityService.class);
        final User user = createMock(User.class);
        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andReturn(securityService);
        expect(securityService.hasUser(EasyMock.anyObject(String.class))).andReturn(true);
        expect(securityService.getUser(EasyMock.anyObject(String.class))).andReturn(user);
        expect(user.isSystemUser()).andReturn(false);

        captured = newCapture();
        eventBus.post(EasyMock.capture(captured));

        replay(eventBus, session, workspace, securityService, user);
    }

    @After
    public void removeService() {
        HippoServiceRegistry.unregisterService(eventBus, HippoEventBus.class);
    }

    @Test
    public void testEventIsPostedToEventBus() throws Exception {

        workflowLogger.logWorkflowStep("userName", "className", "methodName", null, "returnValue", null,
                "subjectPath", "interaction", "interactionId", "category", "workflowName", null);

        verify(eventBus, session);

        final HippoWorkflowEvent event = captured.getValue();
        assertEquals("repository", event.application());
        assertEquals("workflow", event.category());
        assertEquals("userName", event.user());
        assertEquals("className", event.className());
        assertEquals("methodName", event.methodName());
        assertEquals("returnValue", event.returnValue());
        assertEquals("subjectPath", event.subjectPath());
        assertEquals("interaction", event.interaction());
        assertEquals("interactionId", event.interactionId());
        assertEquals("category", event.workflowCategory());
        assertEquals("workflowName", event.workflowName());
    }

    @Test
    public void by_default_methodName_is_the_logged_action() {
        workflowLogger.logWorkflowStep("userName", "className", "foo", null, "returnValue", null,
                "subjectPath", "interaction", "interactionId", "category", "workflowName", null);

        verify(eventBus, session);

        final HippoWorkflowEvent event = captured.getValue();
        assertEquals("foo", event.action());
    }

    @Test
    public void if_methodName_is_triggerAction_the_args_are_inspected_for_ActionAware_agument() {
        workflowLogger.logWorkflowStep("userName", "className", "triggerAction", new Object[]{DocumentWorkflowAction.unlock()}, "returnValue", null,
                "subjectPath", "interaction", "interactionId", "category", "workflowName", null);
        verify(eventBus, session);

        final HippoWorkflowEvent event = captured.getValue();
        assertEquals("unlock", event.action());
    }

    @Test
    public void if_methodName_is_not_triggerAction_the_ActionAware_agument_is_not_used() {
        workflowLogger.logWorkflowStep("userName", "className", "notTriggerAction", new Object[]{DocumentWorkflowAction.unlock()}, "returnValue", null,
                "subjectPath", "interaction", "interactionId", "category", "workflowName", null);
        verify(eventBus, session);

        final HippoWorkflowEvent event = captured.getValue();
        assertEquals("notTriggerAction", event.action());
    }
}
