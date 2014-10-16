/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.services.autoreload;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JCR-based, thread-safe, reconfigurable configuration of the auto-reload service.
 */
class AutoReloadServiceConfig {

    private static final String CONFIG_ENABLED = "enabled";
    private static final boolean DEFAULT_ENABLED = true;

    private static final Logger log = LoggerFactory.getLogger(AutoReloadServiceConfig.class);

    private AtomicBoolean isEnabled = new AtomicBoolean(DEFAULT_ENABLED);

    void reconfigure(final Node config) throws RepositoryException {
        isEnabled.set(JcrUtils.getBooleanProperty(config, CONFIG_ENABLED, DEFAULT_ENABLED));
        log.info("Reconfigured auto-reload service: enabled=" + isEnabled);
    }

    public boolean isEnabled() {
        return isEnabled.get();
    }
}
