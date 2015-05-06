/*
 *  Copyright 2013-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.scheduling;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Captures information about a repository job.
 * <p>
 *  Jobs have names and are grouped. A job must have a unique name within a given group.
 *  Jobs are triggered by {@link RepositoryJobTrigger}s. Jobs can have multiple such triggers
 *  associated with them.
 * </p>
 */
public class RepositoryJobInfo {

    private final String name;
    private final String group;
    private final Class<? extends RepositoryJob> jobClass;
    private final Map<String, String> attributes = new LinkedHashMap<>();

    /**
     * Create a job within the default group.
     *
     * @param name  the name of the job.
     * @param jobClass  the class of the job to run.
     */
    public RepositoryJobInfo(final String name, final Class<? extends RepositoryJob> jobClass) {
        this(name, "default", jobClass);
    }

    /**
     * Create a job.
     *
     * @param name the name of the job.
     * @param group the jobs group.
     * @param jobClass  the class of the job to run
     */
    public RepositoryJobInfo(final String name, final String group, final Class<? extends RepositoryJob> jobClass) {
        this.name = name;
        this.group = group;
        this.jobClass = jobClass;
    }

    /**
     * @return the name of this job.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the group name of this job.
     */
    public String getGroup() {
        return group;
    }

    /**
     * @return the class of the job to run.
     */
    public Class<? extends RepositoryJob> getJobClass() {
        return jobClass;
    }

    /**
     * Set the value of an attribute. Attributes will be available to the {@link RepositoryJob}
     * during execution via the {@link RepositoryJobExecutionContext}
     */
    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    /**
     * @return the value of an attribute by name.
     */
    public String getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * @return all the attributes that were added to this info.
     */
    public Collection<String> getAttributeNames() {
        return Collections.unmodifiableCollection(attributes.keySet());
    }

    /**
     * Override the way the job node is created.
     *
     * @param session JCR session with which to create a new job node.
     * @return  a newly created node representing the job,
     * or <code>null</code> to let the system create one.
     */
    public Node createNode(final Session session) throws RepositoryException {
        return null;
    }

}
