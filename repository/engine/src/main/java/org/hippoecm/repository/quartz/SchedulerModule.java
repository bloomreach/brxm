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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchedulerModule implements DaemonModule {

    private static final Logger log = LoggerFactory.getLogger(SchedulerModule.class);

    static Session session = null;
    static JCRScheduler scheduler = null;
    static SchedulerFactory schedFactory = null;

    public SchedulerModule() {
        log.debug("<init>");
    }

    public void initialize(Session session) {
        log.debug("initializing");
        SchedulerModule.session = session;
        Properties properties = new Properties();
        try {
            properties.put("org.quartz.scheduler.instanceName","Hippo JCR Quartz Job Scheduler");
            properties.put("org.quartz.scheduler.instanceName","HJCRQJS");
            properties.put("org.quartz.scheduler.instanceId","AUTO");
            properties.put("org.quartz.scheduler.skipUpdateCheck", "true");
            properties.put("org.quartz.threadPool.class","org.quartz.simpl.SimpleThreadPool");
            properties.put("org.quartz.threadPool.threadCount","2");
            properties.put("org.quartz.threadPool.threadPriority","5");
            properties.put("org.quartz.jobStore.class",JCRJobStore.class.getName());
            properties.put("org.quartz.jobStore.isClustered","true");
            schedFactory = new SchedulerFactory(session);
            schedFactory.initialize(properties);
            scheduler = (JCRScheduler) schedFactory.getScheduler();
            scheduler.start();
        } catch (SchedulerException ex) {
            log.error(ex.getClass().getName()+": "+ex.getMessage(), ex);
        }
    }

    static JCRScheduler getScheduler(Session session) {
        return new JCRScheduler(scheduler, session);
    }

    public void shutdown() {
        if(scheduler != null) {
            scheduler.shutdown(true);
        }
        session.logout();
    }

    public static class SchedulerFactory extends StdSchedulerFactory {
        private Properties props;
        private Session session;

        public SchedulerFactory(Session session) throws SchedulerException {
            this.session = session;
        }

        public SchedulerFactory(Properties props, Session session) throws SchedulerException {
            super(props);
            this.props = new Properties(props);
            this.session = session;
        }

        public SchedulerFactory(SchedulerFactory factory, Session session) throws SchedulerException {
            super(factory.props);
            this.session = session;
        }

        @Override
        public void initialize(Properties props) throws SchedulerException {
            this.props = new Properties(props);
            super.initialize(props);
        }

        @Override
        protected Scheduler instantiate(QuartzSchedulerResources rsrcs, QuartzScheduler qs) {
            JCRSchedulingContext schedCtxt = new JCRSchedulingContext(session);
            schedCtxt.setInstanceId(rsrcs.getInstanceId());
            schedCtxt.setSession(session);
            Scheduler scheduler = new JCRScheduler(qs, schedCtxt);
            return scheduler;
        }
    }
}
