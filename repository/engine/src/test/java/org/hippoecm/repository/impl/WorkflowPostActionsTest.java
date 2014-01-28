/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import javax.jcr.Node;

import org.easymock.EasyMock;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.standardworkflow.WorkflowEventsWorkflow;
import org.junit.Before;
import org.junit.Test;

/**
 * WorkflowPostActionsBoundMethodTest
 * <P>
 * This tests if the eventsWorkflow is invoked exactly by WorkflowPostActions
 * with setWorkflowCategory(String), setWorkflowMethod(String) and fire(Document).
 * </P>
 * @version $Id$
 */
public class WorkflowPostActionsTest {

    private Node wfSubject;
    private Document document;
    private String workflowCategory = "default";
    private String workflowMethod = "publish";
    private WorkflowEventsWorkflow eventsWorkflow;
    private WorkflowManagerImpl workflowManager;
    private WorkflowDefinition workflowDefinition;

    @Before
    public void setUp() throws Exception {
        // preparing mocks for wfSubject, wfNode and document
        wfSubject = EasyMock.createNiceMock(Node.class);
        final Node wfNode = EasyMock.createNiceMock(Node.class);
        document = EasyMock.createNiceMock(Document.class);
        EasyMock.replay(wfSubject);
        EasyMock.replay(wfNode);
        EasyMock.replay(document);

        eventsWorkflow = EasyMock.createMock(WorkflowEventsWorkflow.class);
        // preparing the mock for workflowManager which just returns the eventsWorkflow
        workflowManager = EasyMock.createNiceMock(WorkflowManagerImpl.class);
        workflowDefinition = new WorkflowDefinition(wfNode);
        EasyMock.expect(workflowManager.createProxiedWorkflow(workflowDefinition, wfSubject)).andReturn(eventsWorkflow).anyTimes();
        EasyMock.replay(workflowManager);

        // expecting method calls: #setWorkflowCategory(String), #setWorkflowMethod(String) and finally #fire(Document) method.
        eventsWorkflow.setWorkflowCategory(workflowCategory);
        EasyMock.expectLastCall();
        eventsWorkflow.setWorkflowMethod(workflowMethod);
        EasyMock.expectLastCall();
        eventsWorkflow.fire(document);
        EasyMock.expectLastCall();
        EasyMock.replay(eventsWorkflow);

    }

    @Test
    public void testWorkflowEventsWorkflowWithWorkflowPostActionsBoundMethod() throws Exception {
        // Now, create a postActionBoundMethod and execute it with the mock document.
        WorkflowPostActionsBoundMethod postAction = new WorkflowPostActionsBoundMethod(workflowManager, wfSubject,
                true, workflowDefinition, workflowCategory, workflowMethod);
        postAction.execute(document);
    }
    @Test

    public void testWorkflowEventsWorkflowWithWorkflowPostActionSimpleQuery() throws Exception {
        // Now, create a postActionBoundMethod and execute it with the mock document.
        WorkflowPostActionSimpleQuery postAction = new WorkflowPostActionSimpleQuery(workflowManager, wfSubject,
                true, workflowDefinition, workflowCategory, workflowMethod);
        postAction.execute(document);
    }
}
