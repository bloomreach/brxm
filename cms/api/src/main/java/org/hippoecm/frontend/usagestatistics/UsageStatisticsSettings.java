/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.usagestatistics;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.settings.GlobalSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsageStatisticsSettings {

    public static final String CONFIG_SEND_USAGE_STATISTICS_TO_HIPPO = "send.usage.statistics.to.hippo";
    public static final boolean DEFAULT_SEND_USAGE_STATISTICS_TO_HIPPO = true;

    public static final String SYSPROP_SEND_USAGE_STATISTICS_TO_HIPPO = CONFIG_SEND_USAGE_STATISTICS_TO_HIPPO;

    private static final Logger log = LoggerFactory.getLogger(UsageStatisticsSettings.class);

    private final boolean isEnabled;

    private UsageStatisticsSettings(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public static UsageStatisticsSettings get() {
        return new UsageStatisticsSettings(checkEnabled());
    }

    private static boolean checkEnabled() {
        boolean result;

        // first check the system property, so sending of usage statistics can always be disabled from the command line
        final String systemPropValue = System.getProperty(SYSPROP_SEND_USAGE_STATISTICS_TO_HIPPO);
        if (systemPropValue != null) {
            result = Boolean.parseBoolean(systemPropValue);
            log.info("Sending usage statistics to Hippo: {} (set by system property '{}')", result, SYSPROP_SEND_USAGE_STATISTICS_TO_HIPPO);
        } else {
            // next, check the global setting {@link #CONFIG_SEND_USAGE_STATISTICS_TO_HIPPO}
            final IPluginConfig globalSettings = GlobalSettings.get();
            result = globalSettings.getAsBoolean(CONFIG_SEND_USAGE_STATISTICS_TO_HIPPO, DEFAULT_SEND_USAGE_STATISTICS_TO_HIPPO);
            log.info("Sending usage statistics to Hippo: {} (set by repository configuration property '{}')", result, CONFIG_SEND_USAGE_STATISTICS_TO_HIPPO);
        }
        return result;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
