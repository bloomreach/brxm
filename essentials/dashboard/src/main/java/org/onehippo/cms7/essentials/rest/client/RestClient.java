/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest.client;


import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @version "$Id$"
 */
public class RestClient {

    public static final int DEFAULT_CONNECTION_TIMEOUT = 2000;
    public static final int DEFAULT_RECEIVE_TIMEOUT = 2000;
    private static Logger log = LoggerFactory.getLogger(RestClient.class);

    /**
     * e.g. http://localhost:8080/site/restapi
     */
    private final String baseResourceUri;
    private long receiveTimeout = DEFAULT_RECEIVE_TIMEOUT;
    private long connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    public RestClient(String baseResourceUri) {
        this.baseResourceUri = baseResourceUri;
    }

    public RestClient(String baseResourceUri, long receiveTimeout, long connectionTimeout) {
        this.baseResourceUri = baseResourceUri;
        this.connectionTimeout = connectionTimeout;
        this.receiveTimeout = receiveTimeout;
    }

    public String getPluginList() {
        final WebClient client = WebClient.create(baseResourceUri);
        return client.get(String.class);
    }


    public String getJson() {
        final WebClient client = WebClient.create(baseResourceUri);
        setTimeouts(client, connectionTimeout, receiveTimeout);
        try {
            return client.get(String.class);
        } catch (Exception e) {
            log.error("Error requesting remote plugin descriptors for repository: " + baseResourceUri, e);
        }
        return null;
    }


    private void setTimeouts(final WebClient client, final long connectionTimeout, final long receiveTimeout) {
        // TODO: figure out how to set timeout with new JAXRS API
        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        if (receiveTimeout != 0) {
            conduit.getClient().setReceiveTimeout(receiveTimeout);
        }
        if (connectionTimeout != 0) {
            conduit.getClient().setConnectionTimeout(connectionTimeout);
        }
    }
}
