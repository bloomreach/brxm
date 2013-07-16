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

import java.text.ParseException;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.scheduling.RepositoryJobCronTrigger;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobSimpleTrigger;
import org.onehippo.repository.scheduling.RepositoryJobTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;


/**
 * RepositoryScheduler service implementation.
 */
public class RepositorySchedulerImpl implements RepositoryScheduler {

    private static final String CONFIG_NODE_PATH = "/hippo:configuration/hippo:modules/scheduler/hippo:moduleconfig";
    private static final String HIPPOSCHED_JOB = "hipposched:job";
    private static final String HIPPOSCHED_JOBGROUP = "hipposched:jobgroup";

    private final Session session;
    private final Scheduler scheduler;

    RepositorySchedulerImpl(Session session, Scheduler scheduler) {
        this.session = session;
        this.scheduler = scheduler;
    }

    @Override
    public void scheduleJob(final RepositoryJobInfo jobInfo, final RepositoryJobTrigger trigger) throws RepositoryException {
        try {
            scheduler.scheduleJob(createQuartzJobDetail(jobInfo), createQuartzTrigger(trigger));
        } catch (SchedulerException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public void deleteJob(final String jobName, final String groupName) throws RepositoryException {
        synchronized (session) {
            final Node jobNode = getJobNode(jobName, groupName);
            if (jobNode != null) {
                jobNode.remove();
                session.save();
            }
        }
    }

    @Override
    public boolean checkExists(final String jobName, final String groupName) throws RepositoryException {
        return getJobNode(jobName, groupName) != null;
    }

    private Trigger createQuartzTrigger(final RepositoryJobTrigger trigger) throws RepositoryException {
        if (trigger instanceof RepositoryJobCronTrigger) {
            final String cronExpression = ((RepositoryJobCronTrigger) trigger).getCronExpression();
            try {
                return new CronTrigger(trigger.getName(), null, cronExpression);
            } catch (ParseException e) {
                throw new RepositoryException(e);
            }
        }
        if (trigger instanceof RepositoryJobSimpleTrigger) {
            final Date startTime = ((RepositoryJobSimpleTrigger) trigger).getStartTime();
            final int repeatCount = ((RepositoryJobSimpleTrigger) trigger).getRepeatCount();
            final long repeatInterval = ((RepositoryJobSimpleTrigger) trigger).getRepeatInterval();
            if (repeatCount != 0) {
                return new SimpleTrigger(trigger.getName(), startTime, new Date(Long.MAX_VALUE), repeatCount, repeatInterval);
            } else {
                return new SimpleTrigger(trigger.getName(), startTime);
            }
        }
        throw new RepositoryException("Unknown trigger type " + trigger.getClass().getName());
    }

    private JobDetail createQuartzJobDetail(final RepositoryJobInfo jobInfo) throws RepositoryException {
        return new RepositoryJobDetail(newJobNode(jobInfo), jobInfo);
    }

    private Node newJobNode(final RepositoryJobInfo info) throws RepositoryException {
        final String name = info.getName();
        final String group = info.getGroup();
        final Node moduleConfig = session.getNode(CONFIG_NODE_PATH);
        final Node jobGroup;
        if (moduleConfig.hasNode(group)) {
            jobGroup = moduleConfig.getNode(group);
        } else {
            jobGroup = moduleConfig.addNode(group, HIPPOSCHED_JOBGROUP);
        }
        if (jobGroup.hasNode(name)) {
            throw new RepositoryException("A job with name '" + name + "' already exists in job group '" + group + "'");
        }
        return jobGroup.addNode(name, HIPPOSCHED_JOB);
    }

    private Node getJobNode(String jobName, String groupName) throws RepositoryException {
        if (StringUtils.isEmpty(groupName)) {
            groupName = "default";
        }
        final Node moduleConfig = session.getNode(CONFIG_NODE_PATH);
        final Node groupNode = JcrUtils.getNodeIfExists(moduleConfig, groupName);
        if (groupNode != null) {
            return JcrUtils.getNodeIfExists(groupNode, jobName);
        }
        return null;
    }


}
