/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.migrator;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.cm.engine.migrator.ConfigurationMigrator;
import org.onehippo.cm.engine.migrator.MigrationException;
import org.onehippo.cm.model.ConfigurationModel;
import org.onehippo.cm.model.definition.NamespaceDefinition;
import org.onehippo.cm.model.tree.Value;
import org.onehippo.repository.util.NodeTypeUtils;
import org.slf4j.Logger;

import static org.apache.commons.lang.StringUtils.substringAfterLast;

public abstract class HstChannelMigratorToV12 implements ConfigurationMigrator {

    private final Set<String> migratedChannelPaths = new HashSet<>();

    private final String DEFAULT_HST_ROOT = "/hst:hst";

    private final String hstRoot;

    private final boolean save;

    public HstChannelMigratorToV12() {
        hstRoot = DEFAULT_HST_ROOT;
        save = true;
    }

    public HstChannelMigratorToV12(final String hstRoot, final boolean save) {
        this.hstRoot = hstRoot;
        this.save = save;
    }

    public abstract Logger getLogger();

    @Override
    public boolean migrate(final Session session, final ConfigurationModel configurationModel, final boolean autoExportEnabled) throws RepositoryException {
        try {
            return doMigrate(session, configurationModel, autoExportEnabled);
        } catch (RepositoryException e) {
            throw  new MigrationException(String.format("Migrator '%s' failed.",
                    this.getClass().getName()), e);
        }
    }

    protected boolean doMigrate(final Session session, final ConfigurationModel configurationModel, final boolean autoExportEnabled) throws RepositoryException {
        if (!shouldRun(session, autoExportEnabled)) {
            getLogger().info(hstRoot + "/hst:channels missing or does not have children :HstChannelMigrator doesn 't need to do anything");
            return false;
        }

        if (configurationModel != null) {
            final boolean success = preemptiveInitializeHstNodeType(session, configurationModel);
            if (!success) {
                throw new MigrationException(String.format("Could not preemptively reload HST node types for migrator '%s'",
                        this.getClass().getName()));
            }
        }

        if (!basicHstConfigurationNodesPresent(session)) {
            getLogger().info("Root hst configuration nodes missing implying the model can't be valid. Nothing to migrate");
            return false;
        }

        getLogger().info("Start HST Channel migration");
        removePreviewChannelsAndConfigurations(session);

        denormalizeSiteConfigurations(session, hstRoot);

        for (Node hostGroup : new NodeIterable(session.getNode(hstRoot + "/hst:hosts").getNodes())) {
            for (Node hostOrMount : new NodeIterable(hostGroup.getNodes())) {
                migrateHostOrMount(hostOrMount);
            }
        }

        // all channels should have been moved now. If there are channels left, we log a warning that the migrator
        // could not migrate all channels because there is ambiguity. This means that some manual work has to be done
        // to clean up the left over channels (most likely just delete them)
        Node channelsNode = session.getNode(hstRoot + "/hst:channels");
        if (channelsNode.getNodes().getSize() > 0) {
            getLogger().info("{} could not successfully move all channel nodes because there seem to be some orphaned channel nodes." +
                    "The following nodes will be deleted.", this);
            for (Node channel : new NodeIterable(channelsNode.getNodes())) {
                getLogger().info("Channel '{}' was orphaned and will be removed.", channel.getPath());
            }
        }
        channelsNode.remove();

        migrateBlueprints(session, hstRoot);

        if (save) {
            session.save();
        }
        return true;
    }

    protected boolean preemptiveInitializeHstNodeType(final Session session, final ConfigurationModel configurationModel) throws RepositoryException {
        for (NamespaceDefinition namespaceDefinition : configurationModel.getNamespaceDefinitions()) {
            if ("hst".equals(namespaceDefinition.getPrefix())) {
                // reload the hst ns
                try {
                    final Value cndPath = namespaceDefinition.getCndPath();
                    if (cndPath == null) {
                        getLogger().error("Expected non null cnd for hst. Cannot run migrator '{}'", this);
                        return false;
                    }
                    final String cndPathOrigin = String.format("'%s' (%s)", cndPath, namespaceDefinition.getOrigin());
                    try (final InputStream nodeTypeStream = namespaceDefinition.getCndPath().getResourceInputStream()) {
                        NodeTypeUtils.initializeNodeTypes(session, nodeTypeStream, cndPathOrigin);
                    }
                } catch (IOException | RepositoryException e) {
                    getLogger().error("Error while trying to re-register the hst cnd. Cannot run migrator '{}'", this);
                    return false;
                }
            }
        }
        return true;
    }

    private void denormalizeSiteConfigurations(final Session session, final String hstRoot) throws RepositoryException {
        final Map<String, List<String>> hstConfigurationPathToListOfSiteNodes = new HashMap();
        for (Node siteNode : new NodeIterable(session.getNode(hstRoot + "/hst:sites").getNodes())) {
            final String configurationPath;
            if (siteNode.hasProperty("hst:configurationpath")) {
                configurationPath = siteNode.getProperty("hst:configurationpath").getString();
            } else {
                configurationPath = hstRoot + "/hst:configuration/" + siteNode.getName();
            }
            if (!hstConfigurationPathToListOfSiteNodes.containsKey(configurationPath)) {
                hstConfigurationPathToListOfSiteNodes.put(configurationPath, new ArrayList<>());
            }
            List<String> siteNodes = hstConfigurationPathToListOfSiteNodes.get(configurationPath);
            siteNodes.add(siteNode.getPath());
        }
        // now check for cases where a single hst configuration is used by multiple site nodes. This is in general
        // not supported any more hence we normalize this
        for (Map.Entry<String, List<String>> entry : hstConfigurationPathToListOfSiteNodes.entrySet()) {
            final List<String> siteNodes = entry.getValue();
            if (siteNodes.size() > 1) {
                final String configurationPath = entry.getKey();
                if (!session.nodeExists(configurationPath)) {
                    getLogger().warn("Expected '{}' to exist but not found. Cannot denormalize hst:sites nodes. There exist HST " +
                            "configuration errors.");
                    continue;
                }
                // found an (bad) CMS 11 old construct which is not allowed in CMS 12 any more.
                // Replace this with hst configuration inheritance. First sort by
                // site node to get consistent order (where the primary configuration is the configuration for the
                // hst:site node that comes alphabetically first
                siteNodes.sort(String::compareTo);

                // find the primary site node: This is the site node that does not have an hst:configurationpath property
                // or can do without the hst:configuration property. If no primary site node, take the first

                final int indexOfPrimarySiteNode = findPrimarySiteNodeIndex(siteNodes, configurationPath);

                for (int i = 0; i < siteNodes.size(); i++) {
                    final String siteNodePath = siteNodes.get(i);
                    final Node siteNode = session.getNode(siteNodePath);
                    if (i == indexOfPrimarySiteNode) {
                        // check whether the hst:configurationpath is superfluous
                        if (configurationPath.equals(hstRoot + "/hst:configurations/" + siteNode.getName())) {
                            // configurationpath prop is superfluous if present
                            if (siteNode.hasProperty("hst:configurationpath")) {
                                siteNode.getProperty("hst:configurationpath").remove();
                            }
                        }
                        continue;
                    }
                    // denormalize this site node
                    final String newConfigName = findAvailableHstConfigurtionNodeName(session, hstRoot, siteNode);
                    final Node newConfigNode = session.getNode(hstRoot + "/hst:configurations").addNode(newConfigName, "hst:configuration");
                    String configurationNodeName = substringAfterLast(configurationPath, "/");
                    newConfigNode.setProperty("hst:inheritsfrom", new String[]{"../" + configurationNodeName, "../" + configurationNodeName + "/hst:workspace"});
                    if (newConfigName.equals(siteNode.getName())) {
                        if (siteNode.hasProperty("hst:configurationpath")) {
                            siteNode.getProperty("hst:configurationpath").remove();
                        }
                    } else {
                        siteNode.setProperty("hst:configurationpath", hstRoot + "/hst:configurations/" + newConfigName);
                    }
                }
            }
        }
    }

    private int findPrimarySiteNodeIndex(final List<String> siteNodes, final String configurationPath) {
        for (String siteNode : siteNodes) {
            if (substringAfterLast(siteNode, "/").equals(substringAfterLast(configurationPath, "/"))) {
                return siteNodes.indexOf(siteNode);
            }
        }
        return 0;
    }

    private String findAvailableHstConfigurtionNodeName(final Session session, final String hstRoot, final Node siteNode) throws RepositoryException {
        Node configurationsNode = session.getNode(hstRoot + "/hst:configurations");
        if (!configurationsNode.hasNode(siteNode.getName())) {
            return siteNode.getName();
        }
        int i = 1;
        while (configurationsNode.hasNode(siteNode.getName() + i)) {
            i++;
        }
        return siteNode.getName() + i;
    }

    private void migrateHostOrMount(final Node hostOrMount) throws RepositoryException {
        if (hostOrMount.isNodeType("hst:mount")) {
            migrateMount(hostOrMount);
        } else {
            for (Node child : new NodeIterable(hostOrMount.getNodes())) {
                migrateHostOrMount(child);
            }
        }
    }

    private void migrateMount(final Node mount) throws RepositoryException {

        final Session session = mount.getSession();
        if (mount.hasProperty("hst:channelpath") && mount.hasProperty("hst:mountpoint")) {
            final Property channelPathProperty = mount.getProperty("hst:channelpath");
            final String channelPath = channelPathProperty.getString();
            channelPathProperty.remove();

            if (mount.hasProperty("hst:ismapped") && !mount.getProperty("hst:ismapped").getBoolean()) {
                getLogger().warn("Mount {} has a channelpath but also hst:ismapped = false. This was a configuration error. Just " +
                        "the channelpath will be removed.", mount.getPath());
            } else if (migratedChannelPaths.contains(channelPath)) {
                // channel path already migrated
            } else {
                try {
                    if (session.nodeExists(channelPath)) {

                        final String channelTargetConfiguration = findChannelTargetConfiguration(mount);

                        if (session.nodeExists(channelTargetConfiguration)) {
                            final Node targetConfiguration = session.getNode(channelTargetConfiguration);
                            final Node workspace;
                            if (targetConfiguration.hasNode("hst:workspace")) {
                                workspace = targetConfiguration.getNode("hst:workspace");
                            } else {
                                workspace =  targetConfiguration.addNode("hst:workspace", "hst:workspace");
                            }
                            final String targetPath = workspace.getPath() + "/hst:channel";
                            session.move(channelPath, targetPath);
                            getLogger().info("Successfully moved '{}' to '{}' ", channelPath, targetPath);
                        } else {
                            getLogger().warn("No configuration found at '{}' which was expected to be there for mount '{}' ",
                                    channelTargetConfiguration, mount.getPath());
                        }
                        migratedChannelPaths.add(channelPath);
                    } else {
                        getLogger().info("Mount {} has an hst:channelpath that does not exist.Removing the hst:channelpath property now. ",
                                mount.getPath(), channelPath);
                    }
                } catch (RepositoryException e) {
                    getLogger().warn("Mount {} contains an invalid hst:channelpath {} which is not a valid jcr path.Property will " +
                            "be removed since doens't work in the first place.", mount.getPath(), channelPath);

                }
            }

        } else if (isMapped(mount)) {
            // when the hst:ismapped is false, we only need to set "hst:nochannelinfo" if there is an (possibly inherited!!)
            // hst:mounpoint that points to a hst:configuration that has an hst:channel
            if (hasMountPoint(mount)) {
                mount.setProperty("hst:nochannelinfo", true);
            }
        }

        for (Node child : new NodeIterable(mount.getNodes())) {
            migrateMount(child);
        }
    }

    // hst:ismapped is inherited if not configured on the mount itself
    private boolean isMapped(final Node mount) throws RepositoryException {
        if (!mount.isNodeType("hst:mount")) {
            // default mapped is true
            return true;
        }
        if (mount.hasProperty("hst:ismapped")) {
            return mount.getProperty("hst:ismapped").getBoolean();
        }
        return isMapped(mount.getParent());
    }

    // hst:mountpoint is inherited if not configured on the 'mount' node itself
    private boolean hasMountPoint(final Node mount) throws RepositoryException {
        if (!mount.isNodeType("hst:mount")) {
            return false;
        }
        if (mount.hasProperty("hst:mountpoint")) {
            return true;
        }
        return hasMountPoint(mount.getParent());
    }

    // the target for the channel node and null if we can't find a target
    private String findChannelTargetConfiguration(final Node mount) throws RepositoryException {
        final Session session = mount.getSession();
        final String mountPoint =
                mount.getProperty("hst:mountpoint").getString();
        try {
            if (!session.nodeExists(mountPoint)) {
                getLogger().warn("Mount {} has an hst:mountpoint {} that does not exist.", mount.getPath(), mountPoint);
                return null;
            } else {
                final Node hstSiteNode = session.getNode(mountPoint);
                final String configurationPath;
                if (hstSiteNode.hasProperty("hst:configurationpath")) {
                    configurationPath =
                            hstSiteNode.getProperty("hst:configurationpath").getString();
                } else {
                    configurationPath = hstRoot +
                            "/hst:configurations/" + hstSiteNode.getName();
                }
                return configurationPath;
            }
        } catch (RepositoryException e) {
            getLogger().warn("Mount {} contains an invalid hst:mountpoint {} which is not a valid jcr path.Fix this. "
                    , mount.getPath(), mountPoint);
            return null;
        }
    }


    protected boolean shouldRun(final Session session, final boolean autoExportEnabled) throws RepositoryException {
        try {
            final boolean channelsNodeExists = session.nodeExists(hstRoot + "/hst:channels");
            if (channelsNodeExists && session.getNode(hstRoot + "/hst:channels").getNodes().getSize() > 0) {
                getLogger().info("{} will run because /hst:hst/hst:channels node exists and has children.", this);
                return true;
            }
        } catch (NamespaceException e) {
            getLogger().info("hst namespace has not yet been registered before. Nothing to migrate");
            return false;
        }
        return false;
    }


    private boolean basicHstConfigurationNodesPresent(final Session session) throws RepositoryException {
        if (!session.nodeExists(hstRoot + "/hst:sites")) {
            getLogger().info("Missing node /hst:hst/hst:sites.");
            return false;
        }
        if (session.getNode(hstRoot + "/hst:sites").getNodes().getSize() == 0) {
            getLogger().info("Missing children for /hst:hst/hst:sites.");
            return false;
        }
        if (!session.nodeExists(hstRoot + "/hst:configurations")) {
            getLogger().info("Missing node /hst:hst/hst:configurations.");
            return false;
        }
        if (session.getNode(hstRoot +
                "/hst:configurations").getNodes().getSize() == 0) {
            getLogger().info("Missing children for /hst:hst/hst:configurations.");
            return false;
        }
        if (!session.nodeExists(hstRoot + "/hst:hosts")) {
            getLogger().info("Missing node /hst:hst/hst:hosts.");
            return false;
        }
        if (session.getNode(hstRoot + "/hst:hosts").getNodes().getSize() == 0) {
            getLogger().info("Missing children for /hst:hst/hst:hosts.");
            return false;
        }

        return true;
    }

    private void removePreviewChannelsAndConfigurations(final Session session) throws RepositoryException {
        for (Node channelNode : new NodeIterable(session.getNode(hstRoot + "/hst:channels").getNodes())) {
            if (channelNode.getName().endsWith("-preview")) {
                getLogger().info("Remove preview channel {} node before migrating the channel nodes.", channelNode.getPath());
                channelNode.remove();
            }
        }
        for (Node configuration : new NodeIterable(session.getNode(hstRoot + "/hst:configurations").getNodes())) {
            if (configuration.getName().endsWith("-preview")) {
                getLogger().info("Remove configuration {} node before migrating the channel nodes.", configuration.getPath());
                configuration.remove();
            }
        }

    }

    private void migrateBlueprints(final Session session, final String hstRoot) throws RepositoryException {
        if (!session.nodeExists(hstRoot + "/hst:blueprints")) {
            getLogger().info("There are no blueprints to migrate");
            return;
        }
        for (Node blueprintNode : new NodeIterable(session.getNode(hstRoot + "/hst:blueprints").getNodes())) {
            migrateBlueprint(blueprintNode);
        }
    }

    private void migrateBlueprint(final Node blueprintNode) throws RepositoryException {
        if (!blueprintNode.hasNode("hst:channel")) {
            getLogger().info("No need to migrate blueprint '{}' because does not have an hst:channel node.", blueprintNode.getPath());
            return;
        }
        final Node blueprintConfigurationNode;
        if (blueprintNode.hasNode("hst:configuration")) {
            blueprintConfigurationNode = blueprintNode.getNode("hst:configuration");
        } else {
            blueprintConfigurationNode = blueprintNode.addNode("hst:configuration", "hst:configuration");
        }
        blueprintNode.getSession().move(blueprintNode.getPath() + "/hst:channel", blueprintConfigurationNode.getPath() + "/hst:channel");
    }


    @Override
    public String toString() {
        return this.getClass().getName() + "{}";
    }
}
