/*
 *  Copyright 2010 Hippo.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.ConfigurationParser;
import org.hippoecm.repository.replication.ReplicationJournal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * This class holds the configuration of all {@link ReplicatorNodeConfig}s.
 */
public class ReplicationConfig {

    /** the default logger */
    private static Logger log = LoggerFactory.getLogger(ReplicationConfig.class);

    /** Name of the workspace configuration file. */
    private static final String REPLICATION_XML = "replication.xml";

    /** System property for overriding the replication config file */
    public static final String SYSTEM_CONFIG_PROPERTY = "replication.config";

    /** System property for overriding the servlet config file */
    public static final String SYSTEM_SERVLETCONFIG_PROPERTY = "replication.config";

    /**
     * map of replicator names and replicator configurations
     */
    private final Map<String, ReplicatorNodeConfig> replicators;

    /**
     * Journal configuration.
     */
    private final ReplicationJournal jc;

    /**
     * Creates a repository configuration object.
     *
     * @param home repository home directory
     * @param jc the journal configuration
     * @param fsf file system factory
     */
    public ReplicationConfig(ReplicationJournal jc, Map<String, ReplicatorNodeConfig> replicators, String home) {
        this.jc = jc;
        this.replicators = replicators;
    }

    /**
     * Convenience method that gets the configuration as {@link InputStream} and 
     * wraps the configuration input stream into an
     * {@link InputSource} and invokes the
     * {@link #create(InputSource, String)} method.
     *
     * @param home replication home directory
     * @return repository configuration or null if the configuration cannot be found
     * @throws ConfigurationException on configuration errors
     * @see #create(InputSource, String)
     */
    public static ReplicationConfig create(String home) throws ConfigurationException {
        InputStream input = getReplicationConfigAsStream();
        if (input == null) {
            return null;
        }
        return create(new InputSource(input), home);
    }

    /**
     * Parses the given repository configuration document and returns the
     * parsed and initialized repository configuration. The given repository
     * home directory path will be used as the ${rep.home} parser variable.
     * <p>
     * Note that in addition to parsing the repository configuration, this
     * method also initializes the configuration (creates the configured
     * directories, etc.). The {@link ConfigurationParser} class should be
     * used directly to just parse the configuration.
     *
     * @param xml repository configuration document
     * @param home repository home directory
     * @return repository configuration
     * @throws ConfigurationException on configuration errors
     */
    public static ReplicationConfig create(InputSource xml, String home) throws ConfigurationException {
        Properties variables = new Properties(System.getProperties());
        variables.setProperty(ReplicationConfigurationParser.REPOSITORY_HOME_VARIABLE, home);
        ReplicationConfigurationParser parser = new ReplicationConfigurationParser(variables);

        ReplicationConfig config = parser.parseReplicationConfig(xml);
        //        config.init();

        return config;
    }

    /**
     * If the "file://" protocol is used, the path MUST be absolute.
     * In all other cases the config file is used as a class resource.
     * @return InputStream to the repository config or null if the config is not found
     */
    private static InputStream getReplicationConfigAsStream() {
        // get config from system property
        String configName = System.getProperty(SYSTEM_CONFIG_PROPERTY);

        // if not set, try to use the servlet config
        if (configName == null || "".equals(configName)) {
            configName = System.getProperty(SYSTEM_SERVLETCONFIG_PROPERTY);
        }

        // if still not set, use default
        if (configName == null || "".equals(configName)) {
            log.info("Using default replication config context:/{}", REPLICATION_XML);
            InputStream is = ReplicationConfig.class.getResourceAsStream(REPLICATION_XML);
            if (is == null) {
                log.info("Replication config not found: context:/{}. Disabling replicaiton.", REPLICATION_XML);
                return null;
            }
            return ReplicationConfig.class.getResourceAsStream(REPLICATION_XML);
        }

        // resource
        if (!configName.startsWith("file:")) {
            log.info("Using resource replication config context:/{}", configName);
            InputStream is = ReplicationConfig.class.getResourceAsStream(configName);
            if (is == null) {
                log.info("Replication config not found: context:/{}. Disabling replicaiton.", configName);
                return null;
            }
            return new BufferedInputStream(is);
        }

        // parse file name
        if (configName.startsWith("file://")) {
            configName = configName.substring(6);
        } else if (configName.startsWith("file:/")) {
            configName = configName.substring(5);
        } else if (configName.startsWith("file:")) {
            configName = "/" + configName.substring(5);
        }
        log.info("Using file replication config: file:/{}", configName);

        // get the buffered inputstream
        File configFile = new File(configName);
        try {
            FileInputStream fis = new FileInputStream(configFile);
            return new BufferedInputStream(fis);
        } catch (FileNotFoundException e) {
            log.info("Replication config not found: file:/{}. Disabling replicaiton.", configName);
            log.debug("Cause: ", e);
            return null;
        }
    }

    /**
     * Returns all replicator configurations.
     *
     * @return replicator configurations
     */
    public Collection<ReplicatorNodeConfig> getReplicatorConfigs() {
        return replicators.values();
    }

    /**
     * Returns the configuration of the specified replicator.
     *
     * @param id replicator id
     * @return replicator configuration, or <code>null</code> if the named
     *         replicator does not exist
     */
    public ReplicatorNodeConfig getReplicatorConfig(String id) {
        return replicators.get(id);
    }

    /**
     * Returns the journal configuration.
     *
     * @return journal configuration
     */
    public ReplicationJournal getJournalConfig() {
        return jc;
    }
}
