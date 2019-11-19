/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.migrator.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.collections4.CollectionUtils;
import org.onehippo.cm.engine.migrator.ConfigurationMigrator;
import org.onehippo.cm.engine.migrator.MigrationException;
import org.onehippo.cm.engine.migrator.PostMigrator;
import org.onehippo.cm.model.ConfigurationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PostMigrator
public class ContainerComponentPropertyMigrator implements ConfigurationMigrator {

    private static final Logger log = LoggerFactory.getLogger(ContainerComponentPropertyMigrator.class);
    private static final String PARENT_NODE_TYPE = "hst:containercomponent";
    private static final String STANDARD_CONTAINER_COMPONENT_PROPERTY_NAME = "hst:componentclassname";
    private static final String OLD_STANDARD_CONTAINER_COMPONENT_PATH = "org.hippoecm.hst.pagecomposer.builtin.components.StandardContainerComponent";
    private static final String NEW_STANDARD_CONTAINER_COMPONENT_PATH = "org.hippoecm.hst.builtin.components.StandardContainerComponent";

    @Override
    public boolean migrate(final Session session, final ConfigurationModel configurationModel, final boolean autoExportEnabled) throws RepositoryException {
        try {
            return doMigrate(session, configurationModel, autoExportEnabled);
        } catch (final RepositoryException e) {
            throw new MigrationException("ContainerComponentPropertyMigrator failed.", e);
        }
    }

    private boolean doMigrate(final Session session, final ConfigurationModel configurationModel, final boolean autoExportEnabled) throws RepositoryException {

        final List<Property> containerComponentPropertyNodes = getContainerComponentPropertyNodes(
                session.getWorkspace().getQueryManager());
        if (CollectionUtils.isEmpty(containerComponentPropertyNodes)) {
            log.info("No container component property found with the default values.");
            return false;
        }

        log.info("{} unnecessary default container component values have been found in repository.", containerComponentPropertyNodes.size());
        final boolean success = cleanComponentsConfiguration(containerComponentPropertyNodes);
        if (!success) {
            throw new MigrationException("Could not clean unnecessary configurations for migrator ContainerComponentPropertyMigrator");
        }

        session.save();
        return true;
    }

    private boolean cleanComponentsConfiguration(final List<Property> componentComponentPropertyNodes) {
        for (Property property : componentComponentPropertyNodes) {
            try {
                property.remove();
            } catch (final RepositoryException e) {
                log.error("Cleaning unnecessary properties for ContainerComponentPropertyMigrator configuration failed.", e);
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private List<Property> getContainerComponentPropertyNodes(final QueryManager queryManager) throws RepositoryException {

        final Query query = queryManager.createQuery("//element(*, " + PARENT_NODE_TYPE + ")"
                + "[@" + STANDARD_CONTAINER_COMPONENT_PROPERTY_NAME + "='" + OLD_STANDARD_CONTAINER_COMPONENT_PATH + "' or "
                + "@" + STANDARD_CONTAINER_COMPONENT_PROPERTY_NAME + "='" + NEW_STANDARD_CONTAINER_COMPONENT_PATH + "']", Query.XPATH);
        final QueryResult queryResult = query.execute();

        final NodeIterator nodes = queryResult.getNodes();
        if (nodes.getSize() == 0) {
            return Collections.emptyList();
        }

        final List<Property> containerComponentProperties = new ArrayList<>((int) nodes.getSize());
        while (nodes.hasNext()) {
            final Node node = (Node) nodes.next();
            containerComponentProperties.add(node.getProperty(STANDARD_CONTAINER_COMPONENT_PROPERTY_NAME));
        }

        return containerComponentProperties;
    }

}
