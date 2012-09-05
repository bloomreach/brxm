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
package org.onehippo.cms7.brokenlinks;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visitor context, immutable
 */
public class CheckExternalBrokenLinksConfig {

    private static Logger log = LoggerFactory.getLogger(CheckExternalBrokenLinksConfig.class);

    private static final String CONFIG_START_PATH = "startPath";
    private static final String CONFIG_NR_HTTP_THREADS = "nrHttpThreads";
    private static final String CONFIG_SOCKET_TIMEOUT = "socketTimeout";
    private static final String CONFIG_CONNECTION_TIMEOUT = "connectionTimeout";
    private static final String CONFIG_DOCUMENTVISITORCLASS = "documentVisitorClass";

    private static final String DEFAULT_START_PATH = "/content/documents";
    private static final int DEFAULT_NR_HTTP_THREADS = 10;
    private static final int DEFAULT_SOCKET_TIMEOUT = 10000;
    private static final int DEFAULT_CONNECT_TIMEOUT = 10000;

    private String startPath;
    private int nrOfHttpThreads;
    private int socketTimeout;
    private int connectionTimeout;

    public CheckExternalBrokenLinksConfig(Map map) {
        startPath = getString(map, CONFIG_START_PATH, DEFAULT_START_PATH);
        nrOfHttpThreads = getInteger(map, CONFIG_NR_HTTP_THREADS, DEFAULT_NR_HTTP_THREADS);
        socketTimeout = getInteger(map, CONFIG_SOCKET_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
        connectionTimeout = getInteger(map, CONFIG_CONNECTION_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
        String className = getString(map, CONFIG_DOCUMENTVISITORCLASS, null);
        if (className != null) {
            log.warn("Document visitor classname is not used any more. You can remove the property 'documentVisitorClass'. Ignoring configured documentVisitorClass = '{}'", className);
        }
    }

    public int getNrOfHttpThreads() {
        return nrOfHttpThreads;
    }

    public String getStartPath() {
        return startPath;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    private String getString(Map map, String name, String defaultValue) {
        Object value = map.get(name);
        if (value != null) {
            return value.toString();
        }
        return defaultValue;
    }

    private int getInteger(Map map, String name, int defaultValue) {
        Object value = map.get(name);
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                log.warn("Illegal integer value for '{}': {}", name, value.toString());
            }
        }
        return defaultValue;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("CheckBrokenLinksConfig")
                .append("{startPath='").append(startPath).append('\'')
                .append(", nrOfHttpThreads=").append(nrOfHttpThreads)
                .append(", socketTimeout=").append(socketTimeout)
                .append(", connectionTimeout=").append(connectionTimeout)
                .append('}');
        return sb.toString();
    }
}
