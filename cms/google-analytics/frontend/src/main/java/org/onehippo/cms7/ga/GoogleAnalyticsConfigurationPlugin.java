/*
 *  Copyright 2011-2012 Hippo.
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
package org.onehippo.cms7.ga;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleAnalyticsConfigurationPlugin extends Plugin implements IGoogleAnalyticsConfigurationService {

    private static final long serialVersionUID = 1L;
    
    private static final Logger log = LoggerFactory.getLogger(GoogleAnalyticsConfigurationPlugin.class);
    
    private static final String CONFIG_NODE_PATH = "/hippo:configuration/hippo:modules/googleAnalyticsConfiguration/hippo:moduleconfig";
    private static final String TABLE_ID_PROPERTY_NAME = "hippogoogleanalytics:tableId";
    private static final String USERNAME_PROPERTY_NAME = "hippogoogleanalytics:username";
    private static final String PASSWORD_PROPERTY_NAME = "hippogoogleanalytics:password";

    private String username;
    private String password;
    private String tableId;

    public GoogleAnalyticsConfigurationPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        loadConfiguration();
        context.registerService(this, IGoogleAnalyticsConfigurationService.class.getName());
    }
    
    private void loadConfiguration() {
        UserSession userSession = UserSession.get();
        Session session = userSession.getJcrSession();
        try {
            Node node = session.getNode(CONFIG_NODE_PATH);
            this.tableId = node.getProperty(TABLE_ID_PROPERTY_NAME).getString();
            this.username = node.getProperty(USERNAME_PROPERTY_NAME).getString();
            this.password = node.getProperty(PASSWORD_PROPERTY_NAME).getString();
        } catch (RepositoryException e) {
            log.error("Failed to load google analytics configuration: " + e.toString());
        }
    }

    @Override
    public String getTableId() {
        return tableId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

}
