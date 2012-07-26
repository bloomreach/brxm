/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.quartz;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.WorkflowInvocation;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class WorkflowJob implements Job {

    private static final String IMPERSONATED_USER = "workflowuser";
    private static final char[] IMPERSONATED_PASSWORD = new char[0];

    public void execute(JobExecutionContext context) throws JobExecutionException {
        Session impersonated = null;
        try {
            JobDetail jobDetail = context.getJobDetail();
            JobDataMap jobDataMap = jobDetail.getJobDataMap();
            WorkflowInvocation invocation = (WorkflowInvocation)jobDataMap.get("invocation");
            JCRScheduler scheduler = (JCRScheduler)context.getScheduler();
            Session session = ((JCRSchedulingContext)scheduler.ctx).getSession();

            synchronized(session) {
                impersonated = session.impersonate(new SimpleCredentials(IMPERSONATED_USER, IMPERSONATED_PASSWORD));
            }
            String uuid = (String) jobDataMap.get("document");
            invocation.setSubject(impersonated.getNodeByIdentifier(uuid));
            invocation.invoke(impersonated);
            impersonated.save();

        } catch (WorkflowException ex) {
            throw new JobExecutionException(ex.getClass().getName() + ": " + ex.getMessage());
        } catch (RepositoryException ex) {
            throw new JobExecutionException(ex.getClass().getName() + ": " + ex.getMessage());
        } finally {
            if (impersonated != null) {
                impersonated.logout();
            }
        }
    }
}
