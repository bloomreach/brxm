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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.modules.AbstractReconfigurableSchedulingDaemonModule;
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
public class BrokenLinksCheckerDaemonModule extends AbstractReconfigurableSchedulingDaemonModule {

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
    private static final String CRON_JOB_GROUP_PARAM_PROP = "cronJobGroup";

    @Override
    protected boolean isSchedulerEnabled(Node moduleConfig) throws RepositoryException {
        final Node moduleConfigNode = getNonHandleModuleConfigurationNode(moduleConfig);
        return JcrUtils.getBooleanProperty(moduleConfigNode, ENABLED_PARAM_PROP, Boolean.FALSE);
    }

    @Override
    protected RepositoryJobInfo getRepositoryJobInfo(Node moduleConfig) throws RepositoryException {
        final Node moduleConfigNode = getNonHandleModuleConfigurationNode(moduleConfig);

        String jobName = StringUtils.defaultIfBlank(JcrUtils.getStringProperty(moduleConfigNode, CRON_JOB_NAME_PARAM_PROP, null), BrokenLinksCheckingJob.class.getName());
        String jobGroup = StringUtils.defaultIfBlank(JcrUtils.getStringProperty(moduleConfigNode, CRON_JOB_GROUP_PARAM_PROP, null), DEFAULT_GROUP);

        RepositoryJobInfo jobInfo = new RepositoryJobInfo(jobName, jobGroup, BrokenLinksCheckingJob.class);

        for (Map.Entry<String, String> entry : getModuleConfigurationParametersMap(moduleConfigNode).entrySet()) {
            jobInfo.setAttribute(entry.getKey(), entry.getValue());
        }

        return jobInfo;
    }

    @Override
    protected RepositoryJobTrigger getRepositoryJobTrigger(Node moduleConfig, RepositoryJobInfo jobInfo) throws RepositoryException {
        final Node moduleConfigNode = getNonHandleModuleConfigurationNode(moduleConfig);

        String cronExpr = JcrUtils.getStringProperty(moduleConfigNode, CRON_EXPRESSION_PARAM_PROP, null);

        if (StringUtils.isNotBlank(cronExpr)) {
            final RepositoryJobTrigger jobTrigger = new RepositoryJobCronTrigger(jobInfo.getGroup() + "-" + jobInfo.getName() + "-trigger", cronExpr);
            return jobTrigger;
        }

        return null;
    }

    /**
     * Returns the _Broken_Link_Checker_Specific_ default module configuration node unless the node is type of hippo:handle.
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
                    } else if (prop.getType() == PropertyType.LONG) {
                        params.put(prop.getName(), Long.toString(prop.getLong()));
                    }
                }
            }
        }

        return params;
    }
}
