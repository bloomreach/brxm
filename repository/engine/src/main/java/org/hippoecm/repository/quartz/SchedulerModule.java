/*
 *  Copyright 2008 Hippo.
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

import java.util.Properties;

import javax.jcr.Session;

import org.hippoecm.repository.ext.DaemonModule;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
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
        SCHEDULER_FACTORY_PROPERTIES.put(JcrSchedulerFactory.PROP_JOB_STORE_ISCLUSTERED,  "true");
    }

    private static SchedulerModule instance;

    private Session session;
    private JCRScheduler scheduler = null;

    public void initialize(Session session) {
        this.session = session;
        try {
            final JcrSchedulerFactory schedFactory = new JcrSchedulerFactory(SCHEDULER_FACTORY_PROPERTIES);
            scheduler = (JCRScheduler) schedFactory.getScheduler();
            scheduler.start();
        } catch (SchedulerException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        }
        instance = this;
    }

    static Scheduler getScheduler(Session session) {
        return new JCRScheduler(instance.scheduler, session);
    }

    static Session getSession() {
        return instance.session;
    }

    public void shutdown() {
        if(scheduler != null) {
            scheduler.shutdown(true);
        }
        session.logout();
    }

    private class JcrSchedulerFactory extends StdSchedulerFactory {

        private static final String PROP_JOB_STORE_ISCLUSTERED = "org.quartz.jobStore.isClustered";
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
