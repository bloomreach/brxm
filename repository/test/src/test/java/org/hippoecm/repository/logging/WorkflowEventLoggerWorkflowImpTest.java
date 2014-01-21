/*
 *  Copyright 2012-2014 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.standardworkflow.WorkflowEventLoggerWorkflowImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.eventbus.HippoEventBus;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;

import javax.jcr.Session;

import static junit.framework.Assert.assertEquals;
import static org.easymock.EasyMock.*;

public class WorkflowEventLoggerWorkflowImpTest {

    private HippoEventBus eventBus;

    @Before
    public void createService() {
        eventBus = createNiceMock(HippoEventBus.class);
        HippoServiceRegistry.registerService(eventBus, HippoEventBus.class);
    }

    @After
    public void removeService() {
        HippoServiceRegistry.unregisterService(eventBus, HippoEventBus.class);
    }

    @Test
    public void testEventIsPostedToEventBus() throws Exception {
        final Session session = createNiceMock(Session.class);
        WorkflowEventLoggerWorkflowImpl eventLogger = new WorkflowEventLoggerWorkflowImpl(null, session, null);

        final HippoWorkspace workspace = createMock(HippoWorkspace.class);
        final SecurityService securityService = createMock(SecurityService.class);
        final User user = createMock(User.class);
        expect(session.getWorkspace()).andReturn(workspace);
        expect(workspace.getSecurityService()).andReturn(securityService);
        expect(securityService.hasUser(EasyMock.anyObject(String.class))).andReturn(true);
        expect(securityService.getUser(EasyMock.anyObject(String.class))).andReturn(user);
        expect(user.isSystemUser()).andReturn(false);

        final Capture<HippoEvent> captured = new Capture<HippoEvent>();
        eventBus.post(EasyMock.capture(captured));
        replay(eventBus, session, workspace, securityService, user);

        eventLogger.logEvent("userName", "className", "methodName");

        verify(eventBus, session);

        final HippoEvent event = captured.getValue();
        assertEquals("repository", event.application());
        assertEquals("workflow", event.category());
        assertEquals("userName", event.user());
        assertEquals("className", event.get("className"));
        assertEquals("methodName", event.get("methodName"));
    }

}
