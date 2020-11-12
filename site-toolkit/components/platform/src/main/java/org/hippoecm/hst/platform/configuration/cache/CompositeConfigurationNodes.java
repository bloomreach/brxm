/*
 *  Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONFIGURATION;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_WORKSPACE;

public class CompositeConfigurationNodes {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstNodeImpl.class);

    final private List<String> compositeConfigurationDependenyPaths = new ArrayList<String>();

    final private HstNode configurationRootNode;
    final private Map<String, CompositeConfigurationNode> compositeConfigurationNodes = new HashMap<>();
    final private List<String> compositeConfigurationNodeRelPaths;
    private boolean compositeConfigurationNodesLoaded;
    final private List<HstNode> orderedRootConfigurationNodeInheritanceList = new ArrayList<>();
    private List<UUID> cacheKey;

    public CompositeConfigurationNodes(HstNode configurationRootNode, String... relPaths) {

        this.configurationRootNode = configurationRootNode;
        compositeConfigurationNodeRelPaths = Arrays.asList(relPaths);
        compositeConfigurationDependenyPaths.add(configurationRootNode.getValueProvider().getPath());
        compositeConfigurationDependenyPaths.addAll(createDependencyPaths(configurationRootNode.getValueProvider().getPath(), relPaths, true));

        final HstNode configurationsNode = configurationRootNode.getParent();
        final String defaultConfigurationRootPath = configurationsNode.getValueProvider().getPath() + "/" + HstNodeTypes.NODENAME_HST_HSTDEFAULT;
        // hst:default is the implicit inherited configuration

        compositeConfigurationDependenyPaths.add(defaultConfigurationRootPath);
        compositeConfigurationDependenyPaths.addAll(createDependencyPaths(defaultConfigurationRootPath, relPaths, false));

        // Add all the explicitly inherited hst:configuration nodes.
        if (configurationRootNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM)) {
            final Set<String> alreadyInherited = new HashSet<>();
            String[] inherits = configurationRootNode.getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM);
            for (String inheritPath : inherits) {
                addInherited(configurationRootNode, configurationsNode, inheritPath, relPaths, alreadyInherited);
            }
        }

        // add the hst:default node as last one
        HstNode defaultConfigurationRootHstNode = configurationsNode.getNode(HstNodeTypes.NODENAME_HST_HSTDEFAULT);
        if (defaultConfigurationRootHstNode != null) {
            orderedRootConfigurationNodeInheritanceList.add(defaultConfigurationRootHstNode);
        }
    }

    private void addInherited(final HstNode currentConfigurationRootNode, final HstNode configurationsNode,
                              final String inheritPath, final String[] relPaths, final Set<String> alreadyInherited) {

        if (!inheritPath.startsWith("../")) {
            log.warn("Skipping invalid hst:inheritsfrom '{}' for '{}'. Property should start with '../'", inheritPath,
                    currentConfigurationRootNode.getValueProvider().getPath());
        }
        String hstConfigsInheritedRelPath = inheritPath.substring(3);
        String absHstConfigsInheritedPath = configurationsNode.getValueProvider().getPath() + "/" + hstConfigsInheritedRelPath;
        HstNode inheritConfig = configurationsNode.getNode(hstConfigsInheritedRelPath);
        // regardless whether absHstConfigsInheritedPath exists or not, add it to the compositeConfigurationDependencyPaths : If it gets
        // added later on, it does impact this CompositeConfigurationNodes
        compositeConfigurationDependenyPaths.add(absHstConfigsInheritedPath);
        compositeConfigurationDependenyPaths.addAll(createDependencyPaths(absHstConfigsInheritedPath, relPaths, false));

        if (inheritConfig != null) {
            if (alreadyInherited.contains(inheritConfig.getValueProvider().getIdentifier()  + "-" + inheritPath)) {
                log.debug("Already inherited configuration '{}' for '{}'", currentConfigurationRootNode.getValueProvider().getPath(), inheritPath);
                return;
            }
            alreadyInherited.add(inheritConfig.getValueProvider().getIdentifier() + "-" + inheritPath);
            if (!isValidInheritedNode(inheritConfig)) {
                log.error("Relative inherit path '{}' for node '{}' does not point to a node of type " +
                                "'{}' or '{}', or it does not point to a child node of '{}', or it does not exist. Fix this path.",
                        new String[]{inheritPath, currentConfigurationRootNode.getValueProvider().getPath(),
                                NODETYPE_HST_CONFIGURATION, NODETYPE_HST_WORKSPACE, NODENAME_HST_WORKSPACE});
                return;
            }
            orderedRootConfigurationNodeInheritanceList.add(inheritConfig);
            HstNode rootInheritedConfiguration = getHstConfiguration(inheritConfig);
            populateCascadingInheritanceList(currentConfigurationRootNode, configurationsNode, relPaths, alreadyInherited, hstConfigsInheritedRelPath, rootInheritedConfiguration);
        } else {
            // inherited config is null. This might be because for example 'subproject' inherits from '../corporate/hst:workspace/hst:pages' and
            // 'corporate' inherits from '../common/hst:workspace/hst:pages', and at the same time 'corporate' does not have 'hst:workspace/hst:pages'. Then,
            // still an attempt should be done to inherit '../common/hst:workspace/hst:pages'
            if (!hstConfigsInheritedRelPath.contains("/")) {
                return;
            }
            // try to do cascading population of the root configuration if there is root configuration
            final String rootName = StringUtils.substringBefore(hstConfigsInheritedRelPath, "/");
            HstNode rootInheritedConfiguration = configurationsNode.getNode(rootName);
            if (rootInheritedConfiguration != null) {
                // try cascading inheritance
                populateCascadingInheritanceList(currentConfigurationRootNode, configurationsNode, relPaths, alreadyInherited, hstConfigsInheritedRelPath, rootInheritedConfiguration);
            }
        }
    }

    private void populateCascadingInheritanceList(final HstNode configurationRootNode,
                                                  final HstNode configurationsNode,
                                                  final String[] relPaths,
                                                  final Set<String> alreadyInherited,
                                                  final String hstConfigsInheritedRelPath,
                                                  final HstNode rootInheritedConfiguration) {

        if (rootInheritedConfiguration.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM)) {
            String[] cascadingInheritedPaths = rootInheritedConfiguration.getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM);
            for (String cascadingInheritedPath : cascadingInheritedPaths) {
                if (hstConfigsInheritedRelPath.contains("/")) {
                    // inheritance is something like ../common/hst:workspace or ../common/hst:workspace/hst:pages
                    // only inherit via cascading the same relative path
                    final String relPath = StringUtils.substringAfter(hstConfigsInheritedRelPath, "/");
                    if (cascadingInheritedPath.endsWith("/" + relPath)) {
                        // if inherited config exists, inherit, otherwise skip
                        final HstNode cascading = configurationsNode.getNode(StringUtils.substringBefore(hstConfigsInheritedRelPath, "/"));
                        if (cascading == null) {
                            log.debug("No cascading config present at '{}' for '{}'. Skip it.", relPath, configurationRootNode.getValueProvider().getPath());
                        } else {
                            addInherited(cascading, configurationsNode, cascadingInheritedPath, relPaths, alreadyInherited);
                        }
                    } else {
                        log.debug("Do not cascade inheritance for '{}' for '{}'", cascadingInheritedPath, configurationRootNode.getValueProvider().getPath());
                    }
                } else {
                    // cascading inheritance
                    final HstNode cascading = configurationsNode.getNode(hstConfigsInheritedRelPath);
                    if (cascading == null) {
                        log.debug("No cascading config present at '{}' for '{}'. Skip it.", hstConfigsInheritedRelPath, configurationRootNode.getValueProvider().getPath());
                    } else {
                        addInherited(cascading, configurationsNode, cascadingInheritedPath, relPaths, alreadyInherited);
                    }
                }
            }
        }
    }

    private HstNode getHstConfiguration(final HstNode inheritConfig) {
        if  (NODETYPE_HST_CONFIGURATION.equals(inheritConfig.getNodeTypeName())) {
            return inheritConfig;
        }
        if (NODETYPE_HST_WORKSPACE.equals(inheritConfig.getNodeTypeName())) {
            return inheritConfig.getParent();
        }
        if (NODETYPE_HST_WORKSPACE.equals(inheritConfig.getParent().getNodeTypeName())) {
            return inheritConfig.getParent().getParent();
        }
        throw new IllegalArgumentException(String.format("Illegal Inherited configuration '%s' should not be possible at this point.",
                inheritConfig.getValueProvider().getPath()));
    }

    /**
     * Valid inherited configuration node is one that meets one of the following criteria
     * <lu>
     * <li>points to a node of type hst:configuration node, for example hst:inheritsfrom = ../common</li>
     * <li>points to a node of type hst:workspace node, for example hst:inheritsfrom = ../common/hst:workspace</li>
     * <li>points to a child node of a node of type hst:workspace, for example hst:inheritsfrom =
     * ../common/hst:workspace/hst:pages</li>
     * </lu>
     *
     * @param inheritConfig
     * @return
     */
    private boolean isValidInheritedNode(final HstNode inheritConfig) {
        return (NODETYPE_HST_CONFIGURATION.equals(inheritConfig.getNodeTypeName())
                || NODETYPE_HST_WORKSPACE.equals(inheritConfig.getNodeTypeName())
                || NODETYPE_HST_WORKSPACE.equals(inheritConfig.getParent().getNodeTypeName()));
    }

    public HstNode getConfigurationRootNode() {
        return configurationRootNode;
    }


    public Map<String, CompositeConfigurationNode> getCompositeConfigurationNodes() {
        if (compositeConfigurationNodesLoaded) {
            return compositeConfigurationNodes;
        }
        compositeConfigurationNodesLoaded = true;
        for (String compositeConfigurationNodeRelPath : compositeConfigurationNodeRelPaths) {

            // a mainConfigNode is a node like hst:sitemap or hst:pages for example
            HstNode mainConfigNode = configurationRootNode.getNode(compositeConfigurationNodeRelPath);

            if (mainConfigNode == null) {
                // check whether there is a mainConfigNode in the workspace. Take that one if present
                mainConfigNode = configurationRootNode.getNode(NODENAME_HST_WORKSPACE + "/" + compositeConfigurationNodeRelPath);
            }

            List<HstNode> fallbackMainConfigNodes = new ArrayList<>();

            final String relativeInheritPath = compositeConfigurationNodeRelPath;
            final boolean isMainConfigNodeInherited = (mainConfigNode == null);

            for (HstNode inherited : orderedRootConfigurationNodeInheritanceList) {
                // for hst:containers the relativeInheritPath starts with hst:workspace
                String workspaceAccountedRelativeInheritPath = relativeInheritPath;
                if (relativeInheritPath.startsWith(NODENAME_HST_WORKSPACE)) {
                    if (inherited.getNodeTypeName().equals(NODETYPE_HST_WORKSPACE)) {
                        log.debug("Merging explicitly inherited workspace configuration for '{}'",
                                inherited.getValueProvider().getPath());
                        // hst:inheritsfrom is something like ../common/hst:workspace

                        // remove the hst:workspace part : in inherited HstNode already
                        workspaceAccountedRelativeInheritPath = relativeInheritPath.substring(NODENAME_HST_WORKSPACE.length() + 1);
                    } else if (inherited.getParent().getNodeTypeName().equals(NODETYPE_HST_WORKSPACE)
                            && inherited.getValueProvider().getPath().endsWith(relativeInheritPath)) {

                        log.debug("Merging explicitly inherited workspace configuration for '{}'",
                                inherited.getValueProvider().getPath());
                        // hst:inheritsfrom is something like ../common/hst:workspace/hst:pages
                        workspaceAccountedRelativeInheritPath = "";

                    } else {
                        log.debug("Do not merge workspace nodes since not explicitly inherited");
                        continue;
                    }
                } else if (inherited.getParent().getNodeTypeName().equals(NODETYPE_HST_WORKSPACE)) {
                    if (inherited.getValueProvider().getPath().endsWith(relativeInheritPath)) {
                        log.debug("Merging explicitly inherited workspace child configuration for '{}'",
                                inherited.getValueProvider().getPath());
                        // hst:inheritsfrom is something like ../common/hst:workspace/hst:pages
                        workspaceAccountedRelativeInheritPath = "";
                    } else {
                        log.debug("Do not merge workspace nodes '{}' since '{}' is explicitly inherited",
                                relativeInheritPath, inherited.getValueProvider().getPath());
                        continue;
                    }
                }
                if (mainConfigNode == null) {
                    if (workspaceAccountedRelativeInheritPath.isEmpty()) {
                        mainConfigNode = inherited;
                    } else {
                        mainConfigNode = inherited.getNode(workspaceAccountedRelativeInheritPath);
                    }
                } else {
                    HstNode inheritedMainConfigNode;
                    if (workspaceAccountedRelativeInheritPath.isEmpty()) {
                        inheritedMainConfigNode = inherited;
                    } else {
                        inheritedMainConfigNode = inherited.getNode(workspaceAccountedRelativeInheritPath);
                    }
                    if (inheritedMainConfigNode != null) {
                        fallbackMainConfigNodes.add(inheritedMainConfigNode);
                    }
                }
            }

            if (mainConfigNode == null) {
                continue;
            }
            // do the actual composite: start with 'mainConfigNode' and then merge in the nodes from inheritanceListForMainConfigNode
            CompositeConfigurationNode compositeConfigurationNode = new CompositeConfigurationNode(mainConfigNode, fallbackMainConfigNodes, isMainConfigNodeInherited);
            compositeConfigurationNodes.put(compositeConfigurationNodeRelPath, compositeConfigurationNode);

        }
        return compositeConfigurationNodes;
    }

    /**
     * @return the cachekey for this {@link CompositeConfigurationNodes} object : It is the UUIDs of all main
     * configuration nodes (like hst:pages, hst:templates) *PLUS* their direct children (and thus not
     * descendants of those children) plus the UUID of the 'configurationRootNode'
     */
    public List<UUID> getCacheKey() {
        if (cacheKey != null) {
            return cacheKey;
        }
        List<UUID> uuids = new ArrayList<>();
        for (CompositeConfigurationNode compositeConfigurationNode : getCompositeConfigurationNodes().values()) {
            uuids.add(UUID.fromString(compositeConfigurationNode.getMainConfigNode().getValueProvider().getIdentifier()));
            for (HstNode compositeChild : compositeConfigurationNode.getCompositeChildren().values()) {
                uuids.add(UUID.fromString(compositeChild.getValueProvider().getIdentifier()));
            }
        }
        uuids.add(UUID.fromString(configurationRootNode.getValueProvider().getIdentifier()));
        cacheKey = uuids;
        return cacheKey;
    }

    private List<String> createDependencyPaths(final String rootPath, final String[] relPaths, boolean includeWorkspacePath) {
        List<String> paths = new ArrayList<>();
        boolean isInheritedWorkspace = false;
        if (rootPath.contains("/" + NODENAME_HST_WORKSPACE)) {
            // explicitly inherited workspace (eg hst:inheritsfrom = ../common/hst:workspace)
            isInheritedWorkspace = true;
            final String mainConfigNodeName = StringUtils.substringAfter(rootPath, "/" + NODENAME_HST_WORKSPACE + "/");
            if (!mainConfigNodeName.isEmpty()) {
                // explicitly inherited workspace main config node (eg hst:inheritsfrom = ../common/hst:workspace/hst:pages))
                // just return this explicit dependency path
                paths.add(rootPath);
                return paths;
            }
        }
        for (String relPath : relPaths) {
            if (isInheritedWorkspace) {
                if (relPath.startsWith(NODENAME_HST_WORKSPACE + "/")) {
                    // hst:workspace/hst:containers
                    paths.add(rootPath + relPath.substring(NODENAME_HST_WORKSPACE.length()));
                } else {
                    paths.add(rootPath + "/" + relPath);
                }
            } else {
                paths.add(rootPath + "/" + relPath);
                if (includeWorkspacePath && !relPath.startsWith(NODENAME_HST_WORKSPACE)) {
                    // also include the workspace variant as below workspace nodes like hst:sitemenus or hst:sitemap can also be found
                    // if relPath already starts with hst:workspace it doesn't really matter. No bother to check
                    paths.add(rootPath + "/" + NODENAME_HST_WORKSPACE + "/" + relPath);
                }
            }
        }
        return paths;
    }

    public List<String> getCompositeConfigurationDependencyPaths() {
        return compositeConfigurationDependenyPaths;
    }

    public static class CompositeConfigurationNode {

        private HstNode mainConfigNode;
        private Map<String, HstNode> compositeChildren = new HashMap<>();

        public CompositeConfigurationNode(final HstNode mainConfigNode,
                                          final List<HstNode> fallbackMainConfigNodes,
                                          final boolean isMainConfigNodeInherited) {
            this.mainConfigNode = mainConfigNode;

            HstNode mainConfigNodeInWorkspace = null;
            final HstNode parent = mainConfigNode.getParent();
            if (!parent.getName().equals(NODENAME_HST_WORKSPACE) && !isMainConfigNodeInherited) {
                // mainConfigNode is not already a node in workspace
                mainConfigNodeInWorkspace = parent.getNode(NODENAME_HST_WORKSPACE + "/" + mainConfigNode.getName());
            }

            for (HstNode child : mainConfigNode.getNodes()) {
                compositeChildren.put(child.getName(), child);
            }
            if (mainConfigNodeInWorkspace != null) {
                // MERGE now the workspace nodes as well
                for (HstNode child : mainConfigNodeInWorkspace.getNodes()) {
                    if (compositeChildren.containsKey(child.getName())) {
                        HstNode present = compositeChildren.get(child.getName());
                        log.warn("Please correct the configuration because duplicate configuration nodes found. Not allowed to have same node names" +
                                " in hst:workspace below main config nodes (like hst:sitemap, hst:sitemenu, etc). Duplicates" +
                                " are '{}' and '{}'. Node from workspace won't be used.", present.toString(), child.toString());
                    } else {
                        compositeChildren.put(child.getName(), child);
                    }
                }
            }
            for (HstNode fallbackMainConfigNode : fallbackMainConfigNodes) {
                for (HstNode fallBackChild : fallbackMainConfigNode.getNodes()) {
                    if (!compositeChildren.containsKey(fallBackChild.getName())) {
                        compositeChildren.put(fallBackChild.getName(), fallBackChild);
                    }
                }
            }
        }

        public HstNode getMainConfigNode() {
            return mainConfigNode;
        }

        public Map<String, HstNode> getCompositeChildren() {
            return compositeChildren;
        }

    }

}
