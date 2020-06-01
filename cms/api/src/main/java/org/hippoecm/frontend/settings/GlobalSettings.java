/*
 *  Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.settings;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.RepositoryUnavailableException;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global settings for all available applications.
 */
public class GlobalSettings {

    private static final Logger log = LoggerFactory.getLogger(GlobalSettings.class);

    private static final String CONFIG_NODE = "/hippo:configuration/hippo:frontend/settings";

    public static IPluginConfig get() {
        try {
            final Session jcrSession = UserSession.get().getJcrSession();
            final Node config = jcrSession.getNode(CONFIG_NODE);
            final JcrNodeModel nodeModel = new JcrNodeModel(config);
            return new JcrPluginConfig(nodeModel);
        } catch (RepositoryUnavailableException e) {
            log.info("Cannot read global settings, there is no repository yet; using empty defaults");
            return new JavaPluginConfig();
        } catch (RepositoryException e) {
            log.info("Cannot read global settings, using empty defaults", e);
            return new JavaPluginConfig();
        }
    }

}
