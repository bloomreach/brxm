/*
 *  Copyright 2008-2012 Hippo.
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
package org.hippoecm.repository.quartz.workflow;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowInvocation;
import org.hippoecm.repository.quartz.JCRScheduler;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class WorkflowJob implements Job {

    private static final String IMPERSONATED_USER = "workflowuser";
    private static final char[] IMPERSONATED_PASSWORD = new char[0];

    public void execute(JobExecutionContext context) throws JobExecutionException {
        Session workflowSession = null;
        try {
            final JCRScheduler scheduler = (JCRScheduler) context.getScheduler();
            final Session schedulerSession = scheduler.getJCRSchedulingContext().getSession();
            synchronized(schedulerSession) {
                workflowSession = schedulerSession.impersonate(new SimpleCredentials(IMPERSONATED_USER, IMPERSONATED_PASSWORD));
            }

            final WorkflowJobDetail jobDetail = (WorkflowJobDetail) context.getJobDetail();
            final WorkflowInvocation invocation = jobDetail.getInvocation();
            final String subjectIdentifier = jobDetail.getSubjectIdentifier();

            invocation.setSubject(workflowSession.getNodeByIdentifier(subjectIdentifier));
            invocation.invoke(workflowSession);

            workflowSession.save();

        } catch (WorkflowException e) {
            throw new JobExecutionException("Failed to execute workflow job", e);
        } catch (RepositoryException e) {
            throw new JobExecutionException("Failed to execute workflow job", e);
        } finally {
            if (workflowSession != null) {
                workflowSession.logout();
            }
        }
    }
}
