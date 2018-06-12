/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.engine.autoexport;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.ObservationManager;

import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistryListener;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.impl.RepositoryDecorator;
import org.onehippo.repository.InternalHippoRepository;

import static com.google.common.collect.Sets.newHashSet;

class NodeTypeChangesMonitor implements NodeTypeRegistryListener {

    private final NodeTypeRegistry ntRegistry;
    private final AutoExportConfig autoExportConfig;
    private final Session monitorSession;
    // TODO: derive 'protected' namsepacePrefixed from none-sourced ConfigurationModel modules
    private static final Set<String> hippoNamespacePrefixed = newHashSet(
            "hippo",
            "hipposys",
            "hipposysedit",
            "hippofacnav");

    public NodeTypeChangesMonitor(final AutoExportConfig autoExportConfig) throws RepositoryException {
        this.autoExportConfig = autoExportConfig;
        monitorSession = autoExportConfig.createImpersonatedSession();
        InternalHippoRepository internalHippoRepository =
                (InternalHippoRepository) RepositoryDecorator.unwrap(monitorSession.getRepository());
        ntRegistry = internalHippoRepository.getNodeTypeRegistry();
        ntRegistry.addListener(this);
    }

    private String getNamespacePrefix(final Name name) {
        try {
            final String namespacePrefix = monitorSession.getNamespacePrefix(name.getNamespaceURI());
            if (!hippoNamespacePrefixed.contains(namespacePrefix)) {
                return namespacePrefix;
            }
        } catch (RepositoryException e) {
            // ignore
        }
        return null;
    }

    public void logNodeTypeRegistryLastModifiedEvent(final String userData) {
        try {
            final Node autoExportConfigNode = monitorSession.getNode(autoExportConfig.getConfigPath());
            monitorSession.refresh(false);
            ObservationManager observationManager = monitorSession.getWorkspace().getObservationManager();
            observationManager.setUserData(userData);
            try {
                if (autoExportConfigNode.hasProperty(AutoExportConstants.CONFIG_NTR_LAST_MODIFIED_PROPERTY_NAME)) {
                    autoExportConfigNode.getProperty(AutoExportConstants.CONFIG_NTR_LAST_MODIFIED_PROPERTY_NAME).remove();
                }
                autoExportConfigNode.setProperty(AutoExportConstants.CONFIG_NTR_LAST_MODIFIED_PROPERTY_NAME, System.currentTimeMillis());
                monitorSession.save();
                AutoExportServiceImpl.log.debug("journal logging changed nodetype prefix(es): [{}]",userData);
            } finally {
                observationManager.setUserData(null);
            }
        } catch (RepositoryException e) {
            AutoExportServiceImpl.log.error("Failed to generate journal event for NodeTypeRegistry change.", e);
        }
    }

    @Override
    public void nodeTypeRegistered(final Name name) {
        final String namespacePrefix = getNamespacePrefix(name);
        if (namespacePrefix != null) {
            logNodeTypeRegistryLastModifiedEvent(namespacePrefix);
        }
    }

    @Override
    public void nodeTypeReRegistered(final Name name) {
        nodeTypeRegistered(name);
    }

    @Override
    public void nodeTypesUnregistered(final Collection<Name> collection) {
        HashSet<String> namespacePrefixes = new HashSet<>();
        for (final Name name : collection) {
            final String namespacePrefix = getNamespacePrefix(name);
            if (namespacePrefix != null) {
                namespacePrefixes.add(namespacePrefix);
            }
        }
        if (!namespacePrefixes.isEmpty()) {
            final String userData = namespacePrefixes.stream().collect(Collectors.joining("|"));
            logNodeTypeRegistryLastModifiedEvent(userData);
        }
    }

    public void shutdown() {
        ntRegistry.removeListener(this);
        monitorSession.logout();
    }
}
