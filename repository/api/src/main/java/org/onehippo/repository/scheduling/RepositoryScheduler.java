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
package org.onehippo.repository.scheduling;

import javax.jcr.RepositoryException;

import org.onehippo.cms7.services.SingletonService;

/**
 * Repository service for scheduling jobs.
 */
@SingletonService
public interface RepositoryScheduler {

    /**
     * Schedule a job.
     *
     * @param jobInfo  information about the job to schedule.
     * @param trigger  when to schedule the job.
     * @throws RepositoryException  if an error occurs while trying to schedule the job.
     */
    public void scheduleJob(RepositoryJobInfo jobInfo, RepositoryJobTrigger trigger) throws RepositoryException;

    /**
     * Removes a job and its associated triggers.
     *
     * @param jobName  name of the job.
     * @param groupName  name of the group, may be null to indicate the default group.
     * @throws RepositoryException  if an error occurs while trying to remove the job.
     */
    public void deleteJob(String jobName, String groupName) throws RepositoryException;

    /**
     * Removes a job and its associated triggers.
     *
     * @param jobIdentifier  name of the job.
     * @throws RepositoryException  if an error occurs while trying to remove the job.
     */
    public void deleteJob(String jobIdentifier) throws RepositoryException;

    /**
     * Return whether a job already exists.
     *
     * @param jobName  name of the job.
     * @param groupName  name of the the group, may be null to indicate the default group.
     * @return  whether the job exists or not
     * @throws RepositoryException  if an error occurs while checking for the existence of the job.
     */
    public boolean checkExists(String jobName, String groupName) throws RepositoryException;

    /**
     * Trigger the job (execute it now) identified by {@code jobName} and {@code groupName}.
     *
     * @param jobName  name of the job
     * @param groupName  name of the group, may be null to indicate the default group.
     * @throws RepositoryException
     */
    public void executeJob(String jobName, String groupName) throws RepositoryException;

    /**
     * Trigger the job (execute it now) identified by {@code jobIdentifier}.
     *
     * @param jobIdentifier  name of the job
     * @throws RepositoryException
     */
    public void executeJob(String jobIdentifier) throws RepositoryException;

}
