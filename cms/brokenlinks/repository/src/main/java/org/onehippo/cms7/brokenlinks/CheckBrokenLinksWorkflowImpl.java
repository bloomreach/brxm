/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.brokenlinks;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.CronExpression;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.ext.InternalWorkflow;
import org.hippoecm.repository.ext.WorkflowImpl;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated CheckBrokenLinksWorkflow has been deprecated.
 *             A repository daemon module ({@link BrokenLinksCheckerDaemonModule}) will register a scheduled task
 *             to check broken links periodically instead.
 */
@Deprecated
public class CheckBrokenLinksWorkflowImpl extends WorkflowImpl implements CheckBrokenLinksWorkflow, InternalWorkflow {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(CheckBrokenLinksWorkflowImpl.class);

    private final Session rootSession;

    @SuppressWarnings("unused") // workflow engine expects this constructor signature
    public CheckBrokenLinksWorkflowImpl(WorkflowContext context, Session userSession, Session rootSession, Node subject) throws RemoteException, RepositoryException {
        this.rootSession = rootSession;
    }

    @Override
    public Map<String, Serializable> hints() {
        Map<String, Serializable> hints = new TreeMap<String,Serializable>();
        hints.put("checkLinks", Boolean.TRUE);
        return hints;
    }

    @Override
    public void checkLinks() throws WorkflowException, RepositoryException, RemoteException {
        final RepositoryJobExecutionContext context = new RepositoryJobExecutionContext(getWorkflowContext().getInternalWorkflowSession(), getWorkflowContext().getWorkflowConfiguration());
        new BrokenLinksCheckingJob().execute(context);
    }

    @Override
    public void checkLinks(CronExpression cronExpression) throws WorkflowException, RepositoryException, RemoteException {
        log.warn("This Broken Links Checker workflow implementation, '{}', has been deprecated! Please use the new daemon module configuration instead!",
                getClass().getName());
        final WorkflowContext cronContext = getWorkflowContext().getWorkflowContext(cronExpression);
        final CheckBrokenLinksWorkflow workflow = (CheckBrokenLinksWorkflow)cronContext.getWorkflow("brokenlinks");
        workflow.checkLinks();
    }
}
