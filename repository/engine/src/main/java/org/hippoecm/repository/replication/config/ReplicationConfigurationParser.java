/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.replication.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.core.config.BeanConfig;
import org.apache.jackrabbit.core.config.ConfigurationErrorHandler;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.ConfigurationParser;
import org.hippoecm.repository.replication.ReplicationJournal;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Configuration parser. This class is used to parse the replication configuration file.
 * <p>
 * The following code sample outlines the usage of this class:
 * <pre>
 *     Properties variables = ...; // parser variables
 *     ReplicationConfigurationParser parser =
 *         new ReplicationConfigurationParser(variables);
 *     ReplicationConfig rc = parser.parseReplicationConfig(...);
 * </pre>
 * <p>
 * Note that the configuration objects returned by this parser are not
 * initialized. The caller needs to initialize the configuration objects
 * before using them.
 * <p>
 * Note that this class is based on {@link org.apache.jackrabbit.core.config.RepositoryConfigurationParser}
 */
public class ReplicationConfigurationParser extends ConfigurationParser {

    /** Name of the repository home directory parser variable. */
    public static final String REPOSITORY_HOME_VARIABLE = "rep.home";

    /** Name of the journal configuration element. */
    public static final String JOURNAL_ELEMENT = "Journal";

    /** Name of the replicator nodes configuration element. */
    public static final String REPLICATOR_NODES_ELEMENT = "ReplicatorNodes";

    /** Name of the replicator node configuration element. */
    public static final String REPLICATOR_NODE_ELEMENT = "ReplicatorNode";

    /** Name of the replicator configuration element. */
    public static final String REPLICATOR_ELEMENT = "Replicator";

    /** Name of the replicator filter configuration element. */
    public static final String FILTER_ELEMENT = "Filter";

    /** Name of the id configuration attribute. */
    public static final String ID_ATTRIBUTE = "id";

    /** Name of the syncDelay configuration attribute. */
    public static final String SYNC_DELAY_ATTRIBUTE = "syncDelay";

    /** Default synchronization delay, in milliseconds. */
    public static final String DEFAULT_SYNC_DELAY = "5000";

    /** Name of the stopDelay configuration attribute. */
    public static final String STOP_DELAY_ATTRIBUTE = "stopDelay";

    /** Default stop delay, in milliseconds. */
    public static final String DEFAULT_STOP_DELAY = "5000";

    /** Name of the stopDelay configuration attribute. */
    public static final String RETRY_DELAY_ATTRIBUTE = "retryDelay";

    /** Default delay between retries, in milliseconds. */
    public static final String DEFAULT_RETRY_DELAY = "10000";

    /** Name of the maxRetries configuration attribute. */
    public static final String MAX_RETRIES_ATTRIBUTE = "maxRetries";

    /** Default maximum number of retries. */
    public static final String DEFAULT_MAX_RETRIES = "6";

    /**
     * Creates a new configuration parser with the given parser variables.
     *
     * @param variables parser variables
     */
    public ReplicationConfigurationParser(Properties variables) {
        super(variables);
    }

    /**
     * Parses replication configuration. Replication configuration uses the
     * following format:
     * <pre>
     *   &lt;Replication&gt;
     *     &lt;Journal class="..."&gt;
     *       &lt;param name="..." value="..."&gt;
     *     &lt;/Journal&gt;
     *     &lt;ReplicatorNodes&gt;
     *       &lt;ReplicatorNode*&gt;
     *         &lt;param name="..." value="..."&gt;
     *         &lt;Replicator class=".."&gt;
     *           &lt;param name="..." value="..."&gt;
     *         &lt;/Replicator&gt;
     *         &lt;Filter* class=".."&gt;
     *           &lt;param name="..." value="..."&gt;
     *         &lt;/Filter&gt;
     *       &lt;/ReplicatorNode&gt;
     *     &lt;/Replicators&gt;
     *   &lt;/Replication&gt;
     * </pre>
     * <p>
     * In addition to the configured information, the returned repository
     * configuration object also contains the repository home directory path
     * that is given as the ${rep.home} parser variable. Note that the
     * variable <em>must</em> be available for the configuration document to
     * be correctly parsed.
     *
     * @param xml repository configuration document
     * @return replication configuration
     * @throws ConfigurationException if the configuration is broken
     * @see #parseBeanConfig(Element, String)
     * @see #parseVersioningConfig(Element)
     */
    public ReplicationConfig parseReplicationConfig(InputSource xml) throws ConfigurationException {
        Element root = parseXML(xml, true);

        // Repository home directory
        String home = getVariables().getProperty(REPOSITORY_HOME_VARIABLE);

        // Journal configuration
        ReplicationJournal journalConfig = parseJournalConfig(root);

        // Replicator configurations
        Element replicators = getElement(root, REPLICATOR_NODES_ELEMENT);
        Map<String, ReplicatorNodeConfig> rcMap = parseReplicatorNodesConfig(replicators, home);

        return new ReplicationConfig(journalConfig, rcMap, home);
    }

    /**
     * Parses ReplicatorNodes configuration. The ReplicatorNode configuration uses the following format:
     * <pre>
     *   &lt;ReplicatorNodes&gt;
     *     &lt;ReplicatorNode&gt;
     *       &lt;param name="..." value="..."&gt;
     *       &lt;Replicator class=".."&gt;
     *         &lt;param name="..." value="..."&gt;
     *       &lt;/Replicator&gt;
     *       &lt;Filter* class=".."&gt;
     *         &lt;param name="..." value="..."&gt;
     *       &lt;/Filter&gt;
     *     &lt;/ReplicatorNode&gt;
     *   &lt;/Replicators&gt;
     * </pre>
     *
     * @param element the <code>ReplicatorNode</code> element
     * @param replicatorHomeDir the replicator home dir.
     * @return ReplicatorNodeConfig
     * @throws ConfigurationException if the configuration is broken
     */
    protected Map<String, ReplicatorNodeConfig> parseReplicatorNodesConfig(Element parent, String replicatorHomeDir)
            throws ConfigurationException {
        Map<String, ReplicatorNodeConfig> rcMap = new HashMap<String, ReplicatorNodeConfig>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && REPLICATOR_NODE_ELEMENT.equals(child.getNodeName())) {
                Element element = (Element) child;
                ReplicatorNodeConfig rc = parseReplicatorNodeConfig(element, replicatorHomeDir);
                if (rc != null) {
                    rcMap.put(rc.getId(), rc);
                }
            }
        }
        return rcMap;
    }

    /**
     * Parses ReplicatorNode configuration. The ReplicatorNode configuration uses the following format:
     * <pre>
     *   &lt;ReplicatorNode&gt;
     *     &lt;Replicator class=".."&gt;
     *       &lt;param name="..." value="..."&gt;
     *     &lt;/Replicator&gt;
     *     &lt;Filter* class=".."&gt;
     *       &lt;param name="..." value="..."&gt;
     *     &lt;/Filter&gt;
     *   &lt;/ReplicatorNode&gt;
     * </pre>
     *
     * @param element the <code>ReplicatorNode</code> element
     * @param replicatorHomeDir the replicator home dir.
     * @return ReplicatorNodeConfig
     * @throws ConfigurationException if the configuration is broken
     */
    protected ReplicatorNodeConfig parseReplicatorNodeConfig(Element element, String replicatorHomeDir)
            throws ConfigurationException {

        Properties properties = parseParameters(element);
        String id = replaceVariables(properties.getProperty(ID_ATTRIBUTE, null));

        String value = replaceVariables(properties.getProperty(SYNC_DELAY_ATTRIBUTE, DEFAULT_SYNC_DELAY));
        long syncDelay = Long.parseLong(replaceVariables(value));

        value = replaceVariables(properties.getProperty(STOP_DELAY_ATTRIBUTE, DEFAULT_STOP_DELAY));
        long stopDelay = Long.parseLong(replaceVariables(value));

        value = replaceVariables(properties.getProperty(RETRY_DELAY_ATTRIBUTE, DEFAULT_RETRY_DELAY));
        long retryDelay = Long.parseLong(replaceVariables(value));

        value = replaceVariables(properties.getProperty(MAX_RETRIES_ATTRIBUTE, DEFAULT_MAX_RETRIES));
        int maxRetries = Integer.parseInt(replaceVariables(value));

        // Replicator configuration
        ReplicatorConfig replicatorConfig = new ReplicatorConfig(parseBeanConfig(element, REPLICATOR_ELEMENT));

        List<FilterConfig> filterConfigs = parseFilterConfig(element);
        ReplicatorNodeConfig config = new ReplicatorNodeConfig(id, replicatorConfig, filterConfigs);
        config.setSyncDelay(syncDelay);
        config.setStopDelay(stopDelay);
        config.setRetryDelay(retryDelay);
        config.setMaxRetries(maxRetries);
        return config;
    }

    /**
     * Parses Filter configuration. The Filter configuration uses the following format:
     * <pre>
     *   &lt;Filter* class=".."&gt;
     *     &lt;param name="..." value="..."&gt;
     *   &lt;/Filter&gt;
     * </pre>
     *
     * @param element the parent element
     * @return List of FilterConfigs
     * @throws ConfigurationException if the configuration is broken
     */
    protected List<FilterConfig> parseFilterConfig(Element parent) throws ConfigurationException {
        List<FilterConfig> filterConfigs = new ArrayList<FilterConfig>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && FILTER_ELEMENT.equals(child.getNodeName())) {
                Element element = (Element) child;
                filterConfigs.add(new FilterConfig(parseBeanConfig(element)));
            }
        }
        return filterConfigs;
    }
    
    /**
     * Parses journal configuration. Journal configuration uses the following format:
     * <pre>
     *   &lt;Journal class="..."&gt;
     *     &lt;param name="..." value="..."&gt;
     *     ...
     *   &lt;/Journal&gt;
     * </pre>
     * <p/>
     * <code>Journal</code> is a {@link #parseBeanConfig(Element,String) bean configuration}
     * element.
     *
     * @param replication parent replication element
     * @return journal configuration, or <code>null</code>
     * @throws ConfigurationException if the configuration is broken
     */
    protected ReplicationJournal parseJournalConfig(Element replication) throws ConfigurationException {

        BeanConfig beanConfig = new BeanConfig(ReplicationJournal.class.getName(), parseParameters(getElement(replication, JOURNAL_ELEMENT)));
        return beanConfig.<ReplicationJournal>newInstance(ReplicationJournal.class);
    }

    /**
     * Creates a new instance of a configuration parser but with overlayed
     * variables.
     *
     * @param variables the variables overlay
     * @return a new configuration parser instance
     */
    protected ReplicationConfigurationParser createSubParser(Properties variables) {
        // overlay the properties
        Properties props = new Properties(getVariables());
        props.putAll(variables);
        return new ReplicationConfigurationParser(props);
    }
    
    
    /**
     * The method is overridden because the <code>HippoConfigurationEntityResolver</code>
     * is needed.
    */
    @Override
    protected Element parseXML(InputSource xml, boolean validate) throws ConfigurationException {
        try {
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            factory.setValidating(validate);
            DocumentBuilder builder = factory.newDocumentBuilder();
            if (validate) {
                builder.setErrorHandler(new ConfigurationErrorHandler());
            }
            builder.setEntityResolver(HippoConfigurationEntityResolver.INSTANCE);
            Document document = builder.parse(xml);
            return document.getDocumentElement();
        } catch (ParserConfigurationException e) {
            throw new ConfigurationException(
                    "Unable to create configuration XML parser", e);
        } catch (SAXParseException e) {
            throw new ConfigurationException(
                    "Configuration file syntax error. (Line: " + e.getLineNumber() + " Column: " + e.getColumnNumber() + ")", e);
        } catch (SAXException e) {
            throw new ConfigurationException(
                    "Configuration file syntax error. ", e);
        } catch (IOException e) {
            throw new ConfigurationException(
                    "Configuration file could not be read.", e);
        }
    }
}
