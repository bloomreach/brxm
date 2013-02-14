/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.repository.scheduling.RepositoryJobInfo;


class RepositoryJobDetail extends JCRJobDetail {

    static final String REPOSITORY_JOB_CLASS_KEY = "hipposched:repositoryJobClass";
    static final String HIPPOSCHED_CUSTOM_PREFIX = "hipposched:custom:";

    RepositoryJobDetail(final Node jobNode, final RepositoryJobInfo info) throws RepositoryException {
        super(jobNode, RepositoryJobJob.class);
        getJobDataMap().put(REPOSITORY_JOB_CLASS_KEY, info.getJobClass().getName());
        for (String attributeName : info.getAttributeNames()) {
            getJobDataMap().put(HIPPOSCHED_CUSTOM_PREFIX + attributeName, info.getAttribute(attributeName));
        }
    }

}
