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
package org.hippoecm.hst.configuration.model;

import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.LoggerFactory;


public class HstSiteRootNodeImpl extends HstNodeImpl implements HstSiteRootNode {


    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstSiteRootNodeImpl.class);

    private String contentPath;
    private String canonicalContentPath;
    private long version = -1;
    private String configurationPath;
    
    public HstSiteRootNodeImpl(Node siteRootNode, HstNode parent, String rootConfigurationsPath)
            throws RepositoryException {
        
        // do not load child nodes as this is the entire content
        super(siteRootNode, parent, false);
        
        try {
            if (getValueProvider().getString(HstNodeTypes.SITE_CONFIGURATIONPATH) != null) {
                log.warn("'{}' is using deprecated property '{}'. This property is not used any more. Please remove this property and make " +
                        "sure the hst:site has the same name as the hst:configuration node (minus the version info in that name)",
                        siteRootNode.getPath(), HstNodeTypes.SITE_CONFIGURATIONPATH);
            }
            if (getValueProvider().hasProperty(HstNodeTypes.SITE_VERSION)) {
                version = getValueProvider().getLong(HstNodeTypes.SITE_VERSION).longValue();
            }
            
            configurationPath = rootConfigurationsPath + "/" + findConfigurationNameForSite(this.getValueProvider().getName(), version);

            if(siteRootNode.hasNode(HstNodeTypes.NODENAME_HST_CONTENTNODE)) {
                Node contentNode = siteRootNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
                contentPath = contentNode.getPath();
                
                // fetch the mandatory hippo:docbase property to retrieve the canonical node
                if(contentNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    String docbaseUuid = contentNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    
                    try {
                        // test whether docbaseUuid is valid uuid. If UUID.fromString fails, an IllegalArgumentException is thrown
                        UUID.fromString(docbaseUuid);
                        Node node =  contentNode.getSession().getNodeByIdentifier(docbaseUuid);
                        this.canonicalContentPath = node.getPath();

                    } catch (IllegalArgumentException e) {
                        log.warn("Docbase from '{}' does not contain a valid uuid. Content mirror is broken", contentNode.getPath());
                    } catch (ItemNotFoundException e) {
                        log.warn("ItemNotFoundException: Content mirror is broken. ", e.toString());
                    } catch (RepositoryException e) {
                        log.error("RepositoryException: Content mirror is broken. ", e);
                    }
                } else {
                    // contentNode is not a mirror. Take the canonical path to be the same
                    this.canonicalContentPath = this.contentPath;
                }
            }
        } catch (RepositoryException e) {
            log.warn("Repository Exception during instantiating '"+getValueProvider().getName()+"'. Skipping subsite.");
            throw e;
        }
        
    }

    
    
    private String findConfigurationNameForSite(final String name, final long version) {
        String configurationName;
        // if ends with -preview, we strip it off.
        configurationName = StringUtils.substringBefore(name, "-preview");

        if (version > -1) {
            configurationName = configurationName+"-v"+version;
        }
        return configurationName;
    }
    
    @Override
    public String getCanonicalContentPath() {
        return canonicalContentPath;
    }

    @Override
    public long getVersion() {
        return version;
    }
    
    @Override
    public String getConfigurationPath(){
        return configurationPath;
    }

    @Override
    public String getContentPath() {
        return contentPath;
    }

    
}
