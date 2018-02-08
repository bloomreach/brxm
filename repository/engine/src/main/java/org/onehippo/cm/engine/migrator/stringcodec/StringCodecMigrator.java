/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.migrator.stringcodec;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cm.engine.migrator.ConfigurationMigrator;
import org.onehippo.cm.engine.migrator.MigrationException;
import org.onehippo.cm.engine.migrator.PostMigrator;
import org.onehippo.cm.model.ConfigurationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PostMigrator
public class StringCodecMigrator implements ConfigurationMigrator {

    private static final Logger log = LoggerFactory.getLogger(StringCodecMigrator.class);

    private static final String OLD_CODECS_CONFIGURATION_LOCATION = "/hippo:configuration/hippo:frontend/cms/cms-services/settingsService/codecs";
    private static final String NEW_CODECS_CONFIGURATION_LOCATION = "/hippo:configuration/hippo:modules/stringcodec/hippo:moduleconfig";
    private static final String NODE_CONFIGURATION_PARAMETER_PATTERN = "encoding.node*";
    private static final String DISPLAY_CONFIGURATION_PARAMETER = "encoding.display";
    private static final String HIPPO_CONFIGURATION = "/hippo:configuration";
    private static final String HIPPO_MODULECONFIG = "hippo:moduleconfig";

    @Override
    public boolean migrate(final Session session, final ConfigurationModel configurationModel, final boolean autoExportEnabled) throws RepositoryException {
        try {
            return doMigrate(session, configurationModel, autoExportEnabled);
        } catch (final RepositoryException e) {
            throw new MigrationException("StringCodecMigrator failed.", e);
        }
    }

    private boolean doMigrate(final Session session, final ConfigurationModel configurationModel, final boolean autoExportEnabled) throws RepositoryException {
        if (!shouldRun(session, autoExportEnabled)) {
            log.info("Node name is not configured :StringCodecMigrator does not need to do anything.");
            return false;
        }

        final boolean success = migrateStringCodecConfiguration(session);
        if (!success) {
            throw new MigrationException("Could not migrate configuration for migrator StringCodecMigrator");
        }

        session.save();

        return true;
    }

    private boolean shouldRun(final Session session, final boolean autoExportEnabled) throws RepositoryException {
        try {
            if (hasConfigurationProperties(session) && autoExportEnabled) {
                log.info("{} will run because node name is configured.", this);
                return true;
            }
        } catch (final NamespaceException ignore) {
            log.info("String codec has not been configured, nothing to migrate.");
            return false;
        }
        return false;
    }

    private static boolean hasConfigurationProperties(final Session session) throws RepositoryException {
        final boolean oldConfigLocationExists = session.nodeExists(OLD_CODECS_CONFIGURATION_LOCATION);
        if (oldConfigLocationExists) {
            final Node node = session.getNode(OLD_CODECS_CONFIGURATION_LOCATION);
            return node.hasProperty(DISPLAY_CONFIGURATION_PARAMETER) ||
                    node.getProperties(NODE_CONFIGURATION_PARAMETER_PATTERN).getSize() > 0;
        }
        return false;
    }

    private boolean migrateStringCodecConfiguration(final Session session) throws RepositoryException {
        final Node oldConfigLocation = session.getNode(OLD_CODECS_CONFIGURATION_LOCATION);
        final Node newConfigLocation = session.getNode(NEW_CODECS_CONFIGURATION_LOCATION);
        final PropertyIterator properties = oldConfigLocation.getProperties(NODE_CONFIGURATION_PARAMETER_PATTERN);

        while (properties.hasNext()) {
            try {
                setNewProperty(newConfigLocation, properties.nextProperty());
            } catch (final RepositoryException e) {
                log.error("Migrating StringCodec configuration failed.", e);
                return false;
            }
        }

        if (oldConfigLocation.hasProperty(DISPLAY_CONFIGURATION_PARAMETER)) {
            setNewProperty(newConfigLocation, oldConfigLocation.getProperty(DISPLAY_CONFIGURATION_PARAMETER));
        }

        return cleanupConfiguration(oldConfigLocation);
    }

    private boolean cleanupConfiguration(final Node oldConfigLocation) {
        try {
            oldConfigLocation.remove();
        } catch (final RepositoryException e) {
            log.error("Failed to clean up configuration in StringCodecMigrator", e);
            return false;
        }

        return true;
    }

    private static void setNewProperty(final Node configLocation, final Property property) throws RepositoryException {
        configLocation.setProperty(property.getName(), property.getString());
    }
}
