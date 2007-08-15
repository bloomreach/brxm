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
package org.hippoecm.repository.jr;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import nl.hippo.webdav.batchprocessor.PluginConfiguration;

import org.apache.commons.httpclient.HttpClient;
import org.apache.webdav.lib.Property;

public abstract class AbstractDocumentConverter implements DocumentConverter {

    /* Hippo Repository 1.2.x DAV: namespace */
    protected static final String DAV_NAMESPACE = "DAV:";

    /* Hippo Repository 1.2.x hippo namespace */
    protected static final String HIPPO_NAMESPACE = "http://hippo.nl/cms/1.0";

    /* DAV::creationate = 2006-12-18T14:24:53Z */
    protected static final SimpleDateFormat CREATIONDATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /* hippo::publicationDate = 20040124 */
    protected static final SimpleDateFormat PUBLICATIONDATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    /* the initialized jcrSession */
    protected Session jcrSession;

    /* the initialized httpClient */
    protected HttpClient httpClient;

    /* hold the configuration from the properties file */
    protected PluginConfiguration pluginConfiguration;

    /**
     * Setup the converter
     * @param session
     * @param httpClient
     */
    public final void setup(PluginConfiguration configuration, Session session, HttpClient httpClient) {
        setJcrSession(session);
        setHttpClient(httpClient);
        setPluginConfiguration(configuration);
        postSetupHook();
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

    //-------------------------------------------------- WebDAV Related methods

    /**
     * Convert a webdav date property to a calendar date
     * @param webdavProperty
     * @param dateFormat
     * @return calender
     */
    protected Calendar getCalendarFromProperty(Property webdavProperty, SimpleDateFormat dateFormat) {
        Date d;
        try {
            d = dateFormat.parse(webdavProperty.getPropertyAsString());
        } catch (java.text.ParseException e) {
            // use now if the date can't be parsed
            d = new Date();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return c;
    }

    //-------------------------------------------------- JCR Related Methods

    /**
     * Find the hippo:id of a nodetype or create a new node and return the id
     * @param name
     * @param nodeType
     * @param baseNode
     * @return the id of the node
     * @throws RepositoryException
     */
    protected long getIdOrCreate(String name, String nodeType, String baseNode) throws RepositoryException {
        long id = getId(name, nodeType);
        // id has contraint >0 
        if (id < 0) {
            id = createIdNode(name, nodeType, baseNode);
        }
        return id;
    }

    /**
     * Find the id of a nodeType
     * @param name
     * @param nodeType
     * @return the id or -1 if the node doesn't exist
     */
    protected long getId(String name, String nodeType) {
        long id = -1;
        String sql = "SELECT hippo:id FROM " + nodeType + " WHERE  hippo:name = '" + name + "'";
        try {
            Query q = getJcrSession().getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
            QueryResult result = q.execute();
            RowIterator it = result.getRows();
            if (it.hasNext()) {
                Value idValue = it.nextRow().getValue("hippo:id");
                if (idValue != null) {
                    id = idValue.getLong();
                }
            }
        } catch (RepositoryException e) {
            System.err.println(e);
        }

        return id;
    }

    /**
     * Create a new node of a specific nodeType
     * @param name
     * @param nodeType
     * @param baseNode
     * @return the id of the new node
     * @throws RepositoryException
     */
    protected long createIdNode(String name, String nodeType, String baseNode) throws RepositoryException {
        long id = 1 + getMaxIdForNodeType(nodeType);

        checkAndCreateStructureNode(baseNode);
        javax.jcr.Node parent = getJcrSession().getRootNode().getNode(baseNode);
        javax.jcr.Node author = parent.addNode(name, nodeType);

        author.setProperty("hippo:id", id);
        author.setProperty("hippo:name", name);

        return id;

    }

    /**
     * Check if a specific (srtucture) node exists and create the node and parent nodes
     * if needed (like mkdir -p)
     * @param nodeName
     * @throws RepositoryException
     */
    protected void checkAndCreateStructureNode(String nodeName) throws RepositoryException {

        try {
            javax.jcr.Node node = (javax.jcr.Node) getJcrSession().getRootNode();
            String currentPath = "";

            StringTokenizer st = new StringTokenizer(nodeName, "/");

            while (st.hasMoreTokens()) {

                String curName = st.nextToken();

                if (curName == null || "".endsWith(curName)) {
                    continue;
                }

                // add node if it doesn't exist
                if (!node.hasNode(curName)) {
                    node.addNode(curName);
                }
                currentPath += "/" + curName;

                // shift to child node 
                node = node.getNode(curName);
            }
            getJcrSession().save();

        } catch (RepositoryException e) {
            System.err.println(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Find the maximum used id for a specific nodeType
     * @param nodeType
     * @return the max used id or 0 if the nodeType doesn't exist
     */
    protected long getMaxIdForNodeType(String nodeType) {
        long id = 0;
        String sql = "SELECT hippo:id FROM " + nodeType + " ORDER BY hippo:id DESC";
        try {
            Query q = getJcrSession().getWorkspace().getQueryManager().createQuery(sql, Query.SQL);
            QueryResult result = q.execute();
            RowIterator it = result.getRows();
            if (it.hasNext()) {
                Value idValue = it.nextRow().getValue("hippo:id");
                if (idValue != null) {
                    id = idValue.getLong();
                }
            }

        } catch (RepositoryException e) {
            System.err.println(e);
        }
        return id;
    }
}
