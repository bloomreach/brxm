/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONFIGURATION;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_WORKSPACE;

public class CompositeConfigurationNodes {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstNodeImpl.class);

    private List<String> compositeConfigurationDependenyPaths = new ArrayList<String>();

    private HstNode configurationRootNode;
    private Map<String, CompositeConfigurationNode> compositeConfigurationNodes = new HashMap<>();
    private List<String> compositeConfigurationNodeRelPaths;
    private boolean compositeConfigurationNodesLoaded;
    private List<HstNode> orderedRootConfigurationNodeInheritanceList = new ArrayList<>();
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

            String[] inherits = configurationRootNode.getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM);
            for (String inheritPath : inherits) {
                if (!inheritPath.startsWith("../")) {
                    log.warn("hst:inheritsfrom property must start with ../ but this is not the case for '{}'. We skip this inherit", configurationRootNode.getValueProvider().getPath());
                    continue;
                }
                String hstConfigsInheritedRelPath = inheritPath.substring(3);

                String absHstConfigsInheritedPath = configurationsNode.getValueProvider().getPath() + "/" + hstConfigsInheritedRelPath;
                // regardless whether absHstConfigsInheritedPath exists or not, add it to the compositeConfigurationDependencyPaths : If it gets
                // added later on, it does impact this CompositeConfigurationNodes
                compositeConfigurationDependenyPaths.add(absHstConfigsInheritedPath);
                compositeConfigurationDependenyPaths.addAll(createDependencyPaths(absHstConfigsInheritedPath, relPaths, false));
                HstNode inheritConfig = configurationsNode.getNode(hstConfigsInheritedRelPath);
                if (inheritConfig != null && isValidInheritedNode(inheritConfig)) {
                    orderedRootConfigurationNodeInheritanceList.add(inheritConfig);
                    if (inheritConfig.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM)) {
                        log.warn("Skipping inheritfrom property for configuration node '{}' because this node is already inherit. Not allowed to inherit again", inheritConfig.getValueProvider().getPath());
                    }
                } else {
                    log.error("Relative inherit path '{}' for node '{}' does not point to a node of type " +
                                    "'{}' or '{}', or it does not point to a child node of '{}', or it does not exist. Fix this path.",
                            new String[]{inheritPath, configurationRootNode.getValueProvider().getPath(),
                                    NODETYPE_HST_CONFIGURATION, NODETYPE_HST_WORKSPACE, NODENAME_HST_WORKSPACE});
                }
            }
        }

        // add the hst:default node as last one
        HstNode defaultConfigurationRootHstNode = configurationsNode.getNode(HstNodeTypes.NODENAME_HST_HSTDEFAULT);
        if (defaultConfigurationRootHstNode != null) {
            orderedRootConfigurationNodeInheritanceList.add(defaultConfigurationRootHstNode);
        }
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
                            && inherited.getValueProvider().getPath().endsWith(relativeInheritPath) ) {

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
     * descendants of those children)
     */
    public List<UUID> getCacheKey() {
        if (cacheKey != null) {
            return cacheKey;
        }
        long start = System.currentTimeMillis();
        List<UUID> uuids = new ArrayList<>();
        for (CompositeConfigurationNode compositeConfigurationNode : getCompositeConfigurationNodes().values()) {
            uuids.add(UUID.fromString(compositeConfigurationNode.getMainConfigNode().getValueProvider().getIdentifier()));
            for (HstNode compositeChild : compositeConfigurationNode.getCompositeChildren().values()) {
                uuids.add(UUID.fromString(compositeChild.getValueProvider().getIdentifier()));
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Creating cachekey took {} ms.", System.currentTimeMillis() - start);
        }
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
