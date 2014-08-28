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
import java.util.Collection;
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
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_ATTRIBUTE_NAMES;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_ATTRIBUTE_VALUES;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_JOBGROUP;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_REPOSITORY_JOB;
import static org.hippoecm.repository.quartz.HippoSchedJcrConstants.HIPPOSCHED_REPOSITORY_JOB_CLASS;


/**
 * RepositoryScheduler service implementation.
 */
public class RepositorySchedulerImpl implements RepositoryScheduler {

    private final Session session;
    private final JCRScheduler scheduler;

    RepositorySchedulerImpl(Session session, JCRScheduler scheduler) {
        this.session = session;
        this.scheduler = scheduler;
    }

    @Override
    public void scheduleJob(final RepositoryJobInfo jobInfo, final RepositoryJobTrigger trigger) throws RepositoryException {
        synchronized (session) {
            try {
                scheduler.scheduleJob(createQuartzJobDetail(jobInfo), createQuartzTrigger(trigger));
            } catch (SchedulerException e) {
                throw new RepositoryException(e);
            }
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
    public void deleteJob(final String jobIdentifier) throws RepositoryException {
        synchronized (session) {
            final Node jobNode = session.getNodeByIdentifier(jobIdentifier);
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

    @Override
    public void executeJob(final String jobName, String groupName) throws RepositoryException {
        final Node jobNode = getJobNode(jobName, groupName);
        if (jobNode == null) {
            throw new RepositoryException("No such job: " + getGroupName(groupName) + "/" + jobName);
        }
        executeJob(jobNode.getIdentifier());
    }

    @Override
    public void executeJob(final String jobIdentifier) throws RepositoryException {
        try {
            final RepositoryJobDetail jobDetail = (RepositoryJobDetail) scheduler.getJobDetail(new JobKey(jobIdentifier));
            final Job job = jobDetail.getJobClass().newInstance();
            job.execute(new JobExecutionContextImpl(scheduler, jobDetail));
        } catch (SchedulerException | InstantiationException | IllegalAccessException e) {
            throw new RepositoryException(e);
        }
    }

    private Trigger createQuartzTrigger(final RepositoryJobTrigger trigger) throws RepositoryException {
        if (trigger instanceof RepositoryJobCronTrigger) {
            final String cronExpression = ((RepositoryJobCronTrigger) trigger).getCronExpression();
            try {
                return new CronTriggerImpl(trigger.getName(), null, cronExpression);
            } catch (ParseException e) {
                throw new RepositoryException(e);
            }
        }
        if (trigger instanceof RepositoryJobSimpleTrigger) {
            final Date startTime = ((RepositoryJobSimpleTrigger) trigger).getStartTime();
            final int repeatCount = ((RepositoryJobSimpleTrigger) trigger).getRepeatCount();
            final long repeatInterval = ((RepositoryJobSimpleTrigger) trigger).getRepeatInterval();
            if (repeatCount != 0) {
                return new SimpleTriggerImpl(trigger.getName(), startTime, null, repeatCount, repeatInterval);
            } else {
                return new SimpleTriggerImpl(trigger.getName(), startTime);
            }
        }
        throw new RepositoryException("Unknown trigger type " + trigger.getClass().getName());
    }

    private JobDetail createQuartzJobDetail(final RepositoryJobInfo jobInfo) throws RepositoryException {
        Node jobNode = jobInfo.createNode(session);
        if (jobNode == null) {
            jobNode = newJobNode(jobInfo);
        }
        jobNode.setProperty(HIPPOSCHED_REPOSITORY_JOB_CLASS, jobInfo.getJobClass().getName());
        jobNode.setProperty(HIPPOSCHED_ATTRIBUTE_NAMES, attributeNames(jobInfo));
        jobNode.setProperty(HIPPOSCHED_ATTRIBUTE_VALUES, attributeValues(jobInfo));

        return new RepositoryJobDetail(jobNode, jobInfo);
    }

    private Node newJobNode(final RepositoryJobInfo info) throws RepositoryException {
        final String name = info.getName();
        final String group = info.getGroup();
        final Node moduleConfig = session.getNode(SchedulerModule.getModuleConfigPath());
        final Node jobGroup;
        if (moduleConfig.hasNode(group)) {
            jobGroup = moduleConfig.getNode(group);
        } else {
            jobGroup = moduleConfig.addNode(group, HIPPOSCHED_JOBGROUP);
        }
        if (jobGroup.hasNode(name)) {
            throw new RepositoryException("A job with name '" + name + "' already exists in job group '" + group + "'");
        }
        return jobGroup.addNode(name, HIPPOSCHED_REPOSITORY_JOB);
    }

    private Node getJobNode(final String jobName, final String groupName) throws RepositoryException {
        synchronized (session) {
            final Node moduleConfig = session.getNode(SchedulerModule.getModuleConfigPath());
            final Node groupNode = JcrUtils.getNodeIfExists(moduleConfig, getGroupName(groupName));
            if (groupNode != null) {
                return JcrUtils.getNodeIfExists(groupNode, jobName);
            }
        }
        return null;
    }

    private String getGroupName(final String groupName) {
        return StringUtils.isEmpty(groupName) ? "default" : groupName;
    }

    private String[] attributeNames(final RepositoryJobInfo info) {
        final Collection<String> attributeNames = info.getAttributeNames();
        return attributeNames.toArray(new String[attributeNames.size()]);
    }

    private String[] attributeValues(final RepositoryJobInfo info) {
        final Collection<String> attributeNames = info.getAttributeNames();
        final String[] attributeValues = new String[attributeNames.size()];
        int i = 0;
        for (String attributeName : attributeNames) {
            attributeValues[i] = info.getAttribute(attributeName);
            i++;
        }
        return attributeValues;
    }

}
