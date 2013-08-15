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
package org.onehippo.cms7.brokenlinks;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.RequiresService;
import org.onehippo.repository.scheduling.RepositoryJobCronTrigger;
import org.onehippo.repository.scheduling.RepositoryJobInfo;
import org.onehippo.repository.scheduling.RepositoryJobTrigger;
import org.onehippo.repository.scheduling.RepositoryScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BrokenLinksCheckerDaemonModule
 * <P>
 * A daemon module implementation to register/unregister a scheduled job to check broken links periodically.
 * This implementation reads its module configuration and it registers a scheduled task (job)
 * if the module configuration has 'enabled' property set to true and a proper cron expression.
 * Also, this implementation listens to the module configuration change, so it can start/stop/change the
 * scheduled task on module configuration changes.
 * </P>
 */
@RequiresService(types = { RepositoryScheduler.class })
public class BrokenLinksCheckerDaemonModule extends AbstractReconfigurableDaemonModule {

    private static Logger log = LoggerFactory.getLogger(BrokenLinksCheckerDaemonModule.class);

    /**
     * Flag property name whether or not the scheduled job should be enabled.
     */
    private static final String ENABLED_PARAM_PROP = "enabled";

    /**
     * Cron expression property name for the job scheduling.
     */
    private static final String CRON_EXPRESSION_PARAM_PROP = "cronExpression";

    /**
     * Cron job name. If not specified, the FQCN of {@link BrokenLinksCheckingJob} is used.
     */
    private static final String CRON_JOB_NAME_PARAM_PROP = "cronJobName";

    /**
     * Cron job group. If not specified, the default group is "default".
     */
    private static final String CRON_JOB_GROUP_PARAM_PROP = "cronJobName";

    private RepositoryJobInfo brokenLinksCheckingJobInfo;

    private boolean enabled;
    private String cronExpression;
    private String cronJobName;
    private String cronJobGroup;

    public boolean isEnabled() {
        return enabled;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public String getCronJobName() {
        return cronJobName;
    }

    public String getCronJobGroup() {
        return cronJobGroup;
    }

    @Override
    protected void doConfigure(Node moduleConfig) throws RepositoryException {
        final Node moduleConfigNode = getNonHandleModuleConfigurationNode(moduleConfig);
        enabled = JcrUtils.getBooleanProperty(moduleConfigNode, ENABLED_PARAM_PROP, Boolean.FALSE);
        cronExpression = JcrUtils.getStringProperty(moduleConfigNode, CRON_EXPRESSION_PARAM_PROP, null);

        cronJobName = JcrUtils.getStringProperty(moduleConfigNode, CRON_JOB_NAME_PARAM_PROP, null);

        if (StringUtils.isBlank(cronJobName)) {
            cronJobName = BrokenLinksCheckingJob.class.getName();
        }

        cronJobGroup = JcrUtils.getStringProperty(moduleConfigNode, CRON_JOB_GROUP_PARAM_PROP, null);

        if (StringUtils.isBlank(cronJobGroup)) {
            cronJobGroup = "default";
        }
    }

    @Override
    protected void doInitialize(Session session) throws RepositoryException {
        final Node moduleConfig = JcrUtils.getNodeIfExists(moduleConfigPath, session);
        scheduleJob(moduleConfig);
    }

    @Override
    protected void onConfigurationChange(final Node moduleConfig) throws RepositoryException {
        try {
            synchronized (this) {
                super.onConfigurationChange(moduleConfig);
                deleteScheduledJob();
                scheduleJob(moduleConfig);
            }
        } catch (RepositoryException e) {
            log.error("Failed to reconfigure broken links checker.", e);
        }
    }

    @Override
    protected void doShutdown() {
    }

    private void deleteScheduledJob() {
        if (brokenLinksCheckingJobInfo == null) {
            return;
        }

        try {
            final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);
            repositoryScheduler.deleteJob(brokenLinksCheckingJobInfo.getName(), brokenLinksCheckingJobInfo.getGroup());
            log.info("Deleted the scheduled job: '{}'.", brokenLinksCheckingJobInfo.getName());
        } catch (RepositoryException e) {
            log.error("Failed to delete job: " + brokenLinksCheckingJobInfo, e);
        }

        brokenLinksCheckingJobInfo = null;
    }

    private void scheduleJob(Node moduleConfig) {
        if (!isEnabled()) {
            log.info("The broken links checker is not enabled in the module configuration. Skipping broken links checker job scheduling.");
            return;
        }

        if (StringUtils.isBlank(getCronExpression())) {
            log.warn("The cron expression for broken links checker module is blank: '{}'. Skipping broken links checker job scheduling.", getCronExpression());
            return;
        }

        try {
            final RepositoryScheduler repositoryScheduler = HippoServiceRegistry.getService(RepositoryScheduler.class);

            if (repositoryScheduler.checkExists(getCronJobName(), getCronJobGroup())) {
                return;
            }

            RepositoryJobInfo tempJobInfo = new RepositoryJobInfo(getCronJobName(), getCronJobGroup(), BrokenLinksCheckingJob.class);

            for (Map.Entry<String, String> entry : getModuleConfigurationParametersMap(moduleConfig).entrySet()) {
                tempJobInfo.setAttribute(entry.getKey(), entry.getValue());
            }

            final RepositoryJobTrigger brokenLinksCheckingJobTrigger =
                    new RepositoryJobCronTrigger(tempJobInfo.getGroup() + "-" + tempJobInfo.getName() + "-trigger", getCronExpression());
            repositoryScheduler.scheduleJob(tempJobInfo, brokenLinksCheckingJobTrigger);
            brokenLinksCheckingJobInfo = tempJobInfo;
            log.info("Scheduled a job: '{}:{}'.", tempJobInfo.getGroup(), tempJobInfo.getName());
        } catch (RepositoryException e) {
            log.error("Failed to scheudle a job.", e);
        }
    }

    /**
     * Returns the default module configuration node unless the node is type of hippo:handle.
     * If it is hippo:handle, returns the child node having the same node name for backward compatibilty.
     * @return
     * @throws RepositoryException
     */
    private Node getNonHandleModuleConfigurationNode(Node moduleConfig) throws RepositoryException {
        Node moduleConfigNode = moduleConfig;

        // for backward compatibility, the /hippo:configuration/hippo:modules/brokenlinks/hippo:moduleconfig is handle node,
        // and all the configuration parameters are stored in the child node of the handle node.
        if (moduleConfigNode != null && moduleConfigNode.isNodeType(HippoNodeType.NT_HANDLE) && moduleConfigNode.hasNode(moduleConfigNode.getName())) {
            moduleConfigNode = moduleConfigNode.getNode(moduleConfigNode.getName());
        }

        return moduleConfigNode;
    }

    private Map<String, String> getModuleConfigurationParametersMap(Node moduleConfig) throws RepositoryException {
        Map<String, String> params = new HashMap<String, String>();

        final Node moduleConfigNode = getNonHandleModuleConfigurationNode(moduleConfig);

        if (moduleConfigNode != null) {
            for (PropertyIterator pit = moduleConfigNode.getProperties(); pit.hasNext(); ) {
                Property prop = pit.nextProperty();

                if (!prop.isMultiple()) {
                    if (prop.getType() == PropertyType.STRING) {
                        params.put(prop.getName(), prop.getString());
                    } else if (prop.getType() == PropertyType.BOOLEAN) {
                        params.put(prop.getName(), Boolean.toString(prop.getBoolean()));
                    }
                }
            }
        }

        return params;
    }
}
