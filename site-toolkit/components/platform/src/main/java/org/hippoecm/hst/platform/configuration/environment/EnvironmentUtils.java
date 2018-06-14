/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.environment;

import java.util.Optional;

import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.services.environment.EnvironmentSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_HOSTS;
import static org.hippoecm.hst.environment.EnvironmentParameters.ACTIVE_HOST_GROUP_PARAM;
import static org.hippoecm.hst.site.HstServices.getComponentManager;
import static org.onehippo.cms7.services.HippoServiceRegistry.getService;

public class EnvironmentUtils {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentUtils.class);

    private EnvironmentUtils() {}

    static Optional<String> activeHostGroupOptional;

    static String getActiveHostGroup() {
        if (activeHostGroupOptional != null) {
            return activeHostGroupOptional.orElse(null);
        }
        synchronized (EnvironmentUtils.class) {
            if (activeHostGroupOptional != null) {
                return activeHostGroupOptional.orElse(null);
            }
            final EnvironmentSettings environmentSettings = getService(EnvironmentSettings.class);
            if (environmentSettings == null) {
                log.info("EnvironmentSettings are not available");
                activeHostGroupOptional = Optional.empty();
                return null;
            }

            final String activeHostGroup = environmentSettings.get(ACTIVE_HOST_GROUP_PARAM);
            if (activeHostGroup == null) {
                log.info("EnvironmentSettings does not provide an active host group value for '{}'.", ACTIVE_HOST_GROUP_PARAM);
                activeHostGroupOptional = Optional.empty();
                return null;
            }
            activeHostGroupOptional = Optional.of(activeHostGroup);
            return activeHostGroup;
        }
    }


    static String getActiveHostGroupPath() {
        final String activeHostGroup = getActiveHostGroup();
        if (activeHostGroup == null) {
            return null;
        }
        return getHstRootPath() + "/" + NODENAME_HST_HOSTS + "/" + activeHostGroup;
    }

    static String getHstRootPath() {
        return HstServices.getComponentManager().getContainerConfiguration().getString("hst.configuration.rootPath");
    }

}
