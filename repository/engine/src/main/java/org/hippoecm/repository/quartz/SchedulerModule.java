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

import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.ConfigurableDaemonModule;
import org.onehippo.repository.modules.DaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.core.QuartzScheduler;
import org.quartz.core.QuartzSchedulerResources;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.SimpleThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProvidesService(types = { RepositoryScheduler.class } )
public class SchedulerModule implements DaemonModule, ConfigurableDaemonModule {

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

    private static SchedulerModule instance;

    private Session session;
    private JCRScheduler scheduler = null;
    private RepositoryScheduler service;
    private String moduleConfigPath;

    private static boolean isEnabled() {
        return !Boolean.getBoolean("hippo.scheduler.disabled");
    }

    @Override
    public void configure(final Node moduleConfig) throws RepositoryException {
        moduleConfigPath = moduleConfig.getPath();
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
            return;
        }
        service = new RepositorySchedulerImpl(session, scheduler);
        HippoServiceRegistry.registerService(service, RepositoryScheduler.class);
    }

    static Session getSession() {
        return instance.session;
    }

    static String getModuleConfigPath() {
        return instance.moduleConfigPath;
    }

    @Override
    public void shutdown() {
        if (service != null) {
            HippoServiceRegistry.unregisterService(service, RepositoryScheduler.class);
        }
        if(scheduler != null) {
            scheduler.shutdown(true);
        }
        session.logout();
    }

    private class JcrSchedulerFactory extends StdSchedulerFactory {

        private static final String PROP_THREAD_POOL_THREADCOUNT = "org.quartz.threadPool.threadCount";
        private static final String PROP_THREAD_POOL_THREADPRIORITY = "org.quartz.threadPool.threadPriority";

        public JcrSchedulerFactory(Properties properties) throws SchedulerException {
            super(properties);
        }

        @Override
        protected Scheduler instantiate(QuartzSchedulerResources rcs, QuartzScheduler qs) {
            try {
                qs.getSchedulerContext().put(Session.class.getName(), session);
                return new JCRScheduler(qs);
            } catch (SchedulerException e) {
                throw new RuntimeException("Unexpected exception creating scheduler", e);
            }
        }
    }

}
