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

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.WorkflowInvocation;
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModule;

public class SchedulerWorkflowModule implements WorkflowInvocationHandlerModule {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    Date timestamp;

    public SchedulerWorkflowModule(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Object submit(WorkflowManager manager, WorkflowInvocation invocation) {
        try {
            if(SchedulerModule.log.isDebugEnabled()) {
                SchedulerModule.log.debug("Storing scheduled workflow {}",invocation.toString());
            }
            Scheduler scheduler = SchedulerModule.getScheduler(invocation.getSubject().getSession());
            Node subject = invocation.getSubject();
            Node handle = subject.getParent();
            if(handle.isNodeType("mix:versionable") && !handle.isCheckedOut()) {
                handle.checkout();
            }
            Node request = handle.addNode("hippo:request","hipposched:job");
            request.addMixin("mix:referenceable");
            String detail = request.getPath();
            JobDetail jobDetail = new JobDetail(detail, null, WorkflowJob.class);
            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put("invocation", invocation);
            jobDataMap.put("document", invocation.getSubject().getUUID());
            jobDetail.setJobDataMap(jobDataMap);
            SimpleTrigger trigger = new SimpleTrigger(detail+"/default", null, timestamp);
            trigger.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW);
            scheduler.scheduleJob(jobDetail, trigger);
        } catch (RepositoryException ex) {
            SchedulerModule.log.error("failure storing scheduled workflow", ex);
        } catch (SchedulerException ex) {
            SchedulerModule.log.error("failure storing scheduled workflow", ex);
        }
        return null;
    }
}
