/*
 *  Copyright 2012 Hippo.
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

import org.hippoecm.repository.ext.WorkflowInvocation;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

public class WorkflowJobDetail extends JobDetail {

    private static final String DOCUMENT_KEY = "document";
    private static final String INVOCATION_KEY = "invocation";

    private final String jobRequestPath;
    private final String jobRequestIdentifier;

    public WorkflowJobDetail(Node request, WorkflowInvocation invocation) throws RepositoryException {
        super(request.getIdentifier(), WorkflowJob.class);
        jobRequestPath = request.getPath();
        jobRequestIdentifier = request.getIdentifier();
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(INVOCATION_KEY, invocation);
        jobDataMap.put(DOCUMENT_KEY, invocation.getSubject().getIdentifier());
        setJobDataMap(jobDataMap);
    }

    public String getJobRequestPath() {
        return jobRequestPath;
    }

    public String getJobRequestIdentifier() {
        return jobRequestIdentifier;
    }

    public String getSubjectIdentifier() {
        return (String) getJobDataMap().get(DOCUMENT_KEY);
    }

    public WorkflowInvocation getInvocation() {
        return (WorkflowInvocation) getJobDataMap().get(INVOCATION_KEY);
    }

}
