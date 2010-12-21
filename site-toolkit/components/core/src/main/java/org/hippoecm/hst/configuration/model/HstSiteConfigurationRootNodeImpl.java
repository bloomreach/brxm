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
package org.hippoecm.hst.configuration.model;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.slf4j.LoggerFactory;

public class HstSiteConfigurationRootNodeImpl extends HstNodeImpl implements HstNode {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstNodeImpl.class);
  
    public HstSiteConfigurationRootNodeImpl(Node jcrNode, HstNode parent, HstManagerImpl hstManagerImpl) throws HstNodeException {
        super(jcrNode, parent, true);
      
        // Load all the explicitly inherited hst:configuration nodes.
        try {
            if(jcrNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM)) {
                if(jcrNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONFIGURATION)) {
                    try {
                       Node inheritNode = jcrNode.getNode(jcrNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM).getString());
                       if(inheritNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONFIGURATION)) {
                           // mark the loaded childs as inherited, hence 'true'
                           if(inheritNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM)) {
                               log.error("Skipping inheritfrom property for configuration node '{}' because this node is already inherit. Not allowed to inherit again", inheritNode.getPath());
                           } else {
                               HstSiteConfigurationRootNodeImpl inheritedConfig = (HstSiteConfigurationRootNodeImpl)hstManagerImpl.getConfigurationRootNodes().get(inheritNode.getPath());
                               if(inheritedConfig == null) {
                               // not yet loaded, load now, then merge
                               try {
                                   inheritedConfig = new HstSiteConfigurationRootNodeImpl(inheritNode, null, hstManagerImpl);
                                   // store the loaded inherited config, otherwise it will be reloaded over and over
                                   hstManagerImpl.getConfigurationRootNodes().put(inheritNode.getPath(), inheritedConfig);
                               } catch (HstNodeException e) {
                                   log.error("Incorrect configured inherited configuration at '{}'. Cannot load inherited config", inheritNode.getPath());
                               }
                                                   }
                               if(inheritedConfig != null) {
                                   merge(inheritedConfig);
                               } else {
                                   log.error("Inherited configuration as '{}' is not loaded correctly. Cannot inherit", inheritNode.getPath());
                               }
                           }
                       } else {
                           log.error("Relative inherit path '{}' for node '{}' does not point to a node of type '"+HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM+"'. Fix this path.", jcrNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM).getString(), getValueProvider().getPath());
                       }
                    } catch (PathNotFoundException e) {
                        log.error("Relative inherit path '"+jcrNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM).getString()+"' for node '"+getValueProvider().getPath()+"' can not be found. Fix this path.");
                    }
                } else {
                    log.info("Found the property '{}' on node '{}' but the property only inherits when configured on nodes of type 'hst:configuration'", HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM, jcrNode.getPath());
                }
                
                // Load the implicitly inherited hst:configuration nodes: When there is a hst:configuration node that is called 'hst:default' then we need to merge this.
                HstSiteConfigurationRootNodeImpl hstDefaultConfig = (HstSiteConfigurationRootNodeImpl)hstManagerImpl.getConfigurationRootNodes().get(HstNodeTypes.NODENAME_HST_HSTDEFAULT);
                if(hstDefaultConfig == null) {
                    // not yet loaded, load now, then merge
                    if(jcrNode.getParent().hasNode(HstNodeTypes.NODENAME_HST_HSTDEFAULT)) {
                        Node hstDefaultNode = jcrNode.getParent().getNode(HstNodeTypes.NODENAME_HST_HSTDEFAULT);
                        try {
                            hstDefaultConfig = new HstSiteConfigurationRootNodeImpl(hstDefaultNode, null, hstManagerImpl);
                        } 
                        catch (HstNodeException e) {
                            log.error("Incorrect configured hst default configuration at '{}'. Cannot load hstdefault config", hstDefaultNode.getPath());
                        }
                        // store the loaded inherited config, otherwise it will be reloaded over and over
                        hstManagerImpl.getConfigurationRootNodes().put(hstDefaultNode.getPath(), hstDefaultConfig);
                    } else {
                        log.info("There is no default configuration node at '{}'. Skip hstdefault configuration", jcrNode.getParent().getPath() + "/"+ HstNodeTypes.NODENAME_HST_HSTDEFAULT);
                    }
                }
                if(hstDefaultConfig != null) {
                    merge(hstDefaultConfig);
                } else {
                    log.info("There is no hstDefaultConfig to merge");
                }
            }
        } catch (RepositoryException e) {
            throw new HstNodeException(e);
        }
        
        
    }

    private void merge(HstSiteConfigurationRootNodeImpl inheritedConfig) {
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_CATALOG);
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_SITEMAP);
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_SITEMAPITEMHANDLERS);
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_PAGES);
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_COMPONENTS);
        merge(inheritedConfig, HstNodeTypes.NODENAME_HST_TEMPLATES);
    }

    /*
     * For merging, we now do not worry about 'cloning' the inherited HstNode. This would be a cleaner way to merge, as now a single HstNode instances
     * can below to different configuration. However, as we do not modify the inherited HstNode, it does not matter for now
     */
    private void merge(HstSiteConfigurationRootNodeImpl inheritedConfig, String nodeName) {
        HstNode inheritedNode = inheritedConfig.getNode(nodeName);
        if(inheritedNode == null) {
            // inherited node does not have the nodeName, return
            return;
        }
        
        HstNode hstNode = getNode(nodeName);
        if(hstNode == null) {
            // inherit the node from inheritedConfig
            this.setNode(nodeName, inheritedNode);
        } else {
            // add the direct children that are in the inheritedConfig but not in the current.
            for(HstNode inheritedChild : inheritedNode.getNodes()) {
                if(hstNode.getNode(inheritedChild.getValueProvider().getName()) == null) {
                    // inherit the child node.
                    ((HstNodeImpl)hstNode).setNode(inheritedChild.getValueProvider().getName(), inheritedChild);
                }
            }
        }
        
    }

}
