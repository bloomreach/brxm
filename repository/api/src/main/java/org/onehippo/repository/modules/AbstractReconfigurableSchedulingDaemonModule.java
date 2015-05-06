/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.modules;

import java.lang.reflect.Method;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class extending {@link AbstractReconfigurableDaemonModule} to allow
 * easy (un)registering of a scheduled job with some default optimization when dealing with JCR events.
 * <p>
 * Derived classes are responsible for creating/returning a valid job info and job trigger
 * by implementing {@link #getRepositoryJobInfo(Node)} and {@link #getRepositoryJobTrigger(Node, RepositoryJobInfo)}.
 * Also, they can return false in {@link #isSchedulerEnabled(Node)} when the scheduled job should be disabled.
 * </p>
 * @deprecated deprecated since 2.28.00, register a repository job with the repository scheduler directly using configuration
 */
@Deprecated
public abstract class AbstractReconfigurableSchedulingDaemonModule extends AbstractReconfigurableDaemonModule {

    private static final Logger log = LoggerFactory.getLogger(AbstractReconfigurableSchedulingDaemonModule.class);

    protected static final String CONFIG_LOCK_ISDEEP_PROPERTY = "jcr:lockIsDeep";

    protected static final String CONFIG_LOCK_OWNER = "jcr:lockOwner";

    protected static final String DEFAULT_GROUP = "default";

    private static final String JACKRABBIT_EVENT_FQCN = "org.apache.jackrabbit.api.observation.JackrabbitEvent";

    private static Method JACKRABBIT_EVENT_IS_EXTERNAL_METHOD;

    private RepositoryJobInfo jobInfo;

    @Override
    protected void doConfigure(Node moduleConfig) throws RepositoryException {
    }

    @Override
    protected void doInitialize(Session session) throws RepositoryException {
        final Node moduleConfig = JcrUtils.getNodeIfExists(moduleConfigPath, session);
        scheduleJob(moduleConfig);
    }

    @Override
    protected void doShutdown() {
    }

    @Override
    protected boolean isReconfigureEvent(Event event) throws RepositoryException {
        String eventPath = event.getPath();
        return !isExternalJackrabbitEvent(event) && !eventPath.endsWith(CONFIG_LOCK_ISDEEP_PROPERTY) && !eventPath.endsWith(CONFIG_LOCK_OWNER);
    }

    /*
     * Because hippo-repository-api jar can be deployed in the shared lib path whereas jackrabbit-api jar can be deployed in a webapp classpath,
     * let's use reflection here to check if the event is an external jackrabbit event.
     */
    private boolean isExternalJackrabbitEvent(Event event) {
        try {
            Class<?> jackrabbitEventClazz = Thread.currentThread().getContextClassLoader().loadClass(JACKRABBIT_EVENT_FQCN);

            if (jackrabbitEventClazz.isAssignableFrom(event.getClass())) {
                if (JACKRABBIT_EVENT_IS_EXTERNAL_METHOD == null) {
                    JACKRABBIT_EVENT_IS_EXTERNAL_METHOD = jackrabbitEventClazz.getDeclaredMethod("isExternal", new Class<?> [] {});
                }

                return (Boolean) JACKRABBIT_EVENT_IS_EXTERNAL_METHOD.invoke(event, (Object[]) null);
            }
        } catch (Exception e) {
            log.error("Failed to invoke org.apache.jackrabbit.api.observation.JackrabbitEvent#isExternal().", e);
        }

        return false;
    }

    @Override
    protected void onConfigurationChange(final Node moduleConfig) throws RepositoryException {
        try {
            synchronized (this) {
                super.onConfigurationChange(moduleConfig);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        unscheduleJob();
                        scheduleJob(moduleConfig);
                    }
                }).start();
            }
        } catch (RepositoryException e) {
            log.warn("Failed to reconfigure the module, " + getClass(), e);
        }
    }

    protected void unscheduleJob() {
        if (jobInfo == null) {
            return;
        }

        try {
            final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);

            if (repositoryScheduler == null) {
                throw new IllegalStateException("Failed to get the repository scheduler service when unscheduling the job.");
            }

            String jobGroup = jobInfo.getGroup();

            if (jobGroup == null) {
                jobGroup = DEFAULT_GROUP;
            }

            repositoryScheduler.deleteJob(jobInfo.getName(), jobGroup);
        } catch (RepositoryException e) {
            log.warn("Failed to unschedule job: " + jobInfo, e);
        }

        jobInfo = null;
    }

    protected void scheduleJob(Node moduleConfig) {
        try {
            if (!isSchedulerEnabled(moduleConfig)) {
                log.info("The module, '{}', is not enabled to register the scheduled job. Skipping broken links checker job scheduling.", getClass());
                return;
            }
        } catch (RepositoryException e) {
            log.error("Failed to check if the scheduler enabled in " + getClass(), e);
            return;
        }

        try {
            jobInfo = getRepositoryJobInfo(moduleConfig);
        } catch (RepositoryException e) {
            log.error("Failed to get repository job info in " + getClass(), e);
        }

        if (jobInfo == null) {
            log.info("{} returned a null jobInfo. Skipping registering a scheduled job.", getClass());
            return;
        }

        try {
            final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);

            if (repositoryScheduler == null) {
                log.error("Failed to get the repository scheduler service. Probably the implementation class doesn't include class annotation, '@RequiresService(types = { RepositoryScheduler.class }'.");
                return;
            }

            String jobGroup = jobInfo.getGroup();

            if (jobGroup == null) {
                jobGroup = DEFAULT_GROUP;
            }

            if (repositoryScheduler.checkExists(jobInfo.getName(), jobGroup)) {
                return;
            }

            final RepositoryJobTrigger trigger = getRepositoryJobTrigger(moduleConfig, jobInfo);

            if (trigger == null) {
                log.warn("{} returned a null job trigger. Skipping registering a scheduled job.", getClass());
                jobInfo = null;
                return;
            }

            repositoryScheduler.scheduleJob(jobInfo, trigger);
        } catch (RepositoryException e) {
            log.error("Failed to schedule a job: " + jobInfo, e);
        }
    }

    /**
     * Returns true only when the scheduling daemon is enabled. By default, it is enabled.
     *
     * @param moduleConfig
     */
    protected boolean isSchedulerEnabled(Node moduleConfig) throws RepositoryException {
        return true;
    }

    /**
     * Returns the scheduled task job info.
     * @param moduleConfig
     */
    protected abstract RepositoryJobInfo getRepositoryJobInfo(Node moduleConfig) throws RepositoryException;

    /**
     * Returns the scheduled task job trigger.
     * @param moduleConfig
     * @param jobInfo
     * @throws RepositoryException
     */
    protected abstract RepositoryJobTrigger getRepositoryJobTrigger(Node moduleConfig, RepositoryJobInfo jobInfo) throws RepositoryException;

}
