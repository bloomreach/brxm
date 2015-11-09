/*
 * Copyright 2015-2015 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.modules.AbstractReconfigurableDaemonModule;
import org.onehippo.repository.modules.ProvidesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ProvidesService(types = DiagnosisService.class)
public class DiagnosisDaemonModule extends AbstractReconfigurableDaemonModule {

    private static Logger log = LoggerFactory.getLogger(DiagnosisDaemonModule.class);

    private static final String ENABLED = "enabled";

    private static final String THRESHOLD_MILLISEC = "thresholdMillisec";

    private static final String DEPTH = "depth";

    private static final String ALLOWED_ADDRESSES = "allowedAddresses";

    private boolean enabled;

    private long thresholdMillisec = -1L;

    private long depth = -1;

    private Set<String> allowedAddresses;

    private final Object configurationLock = new Object();

    private DiagnosisService service;

    @Override
    protected void doConfigure(final Node moduleConfig) throws RepositoryException {
        synchronized (configurationLock) {
            enabled = JcrUtils.getBooleanProperty(moduleConfig, ENABLED, false);
            thresholdMillisec = JcrUtils.getLongProperty(moduleConfig, THRESHOLD_MILLISEC, -1L);
            depth = JcrUtils.getLongProperty(moduleConfig, DEPTH, -1L);
            allowedAddresses = new HashSet<>();

            String [] addrValues = JcrUtils.getMultipleStringProperty(moduleConfig, ALLOWED_ADDRESSES, null);

            if (addrValues != null) {
                for (String addrValue : addrValues) {
                    if (StringUtils.isNotBlank(addrValue)) {
                        allowedAddresses.add(StringUtils.trim(addrValue));
                    }
                }
            }

            log.info("Reconfiguring diagnostic daemon module. enabled: {}, thresholdMillisec: {}, depth: {}, allowedAddresses: {}",
                     enabled, thresholdMillisec, depth, allowedAddresses);
        }
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        HippoServiceRegistry.registerService(service = new DiagnosisService() {
            @Override
            public boolean isEnabledFor(String clientAddress) {
                if (!enabled) {
                    return false;
                }

                if (!allowedAddresses.isEmpty() && !allowedAddresses.contains(clientAddress)) {
                    return false;
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
        }, DiagnosisService.class);
    }

    @Override
    protected void doShutdown() {
        HippoServiceRegistry.unregisterService(service, DiagnosisService.class);
    }

}
