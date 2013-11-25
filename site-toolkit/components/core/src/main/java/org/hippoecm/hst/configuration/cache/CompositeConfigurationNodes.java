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
package org.hippoecm.hst.configuration.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.slf4j.LoggerFactory;

public class CompositeConfigurationNodes {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstNodeImpl.class);

    private List<String> compositeConfigurationDependenyPaths = new ArrayList<String>();

    private HstNode configurationRootNode;
    private Map<String, CompositeConfigurationNode> compositeConfigurationNodes = new HashMap<>();
    private List<String> compositeConfigurationNodeNames;
    private boolean compositeConfigurationNodesLoaded;
    private List<HstNode> orderedRootConfigurationNodeInheritanceList = new ArrayList<>();
    private List<UUID> cacheKey;


    public CompositeConfigurationNodes(HstNode configurationRootNode, String... nodeNames) {

        this.configurationRootNode = configurationRootNode;
        compositeConfigurationNodeNames = Arrays.asList(nodeNames);
        compositeConfigurationDependenyPaths.add(configurationRootNode.getValueProvider().getPath());
        compositeConfigurationDependenyPaths.addAll(createDependencyPaths(configurationRootNode.getValueProvider().getPath(), nodeNames));

        final HstNode configurationsNode = configurationRootNode.getParent();
        final String defaultConfigurationRootPath = configurationsNode.getValueProvider().getPath() + "/"+HstNodeTypes.NODENAME_HST_HSTDEFAULT;
        // hst:default is the implicit inherited configuration

        compositeConfigurationDependenyPaths.add(defaultConfigurationRootPath);
        compositeConfigurationDependenyPaths.addAll(createDependencyPaths(defaultConfigurationRootPath, nodeNames));

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
                // regardless whether absHstConfigsInheritedPath exists or not, add it to the compositeConfigurationDependenyPaths : If it gets
                // added later on, it does impact this CompositeConfigurationNodes
                compositeConfigurationDependenyPaths.add(absHstConfigsInheritedPath);
                compositeConfigurationDependenyPaths.addAll(createDependencyPaths(absHstConfigsInheritedPath, nodeNames));
                HstNode inheritConfig = configurationsNode.getNode(hstConfigsInheritedRelPath);
                if (inheritConfig != null && HstNodeTypes.NODETYPE_HST_CONFIGURATION.equals(inheritConfig.getNodeTypeName())) {
                    orderedRootConfigurationNodeInheritanceList.add(inheritConfig);
                    if (inheritConfig.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM)) {
                        log.error("Skipping inheritfrom property for configuration node '{}' because this node is already inherit. Not allowed to inherit again", inheritConfig.getValueProvider().getPath());
                    }
                } else {
                    log.error("Relative inherit path '{}' for node '{}' does not point to a node of type '{}' or does not exist. Fix this path.",
                            new String[]{inheritPath, configurationRootNode.getValueProvider().getPath(), HstNodeTypes.NODETYPE_HST_CONFIGURATION});
                }
            }
        }

        // add the hst:default node as last one
        HstNode defaultConfigurationRootHstNode = configurationsNode.getNode(HstNodeTypes.NODENAME_HST_HSTDEFAULT);
        if (defaultConfigurationRootHstNode != null) {
            orderedRootConfigurationNodeInheritanceList.add(defaultConfigurationRootHstNode);
        }
    }

    public HstNode getConfigurationRootNode() {
        return configurationRootNode;
    }


    public Map<String, CompositeConfigurationNode> getCompositeConfigurationNodes() {
        if (compositeConfigurationNodesLoaded) {
            return compositeConfigurationNodes;
        }
        compositeConfigurationNodesLoaded = true;
        for (String compositeConfigurationNodeName : compositeConfigurationNodeNames) {

            // a mainConfigNode is a node like hst:sitemap or hst:pages for example
            HstNode mainConfigNode =  configurationRootNode.getNode(compositeConfigurationNodeName);
            List<HstNode> fallbackMainConfigNodes = new ArrayList<>();
            // see if there is an inherited main config node
            for (HstNode inherited : orderedRootConfigurationNodeInheritanceList) {
                if (mainConfigNode == null) {
                    mainConfigNode =  inherited.getNode(compositeConfigurationNodeName);
                } else {
                    HstNode inheritedMainConfigNode = inherited.getNode(compositeConfigurationNodeName);
                    if (inheritedMainConfigNode != null) {
                        fallbackMainConfigNodes.add(inheritedMainConfigNode);
                    }
                }
            }
            if (mainConfigNode == null) {
                continue;
            }
            // do the actual composite: start with 'mainConfigNode' and then merge in the nodes from inheritanceListForMainConfigNode
            CompositeConfigurationNode compositeConfigurationNode = new CompositeConfigurationNode(mainConfigNode, fallbackMainConfigNodes);
            compositeConfigurationNodes.put(compositeConfigurationNodeName, compositeConfigurationNode);

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
            log.debug("Creating cachekey took {} ms.", System.currentTimeMillis() -start);
        }
        cacheKey = uuids;
        return cacheKey;
    }

    private List<String> createDependencyPaths(final String rootPath, final String[] nodeNames) {
        List<String> paths = new ArrayList<>();
        for (String nodeName : nodeNames) {
            paths.add(rootPath + "/" + nodeName);
        }
        return paths;
    }

    public List<String> getCompositeConfigurationDependenyPaths() {
        return compositeConfigurationDependenyPaths;
    }

    public class CompositeConfigurationNode {

        private HstNode mainConfigNode;
        private Map<String, HstNode> compositeChildren = new HashMap<>();

        public CompositeConfigurationNode(final HstNode mainConfigNode, final List<HstNode> fallbackMainConfigNodes) {
            this.mainConfigNode = mainConfigNode;
            for (HstNode child : mainConfigNode.getNodes()) {
                compositeChildren.put(child.getName(), child);
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
