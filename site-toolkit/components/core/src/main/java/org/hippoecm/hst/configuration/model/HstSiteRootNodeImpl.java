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
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.LoggerFactory;


public class HstSiteRootNodeImpl extends HstNodeImpl implements HstSiteRootNode {


    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstSiteRootNodeImpl.class);

    private String contentPath;
    private long version = -1;
    private String configurationPath;
    
    public HstSiteRootNodeImpl(Node siteRootNode, HstNode parent, String rootConfigurationsPath)
            throws RepositoryException {
        
        // do not load child nodes as this is the entire content
        super(siteRootNode, parent, false);
        Session session = siteRootNode.getSession();
        try {
            String configurationName = findConfigurationNameForSite();
            if (configurationName == null) {
                return;
            }
            if (getValueProvider().hasProperty(HstNodeTypes.SITE_VERSION)) {
                version = getValueProvider().getLong(HstNodeTypes.SITE_VERSION).longValue();
            }
            
            configurationPath = rootConfigurationsPath + "/" + findConfigurationNameAndVersionForSite(configurationName, version);

            if (siteRootNode.hasProperty(HstNodeTypes.SITE_CONTENT)) {
                String siteContentPathOrUuid = siteRootNode.getProperty(HstNodeTypes.SITE_CONTENT).getString();
                if (siteContentPathOrUuid.startsWith("/")) {
                    try {
                        Node node = session.getNode(siteContentPathOrUuid);
                        contentPath = node.getPath();
                    } catch (PathNotFoundException e) {
                        log.warn("PathNotFoundException: Cannot lookup content node for path '" + siteContentPathOrUuid + "'. ");
                    } catch (RepositoryException e) {
                        log.warn("RepositoryException: Cannot lookup content node for path '" + siteContentPathOrUuid + "'. ", e);
                    }
                } else {
                    contentPath = nodePathForUuid(session, siteRootNode, siteContentPathOrUuid);
                }
            } else if(siteRootNode.hasNode(HstNodeTypes.NODENAME_HST_CONTENTNODE)) {
                Node facetSelectContentNode = siteRootNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE);
                String facetSelectContentPath = facetSelectContentNode.getPath();

                // fetch the mandatory hippo:docbase property to retrieve the canonical node
                if(facetSelectContentNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                    String docbaseUuid = facetSelectContentNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    contentPath = nodePathForUuid(session, facetSelectContentNode, docbaseUuid);
                    log.warn("Having a hst:content node at '{}' is deprecated. Instead, at '{}' add a String property 'hst:content' with value " +
                            "'{}' OR value '{}'. Note that the path '{}' is preferred above setting a uuid.",
                            new String[]{facetSelectContentNode.getPath(), siteRootNode.getPath(), contentPath, docbaseUuid, contentPath});
                } else {
                    throw new IllegalStateException("Node at path '"+facetSelectContentPath+"' must always be of type '" +
                            HippoNodeType.NT_FACETSELECT + "'");
                }
            }
        } catch (RepositoryException e) {
            log.warn("Repository Exception during instantiating '"+getValueProvider().getName()+"'. Skipping subsite.");
            throw e;
        }
        
    }

    private String nodePathForUuid(final Session session, final Node forNode, final String docbaseUuid) throws RepositoryException {
        try {
            // test whether docbaseUuid is valid uuid. If UUID.fromString fails, an IllegalArgumentException is thrown
            UUID.fromString(docbaseUuid);
            Node node =  session.getNodeByIdentifier(docbaseUuid);
            return node.getPath();
        } catch (IllegalArgumentException e) {
            log.warn("Docbase '{}' is not contain a valid uuid. Cannot lookup content node for '{}'.", docbaseUuid, forNode.getPath());
        } catch (ItemNotFoundException e) {
            log.warn("ItemNotFoundException: Cannot find content node for uuid '{}'.",docbaseUuid);
        } catch (RepositoryException e) {
            log.warn("RepositoryException: Cannot lookup content node for uuid '"+docbaseUuid+"'. ", e);
        }
        return null;
    }

    private String findConfigurationNameForSite() {
        String configuredPath = getValueProvider().getString(HstNodeTypes.SITE_CONFIGURATIONPATH);
        if (configuredPath != null) {
            String configurationName = StringUtils.substringAfterLast(configuredPath, "/");
            if (configurationName.isEmpty()) {
                log.warn("Invalid configuration path '{}' is used for '{}'.", configuredPath, getValueProvider().getPath());
                return null;
            }
            return configurationName;
        }
        String configurationName = getValueProvider().getName();
        return StringUtils.substringBefore(configurationName, "-preview");
    }


    private String findConfigurationNameAndVersionForSite(final String configurationName, final long version) {
        String configurationNameAndVersion = configurationName;
        if (version > -1) {
            configurationNameAndVersion += ("-v"+version);
        }
        return configurationNameAndVersion;
    }
    
    @Override
    public String getCanonicalContentPath() {
        return contentPath;
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
