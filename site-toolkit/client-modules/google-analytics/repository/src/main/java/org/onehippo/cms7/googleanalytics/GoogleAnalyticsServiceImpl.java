/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.googleanalytics;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.googleanalytics.GoogleAnalyticsService;
import org.onehippo.repository.modules.DaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleAnalyticsServiceImpl implements GoogleAnalyticsService, DaemonModule {

    private static final Logger log = LoggerFactory.getLogger(GoogleAnalyticsServiceImpl.class);

    private static final String CONFIG_NODE_PATH = "/hippo:configuration/hippo:modules/googleAnalyticsConfiguration/hippo:moduleconfig";
    private static final String TABLE_ID_PROPERTY_PATH = CONFIG_NODE_PATH + "/hippogoogleanalytics:tableId";
    private static final String USERNAME_PROPERTY_PATH = CONFIG_NODE_PATH + "/hippogoogleanalytics:username";
    private static final String PASSWORD_PROPERTY_PATH = CONFIG_NODE_PATH + "/hippogoogleanalytics:password";
    private static final String ACCOUNT_ID_PROPERTY_PATH = CONFIG_NODE_PATH + "/hippogoogleanalytics:accountId";

    private Session session;

    private volatile String userName;
    private volatile String password;
    private volatile String accountId;
    private volatile String tableId;

    @Override
    public void initialize(final Session session) throws RepositoryException {
        this.session = session;
        session.getWorkspace().getObservationManager().addEventListener(
                new EventListener() {
                    @Override
                    public void onEvent(final EventIterator events) {
                        userName = null;
                        password = null;
                        accountId = null;
                        tableId = null;
                    }
                },
                Event.PROPERTY_CHANGED | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED | Event.NODE_ADDED | Event.NODE_REMOVED,
                CONFIG_NODE_PATH, true, null, null, false
        );
        HippoServiceRegistry.registerService(this, GoogleAnalyticsService.class);
    }

    @Override
    public void shutdown() {
        HippoServiceRegistry.unregisterService(this, GoogleAnalyticsService.class);
    }

    @Override
    public String getAccountId() {
        if (accountId == null) {
            accountId = getStringProperty(session, ACCOUNT_ID_PROPERTY_PATH);
        }
        return accountId;
    }

    @Override
    public String getTableId() {
        if (tableId == null) {
            tableId = getStringProperty(session, TABLE_ID_PROPERTY_PATH);
        }
        return tableId;
    }

    @Override
    public String getUserName() {
        if (userName == null) {
            userName = getStringProperty(session, USERNAME_PROPERTY_PATH);
        }
        return userName;
    }

    @Override
    public String getPassword() {
        if (password == null) {
            password = getStringProperty(session, PASSWORD_PROPERTY_PATH);
        }
        return password;
    }

    private synchronized static String getStringProperty(Session session, String propertyName) {
        String value = null;
        try {
            value = JcrUtils.getStringProperty(session, propertyName, null);
            if (value == null) {
                log.warn("Property not found: " + propertyName);
            }
        } catch (RepositoryException e) {
            log.error("Failed to get property " + propertyName, e);
        }
        return value;

    }
}
