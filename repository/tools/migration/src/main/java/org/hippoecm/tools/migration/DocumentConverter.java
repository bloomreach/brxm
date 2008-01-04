/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.tools.migration;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import nl.hippo.webdav.batchprocessor.OperationOnDeletedNodeException;
import nl.hippo.webdav.batchprocessor.PluginConfiguration;
import nl.hippo.webdav.batchprocessor.ProcessingException;

import org.apache.commons.httpclient.HttpClient;

/**
 * Interface for the converter plugins.
 */
public interface DocumentConverter {

    /**
     * Setup and configure the converter
     */
    void setup(PluginConfiguration config, Session session, HttpClient httpClient) throws RepositoryException;

    /**
     * The postSetupHook is called after setup and can be used to initialize the
     * converter implementation
     */
    void postSetupHook() throws RepositoryException;

    /**
     * Convert each webdav node to a jcr node
     */
    void convertNodeToJCR(nl.hippo.webdav.batchprocessor.Node webdavNode, String nodeName, javax.jcr.Node parent)
            throws RepositoryException, ProcessingException, OperationOnDeletedNodeException, IOException;

    /**
     * Get the JCR session
     * @return Session
     */
    Session getJcrSession();

    /**
     * Set the JCR session
     * @param session The JCR session
     */
    void setJcrSession(Session session);

    /**
     * Get the initialized httpclient
     * @return the httpClient instance
     */
    HttpClient getHttpClient();

    /**
     * Set the initialized httpClient, called from the setup
     * @param httpClient
     */
    void setHttpClient(HttpClient httpClient);
}
