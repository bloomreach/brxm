/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.diagnosis;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.request.Request;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.frontend.util.RequestUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProvidesService(types = DiagnosticsService.class)
public class DiagnosticsDaemonModule extends AbstractReconfigurableDaemonModule {

    private static Logger log = LoggerFactory.getLogger(DiagnosticsDaemonModule.class);

    private static final String ENABLED = "enabled";

    private static final String THRESHOLD_MILLISEC = "thresholdMillisec";

    private static final String DEPTH = "depth";

    private static final String UNIT_THRESHOLD_MILLISEC = "unitThresholdMillisec";

    private static final String ALLOWED_ADDRESSES = "allowedAddresses";

    private static final String ALLOWED_USERS = "allowedUsers";

    private boolean enabled;

    private long thresholdMillisec = -1L;

    private long depth = -1;

    private long unitThresholdMillisec = -1L;

    private Set<String> allowedAddresses;

    private Set<String> allowedUsers;

    private final Object configurationLock = new Object();

    private DiagnosticsService service;

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        synchronized (configurationLock) {
            enabled = JcrUtils.getBooleanProperty(moduleConfig, ENABLED, false);
            thresholdMillisec = JcrUtils.getLongProperty(moduleConfig, THRESHOLD_MILLISEC, -1L);
            depth = JcrUtils.getLongProperty(moduleConfig, DEPTH, -1L);
            unitThresholdMillisec = JcrUtils.getLongProperty(moduleConfig, UNIT_THRESHOLD_MILLISEC, -1L);
            allowedAddresses = new HashSet<>();

            String [] addrValues = JcrUtils.getMultipleStringProperty(moduleConfig, ALLOWED_ADDRESSES, null);

            if (addrValues != null) {
                for (String addrValue : addrValues) {
                    if (StringUtils.isNotBlank(addrValue)) {
                        allowedAddresses.add(StringUtils.trim(addrValue));
                    }
                }
            }

            allowedUsers = new HashSet<>();

            String [] userValues = JcrUtils.getMultipleStringProperty(moduleConfig, ALLOWED_USERS, null);

            if (userValues != null) {
                for (String userValue : userValues) {
                    if (StringUtils.isNotBlank(userValue)) {
                        allowedUsers.add(StringUtils.trim(userValue));
                    }
                }
            }

            log.info("Reconfiguring diagnostic daemon module. enabled: {}, thresholdMillisec: {}, depth: {}, allowedAddresses: {}, allowedUsers: {}",
                     enabled, thresholdMillisec, depth, allowedAddresses, allowedUsers);
        }
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        HippoServiceRegistry.register(service = new DiagnosticsService() {
            @Override
            public boolean isEnabledFor(Request request) {
                if (!enabled) {
                    return false;
                }

                if (!allowedAddresses.isEmpty()) {
                    final String remoteAddr = RequestUtils.getFarthestRemoteAddr(request); 

                    if (remoteAddr == null || !allowedAddresses.contains(remoteAddr)) {
                        return false;
                    }
                }

                if (!allowedUsers.isEmpty()) {
                    final PluginUserSession userSession = PluginUserSession.get();

                    if (userSession == null || userSession.getUserCredentials() == null
                            || !allowedUsers.contains(userSession.getUserCredentials().getUsername())) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public long getThresholdMillisec() {
                return thresholdMillisec;
            }

            @Override
            public int getDepth() {
                return (int) depth;
            }

            @Override
            public long getUnitThresholdMillisec() {
                return unitThresholdMillisec;
            }
        }, DiagnosticsService.class);
    }

    @Override
    protected void doShutdown() {
        HippoServiceRegistry.unregister(service, DiagnosticsService.class);
    }

}
