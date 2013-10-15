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
import java.util.List;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.model.HstNode;
import org.slf4j.LoggerFactory;

/*
 * <p>
 *   Note that this class is <strong>not</strong> thread-safe : It should not be accessed by concurrent threads
 * </p>
 */
public class HstSiteConfigurationRootNodeImpl extends HstNodeImpl {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstNodeImpl.class);

    private List<String> configurationDependenyPaths = new ArrayList<String>();

    public HstSiteConfigurationRootNodeImpl(HstNodeImpl configurationRootNode) {
        super(configurationRootNode, null);

        configurationDependenyPaths.add(configurationRootNode.getValueProvider().getPath());
        // hst:default is the implicit inherited configuration
        final HstNode configurationsNode = configurationRootNode.getParent();
        configurationDependenyPaths.add(configurationsNode.getValueProvider().getPath() + "/hst:default");
        // Load all the explicitly inherited hst:configuration nodes.
        if(configurationRootNode.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM)) {
            if(HstNodeTypes.NODETYPE_HST_CONFIGURATION.equals(configurationRootNode.getNodeTypeName())) {
               String[] inherits = getValueProvider().getStrings(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM);
               for(String inheritPath : inherits) {
                   if(!inheritPath.startsWith("../")) {
                       log.warn("hst:inheritsfrom property must start with ../ but this is not the case for '{}'. We skip this inherit", configurationRootNode.getValueProvider().getPath());
                       continue;
                   }
                   String hstConfigsInheritedRelPath = inheritPath.substring(3);

                   String absHstConfigsInheritedPath = configurationsNode.getValueProvider().getPath() + "/" + hstConfigsInheritedRelPath;
                   // regardless whether absHstConfigsInheritedPath exists or not, add it to the configurationDependenyPaths : If it gets
                   // added later on, it does impact this HstSiteConfigurationRootNodeImpl
                   configurationDependenyPaths.add(absHstConfigsInheritedPath);
                   HstNode inheritConfig = configurationsNode.getNode(hstConfigsInheritedRelPath);
                   if(inheritConfig != null && HstNodeTypes.NODETYPE_HST_CONFIGURATION.equals(inheritConfig.getNodeTypeName())) {
                       // mark the loaded childs as inherited, hence 'true'
                       if(inheritConfig.getValueProvider().hasProperty(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM)) {
                           log.error("Skipping inheritfrom property for configuration node '{}' because this node is already inherit. Not allowed to inherit again", inheritConfig.getValueProvider().getPath());
                       } else {
                          merge(inheritConfig);
                       }
                   } else {
                       log.error("Relative inherit path '{}' for node '{}' does not point to a node of type or does not exist. '"+HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM+"'. Fix this path.", inheritPath, getValueProvider().getPath());
                   }
               }
            } else {
                log.info("Found the property '{}' on node '{}' but the property only inherits when configured on nodes of type 'hst:configuration'", HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM, configurationRootNode.getValueProvider().getPath());
            }
        }
        if(!configurationRootNode.getValueProvider().getName().equals(HstNodeTypes.NODENAME_HST_HSTDEFAULT)) {
            // Load the implicitly inherited hst:default hst:configuration nodes
            HstNode hstDefaultConfig = configurationsNode.getNode(HstNodeTypes.NODENAME_HST_HSTDEFAULT);
            if(hstDefaultConfig == null) {
                log.info("There is no hstDefaultConfig to merge");
            } else {
                merge(hstDefaultConfig);
            } 
        }
    }

    /**
     * @return all the configuration root paths this hst configuration root node depends on : It contains at least the path of
     * the jcr root node for this HstSiteConfigurationRootNodeImpl, but also the paths of the inherited configurations
     * through hst:inheritsfrom
     */
    public List<String> getConfigurationDependenyPaths() {
        return configurationDependenyPaths;
    }


    private void merge(HstNode inheritedConfig) {
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_CATALOG);
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_SITEMENUS);
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_SITEMAP);
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_SITEMAPITEMHANDLERS);
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_PAGES);
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_COMPONENTS);
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_TEMPLATES);
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_WORKSPACE);
    }

    /*
     * For merging, we now do not worry about 'cloning' the inherited HstNode. This would be a cleaner way to merge, as now a single HstNode instances
     * can below to different configuration. However, as we do not modify the inherited HstNode, it does not matter for now
     */
    private void merge(HstNode inheritedConfig, String nodeName) {
        HstNode inheritedNode = inheritedConfig.getNode(nodeName);
        if(inheritedNode == null) {
            // inherited node does not have the nodeName, return
            return;
        }
        
        HstNode hstNode = getNode(nodeName);
        if(hstNode == null) {
            // inherit the node from inheritedConfig. 
            HstNode copy = new HstNodeImpl(true, (HstNodeImpl)inheritedNode, this);
            addNode(nodeName, copy);
        } else { 
            // add the direct children that are in the inheritedConfig but not in the current.
            for(HstNode inheritedChild : inheritedNode.getNodes()) {
                if (hstNode.getNode(inheritedChild.getValueProvider().getName()) == null) {
                    // inherit the child node.
                    HstNode copy = new HstNodeImpl(true, (HstNodeImpl) inheritedChild, hstNode);
                    hstNode.addNode(inheritedChild.getValueProvider().getName(), copy);
                }
            }
        }
        
    }

}
