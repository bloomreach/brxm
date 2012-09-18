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
package org.hippoecm.repository.quartz;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.WorkflowInvocation;
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModule;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSchedulerInvocationModule implements WorkflowInvocationHandlerModule {

    private static final Logger log = LoggerFactory.getLogger(SchedulerModule.class);

    public AbstractSchedulerInvocationModule() {
    }

    public Object submit(WorkflowManager manager, WorkflowInvocation invocation) {
        try {
            if(log.isDebugEnabled()) {
                log.debug("Storing scheduled workflow {}",invocation.toString());
            }
            final Scheduler scheduler = SchedulerModule.getScheduler(invocation.getSubject().getSession());
            final Node handle = invocation.getSubject().getParent();
            if(handle.isNodeType("mix:versionable") && !handle.isCheckedOut()) {
                handle.checkout();
            }
            final Node jobRequestNode = handle.addNode("hippo:request", "hipposched:job");
            jobRequestNode.addMixin("mix:referenceable");

            final String triggerName = jobRequestNode.getPath() + "/detail";
            scheduler.scheduleJob(new WorkflowJobDetail(jobRequestNode, invocation), createTrigger(triggerName));
        } catch (RepositoryException ex) {
            log.error("failure storing scheduled workflow", ex);
        } catch (SchedulerException ex) {
            log.error("failure storing scheduled workflow", ex);
        }
        return null;
    }

    protected abstract Trigger createTrigger(String triggerName);

}
