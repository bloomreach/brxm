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
package org.hippoecm.repository.quartz.workflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.ext.WorkflowInvocation;
import org.hippoecm.repository.ext.WorkflowInvocationHandlerModule;
import org.hippoecm.repository.quartz.SchedulerModule;
import org.hippoecm.repository.util.JcrUtils;
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
                log.debug("Storing scheduled workflow for document {}", invocation.getSubject().getPath());
            }
            final Scheduler scheduler = SchedulerModule.getScheduler(invocation.getSubject().getSession());
            if (scheduler != null) {
                final Node handle;
                if (invocation.getSubject().isNodeType(HippoNodeType.NT_HANDLE)) {
                    handle = invocation.getSubject();
                }
                else {
                    handle = invocation.getSubject().getParent();
                }
                JcrUtils.ensureIsCheckedOut(handle);
                final Node requestNode = handle.addNode("hippo:request", "hipposched:workflowjob");
                scheduler.scheduleJob(new WorkflowJobDetail(requestNode, invocation), createTrigger("default"));
            } else {
                log.warn("Scheduler is not available, cannot schedule workflow for document {}", invocation.getSubject().getPath());
            }
        } catch (RepositoryException ex) {
            log.error("Failed to store scheduled workflow operation", ex);
        } catch (SchedulerException ex) {
            log.error("Failed to schedule workflow operation", ex);
        }
        return null;
    }

    protected abstract Trigger createTrigger(String triggerName);

}
