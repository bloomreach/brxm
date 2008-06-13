/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.tools.migration;

import java.text.SimpleDateFormat;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import nl.hippo.webdav.batchprocessor.PluginConfiguration;

import org.apache.commons.httpclient.HttpClient;

/**
 * Abstract converter implementation for some basic functionalities.
 * Converter plugins should extend this class.
 */
public abstract class AbstractDocumentConverter implements DocumentConverter {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /* Hippo Repository 1.2.x DAV: namespace */
    protected static final String DAV_NAMESPACE = "DAV:";

    /* Hippo Repository 1.2.x hippo namespace */
    protected static final String HIPPO_NAMESPACE = "http://hippo.nl/cms/1.0";

    /* DAV::creationate = 2006-12-18T14:24:53Z */
    protected static final SimpleDateFormat CREATIONDATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /* hipposample::publicationDate = 20040124 */
    protected static final SimpleDateFormat PUBLICATIONDATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    /* TODO: make configurable */
    protected static final String AUTHOR_BASEPATH = "authors";

    /* the initialized jcrSession */
    protected Session jcrSession;

    /* the initialized httpClient */
    private HttpClient httpClient;

    /* hold the configuration from the properties file */
    private PluginConfiguration pluginConfiguration;

    /**
     * Setup the converter
     * @param session
     * @param httpClient
     */
    public final void setup(PluginConfiguration configuration, Session session, HttpClient httpClient) throws RepositoryException {
        setJcrSession(session);
        setHttpClient(httpClient);
        setPluginConfiguration(configuration);
        postSetupHook();
    }

    /**
     * Default translator does just echo's the string
     */
    public String nameTranslate(String name) {
        return name;
    }

    public boolean skipJcrPath(String path) {
        return false;
    }

    /**
     * Get the JCR session
     * @return Session
     */
    public final Session getJcrSession() {
        return this.jcrSession;
    }

    /**
     * Set the JCR session
     * @param session The JCR session
     */
    public final void setJcrSession(Session session) {
        this.jcrSession = session;
    }

    /**
     * Get the initialized httpclient
     * @return the httpClient instance
     */
    public final HttpClient getHttpClient() {
        return this.httpClient;
    }

    /**
     * Set the initialized httpClient, called from the setup
     * @param httpClient
     */
    public final void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Get the plugin configuration
     * @return the configuration
     */
    public final PluginConfiguration getPluginConfiguration() {
        return this.pluginConfiguration;
    }

    /**
     * Set the plugin configuration
     * @param pluginConfiguration
     */
    public final void setPluginConfiguration(PluginConfiguration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

}
