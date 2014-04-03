/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.scxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.commons.scxml2.ActionExecutionContext;
import org.apache.commons.scxml2.Context;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.Evaluator;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.env.SimpleDispatcher;
import org.apache.commons.scxml2.env.SimpleErrorReporter;
import org.apache.commons.scxml2.env.groovy.GroovyContext;
import org.apache.commons.scxml2.env.groovy.GroovyEvaluator;
import org.apache.commons.scxml2.model.ModelException;
import org.apache.commons.scxml2.model.OnEntry;
import org.apache.commons.scxml2.model.State;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.api.WorkflowTask;

/**
 * AbstractWorkflowTaskActionTest
 */
public class AbstractWorkflowTaskActionTest {

    private EventDispatcher evtDispatcher;
    private ErrorReporter errRep;
    private Log appLog;
    ActionExecutionContext exctx;

    private OnEntry onEntry;
    private Context context;

    private AbstractWorkflowTaskAction action;

    @Before
    public void before() throws Exception {
        evtDispatcher = new SimpleDispatcher();
        errRep = new SimpleErrorReporter();
        appLog = new SimpleLog(getClass().getName());

        final State state = new State();
        onEntry = new OnEntry();
        state.addOnEntry(onEntry);

        context = new GroovyContext();
        final Evaluator evaluator = new GroovyEvaluator();
        exctx = EasyMock.createNiceMock(ActionExecutionContext.class);
        EasyMock.expect(exctx.getEventDispatcher()).andReturn(evtDispatcher).anyTimes();
        EasyMock.expect(exctx.getErrorReporter()).andReturn(errRep).anyTimes();
        EasyMock.expect(exctx.getAppLog()).andReturn(appLog).anyTimes();
        EasyMock.expect(exctx.getContext(state)).andReturn(context).anyTimes();
        EasyMock.expect(exctx.getEvaluator()).andReturn(evaluator).anyTimes();
        EasyMock.replay(exctx);
    }

    @Test
    public void testTaskCreationAndInvocation() throws Exception {
        final Object expectedResult = new Object();

        final WorkflowTask workflowTask = EasyMock.createMock(WorkflowTask.class);
        EasyMock.expect(workflowTask.execute()).andReturn(expectedResult).once();
        EasyMock.replay(workflowTask);

        final AtomicInteger initTaskCallCount = new AtomicInteger();
        final AtomicInteger createWorkflowTaskCallCount = new AtomicInteger();
        final AtomicInteger processTaskResultCallCount = new AtomicInteger();

        action = new AbstractWorkflowTaskAction<WorkflowTask>() {
            @Override
            protected void initTask(WorkflowTask task) throws ModelException, SCXMLExpressionException {
                initTaskCallCount.incrementAndGet();
                assertSame(workflowTask, task);
                super.initTask(task);
            }
            @Override
            protected WorkflowTask createWorkflowTask() {
                createWorkflowTaskCallCount.incrementAndGet();
                return workflowTask;
            }
            @Override
            protected void processTaskResult(Object taskResult) {
                processTaskResultCallCount.incrementAndGet();
                assertSame(expectedResult, taskResult);
                getContext().set("eventResult", taskResult);
            }
        };
        action.setParent(onEntry);

        action.execute(exctx);
        assertEquals(1, createWorkflowTaskCallCount.get());
        assertEquals(1, initTaskCallCount.get());
        assertEquals(1, processTaskResultCallCount.get());

        assertTrue(context.has("eventResult"));
        assertSame(expectedResult, context.get("eventResult"));

        EasyMock.verify(workflowTask);
    }

    @Test
    public void testNullResultFromTask() throws Exception {
        final Object expectedResult = null;

        final WorkflowTask workflowTask = EasyMock.createMock(WorkflowTask.class);
        EasyMock.expect(workflowTask.execute()).andReturn(expectedResult).once();
        EasyMock.replay(workflowTask);

        action = new AbstractWorkflowTaskAction() {
            @Override
            protected WorkflowTask createWorkflowTask() {
                return workflowTask;
            }
        };
        action.setParent(onEntry);

        action.execute(exctx);

        assertFalse(context.has("eventResult"));
        assertNull(context.get("eventResult"));

        EasyMock.verify(workflowTask);
    }
}
