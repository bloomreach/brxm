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
package org.hippoecm.repository.quartz;

import java.text.ParseException;
import java.util.Date;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.ext.DaemonModule;
import org.onehippo.cms7.services.HippoServiceRegistry;
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
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.SimpleThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerModule implements DaemonModule {

    private static final Logger log = LoggerFactory.getLogger(SchedulerModule.class);

    private static final Properties SCHEDULER_FACTORY_PROPERTIES = new Properties();
    static {
        SCHEDULER_FACTORY_PROPERTIES.put(JcrSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "Hippo JCR Quartz Job Scheduler");
        SCHEDULER_FACTORY_PROPERTIES.put(JcrSchedulerFactory.PROP_SCHED_INSTANCE_ID, "AUTO");
        SCHEDULER_FACTORY_PROPERTIES.put(JcrSchedulerFactory.PROP_SCHED_SKIP_UPDATE_CHECK, "true");
        SCHEDULER_FACTORY_PROPERTIES.put(JcrSchedulerFactory.PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getName());
        SCHEDULER_FACTORY_PROPERTIES.put(JcrSchedulerFactory.PROP_THREAD_POOL_THREADCOUNT, "2");
        SCHEDULER_FACTORY_PROPERTIES.put(JcrSchedulerFactory.PROP_THREAD_POOL_THREADPRIORITY, "5");
        SCHEDULER_FACTORY_PROPERTIES.put(JcrSchedulerFactory.PROP_JOB_STORE_CLASS, JCRJobStore.class.getName());
    }
    private static final String CONFIG_NODE_PATH = "/hippo:configuration/hippo:modules/scheduler/hippo:moduleconfig";
    private static final String HIPPOSCHED_JOB = "hipposched:job";
    private static final String HIPPOSCHED_JOBGROUP = "hipposched:jobgroup";

    private static SchedulerModule instance;

    private Session session;
    private JCRScheduler scheduler = null;


    private static boolean isEnabled() {
        return !Boolean.getBoolean("hippo.scheduler.disabled");
    }

    @Override
    public void initialize(Session session) throws RepositoryException {
        instance = this;
        this.session = session;
        try {
            final JcrSchedulerFactory schedFactory = new JcrSchedulerFactory(SCHEDULER_FACTORY_PROPERTIES);
            scheduler = (JCRScheduler) schedFactory.getScheduler();
            if (isEnabled()) {
                scheduler.start();
            } else {
                log.info("Hippo scheduler was disabled by hippo.scheduler.disabled property, " +
                        "scheduled actions will not be executed by this cluster node");
            }
        } catch (SchedulerException e) {
            log.error("Failed to initialize quartz scheduler", e);
        }
        HippoServiceRegistry.registerService(new RepositoryScheduler() {
            @Override
            public void scheduleJob(final RepositoryJobInfo jobInfo, final RepositoryJobTrigger trigger)
                    throws RepositoryException {
                try {
                    scheduler.scheduleJob(createQuartzJobDetail(jobInfo), createQuartzTrigger(trigger));
                } catch (SchedulerException e) {
                    throw new RepositoryException(e);
                }
            }
        }, RepositoryScheduler.class);
    }

    public static Scheduler getScheduler(Session session) {
        return new JCRScheduler(instance.scheduler, session);
    }

    // FIXME: can we get rid of this static with quartz upgrade?
    static Session getSession() {
        return instance.session;
    }

    @Override
    public void shutdown() {
        if(scheduler != null) {
            scheduler.shutdown(true);
        }
        session.logout();
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
                return new SimpleTrigger(trigger.getName(), null, startTime, repeatCount, repeatInterval);
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

    private class JcrSchedulerFactory extends StdSchedulerFactory {

        private static final String PROP_THREAD_POOL_THREADCOUNT = "org.quartz.threadPool.threadCount";
        private static final String PROP_THREAD_POOL_THREADPRIORITY = "org.quartz.threadPool.threadPriority";

        public JcrSchedulerFactory(Properties properties) throws SchedulerException {
            super(properties);
        }

        @Override
        protected Scheduler instantiate(QuartzSchedulerResources rcs, QuartzScheduler qs) {
            JCRSchedulingContext schedCtxt = new JCRSchedulingContext(session);
            schedCtxt.setInstanceId(rcs.getInstanceId());
            return new JCRScheduler(qs, schedCtxt);
        }
    }

}
