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

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.repository.scheduling.RepositoryJob;
import org.onehippo.repository.scheduling.RepositoryJobExecutionContext;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * Quartz {@link Job} that calls a scheduled {@link RepositoryJob}.
 */
public class RepositoryJobJob implements Job {

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        final RepositoryJobDetail repositoryJobDetail = (RepositoryJobDetail) context.getJobDetail();

        final String repositoryJobClassName = repositoryJobDetail.getRepositoryJobClassName();
        final Map<String, String> attributes = repositoryJobDetail.getAttributes();

        try {
            final Class<? extends RepositoryJob> repositoryJobClass =
                    (Class<? extends RepositoryJob>) Class.forName(repositoryJobClassName);
            final RepositoryJob repositoryJob = repositoryJobClass.newInstance();

            final Scheduler scheduler = context.getScheduler();
            final Session session = (Session) scheduler.getContext().get(Session.class.getName());
            repositoryJob.execute(new RepositoryJobExecutionContext(session, attributes));
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SchedulerException | RepositoryException e) {
            throw new JobExecutionException(e);
        }
    }

}
