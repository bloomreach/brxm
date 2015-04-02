/*
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
package org.onehippo.repository.documentworkflow.task;

import java.rmi.RemoteException;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobSimpleTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_METHOD_NAME;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_SUBJECT_ID;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_WORKFLOW_JOB;

/**
 * Custom workflow task for scheduling a publish or depublish workflow operation for a document.
 */
public class ScheduleWorkflowTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ScheduleWorkflowTask.class);

    private String type;
    private Date targetDate;

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Date getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(Date targetDate) {
        this.targetDate = targetDate;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        DocumentHandle dh = getDocumentHandle();

        if (getType() == null || !("publish".equals(getType()) || "depublish".equals(getType()))) {
            throw new WorkflowException("Unknown or undefined ScheduledWorkflowAction: "+getType());
        }
        if (targetDate == null) {
            throw new WorkflowException("ScheduledWorkflowAction: no target date specified");
        }

        final RepositoryScheduler scheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
        scheduler.scheduleJob(new WorkflowJobInfo(dh.getHandle().getIdentifier(), getType()),
                new RepositoryJobSimpleTrigger("default", targetDate));

        return null;
    }

    private static class WorkflowJobInfo extends RepositoryJobInfo {

        private final String handleIdentifier;

        public WorkflowJobInfo(final String handleIdentifier, final String methodName) {
            super(HippoNodeType.HIPPO_REQUEST, WorkflowJob.class);
            this.handleIdentifier = handleIdentifier;
            setAttribute(HIPPOSCHED_SUBJECT_ID, handleIdentifier);
            setAttribute(HIPPOSCHED_METHOD_NAME, methodName);
        }

        @Override
        public Node createNode(final Session session) throws RepositoryException {
            final Node handleNode = session.getNodeByIdentifier(handleIdentifier);
            return handleNode.addNode(HippoNodeType.HIPPO_REQUEST, HIPPOSCHED_WORKFLOW_JOB);
        }
    }

    public static class WorkflowJob implements RepositoryJob {

        @Override
        public void execute(final RepositoryJobExecutionContext context) throws RepositoryException {
            Session session = null;
            String methodName = null;
            String subjectPath = null;
            try {
                session = context.createSession(new SimpleCredentials("workflowuser", new char[] {}));
                final String subjectId = context.getAttribute(HIPPOSCHED_SUBJECT_ID);
                methodName = context.getAttribute(HIPPOSCHED_METHOD_NAME);
                final Node subject = session.getNodeByIdentifier(subjectId);
                subjectPath = subject.getPath();

                final DocumentWorkflow workflow = (DocumentWorkflow) getWorkflowManager(session).getWorkflow("default", subject);
                if ("publish".equals(methodName)) {
                    workflow.publish();
                } else {
                    workflow.depublish();
                }
            } catch (RemoteException | WorkflowException | RepositoryException e) {
                log.error("Execution of scheduled workflow operation {} on {} failed", methodName, subjectPath,  e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        }

        private static WorkflowManager getWorkflowManager(final Session session) throws RepositoryException {
            return ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
        }

    }

}
