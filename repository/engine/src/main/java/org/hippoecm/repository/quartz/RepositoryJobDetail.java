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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.quartz.impl.JobDetailImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_ATTRIBUTE_NAMES;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_ATTRIBUTE_VALUES;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_REPOSITORY_JOB_CLASS;

class RepositoryJobDetail extends JobDetailImpl {

    private static final Logger log = LoggerFactory.getLogger(RepositoryJobDetail.class);

    private final String repositoryJobClassName;
    private final Map<String, String> attributes = new HashMap<String, String>();

    RepositoryJobDetail(final Node jobNode, final RepositoryJobInfo info) throws RepositoryException {
        super(jobNode.getIdentifier(), RepositoryJobJob.class);
        this.repositoryJobClassName = info.getJobClass().getName();
        for (String attributeName : info.getAttributeNames()) {
            attributes.put(attributeName, info.getAttribute(attributeName));
        }
    }

    RepositoryJobDetail(final Node jobNode) throws RepositoryException {
        super(jobNode.getIdentifier(), RepositoryJobJob.class);
        repositoryJobClassName = jobNode.getProperty(HIPPOSCHED_REPOSITORY_JOB_CLASS).getString();
        if (jobNode.hasProperty(HIPPOSCHED_ATTRIBUTE_NAMES)) {
            final Value[] nameValues = jobNode.getProperty(HIPPOSCHED_ATTRIBUTE_NAMES).getValues();
            if (nameValues.length > 0 && !jobNode.hasProperty(HIPPOSCHED_ATTRIBUTE_VALUES)) {
                log.warn("Invalid state of job node at " + jobNode.getPath() + ": property '"
                        + HIPPOSCHED_ATTRIBUTE_NAMES + "' has values but property '"
                        + HIPPOSCHED_ATTRIBUTE_VALUES + "' is not present");
                return;
            }
            final Value[] valueValues = jobNode.getProperty(HIPPOSCHED_ATTRIBUTE_VALUES).getValues();
            if (nameValues.length != valueValues.length) {
                log.warn("Invalid state of job node at " + jobNode.getPath() + ": property '"
                        + HIPPOSCHED_ATTRIBUTE_NAMES + "' must have the same number of values as property '"
                        + HIPPOSCHED_ATTRIBUTE_VALUES + "'");
                return;
            }
            for (int i = 0; i < nameValues.length; i++) {
                attributes.put(nameValues[i].getString(), valueValues[i].getString());
            }

        }
    }

    public String getIdentifier() {
        return getName();
    }

    public String getRepositoryJobClassName() {
        return repositoryJobClassName;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

}
