/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.cms7.hst.ga.config;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleAnalyticsConfigurationImpl implements GoogleAnalyticsConfiguration {

    private static Logger log = LoggerFactory.getLogger(GoogleAnalyticsConfigurationImpl.class);

    private static final String CONFIG_NODE_PATH = "/hippo:configuration/hippo:modules/googleAnalyticsConfiguration/hippo:moduleconfig";
    private static final String ACCOUNT_ID_PROPERTY_NAME = "hippogoogleanalytics:accountId";

    private Repository repository;
    private Credentials credentials;
    
    private String accountId;

    private volatile boolean initialized = false;

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    @Override
    public String getAccountId() {
        if (!initialized) {
            initialize();
        }
        return accountId;
    }
    
    void invalidate() {
        initialized = false;
    }
    
    private synchronized void initialize() {
        if (initialized) {
            return;
        }
        Session session = null;
        try {
            session = getSession();
            Node node = session.getNode(CONFIG_NODE_PATH);
            accountId = node.getProperty(ACCOUNT_ID_PROPERTY_NAME).getString();
        }
        catch (RepositoryException e) {
            log.error("Failed to load google analytics configuration: " + e.getClass().getName() + " : " + e.getMessage());
        }
        finally {
            if (session != null) {
                session.logout();
            }
            initialized = true;
        }
    }

    private Session getSession() throws RepositoryException {
        if (credentials == null) {
            return repository.login();
        } else {
            return repository.login(credentials);
        }
    }
}
