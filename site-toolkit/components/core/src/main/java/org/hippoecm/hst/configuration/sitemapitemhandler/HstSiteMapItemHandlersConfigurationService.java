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
package org.hippoecm.hst.configuration.sitemapitemhandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlerConfiguration;
import org.hippoecm.hst.configuration.sitemapitemhandlers.HstSiteMapItemHandlersConfiguration;
import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.LoggerFactory;

public class HstSiteMapItemHandlersConfigurationService extends AbstractJCRService implements HstSiteMapItemHandlersConfiguration, Service {

    private static final long serialVersionUID = 1L;
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstSiteMapItemHandlersConfigurationService.class);

    private Map<String, HstSiteMapItemHandlerConfiguration> siteMapItemHanderConfigurations = new HashMap<String, HstSiteMapItemHandlerConfiguration>();
    
    public HstSiteMapItemHandlersConfigurationService(Node siteMapItemHandlersNode) throws ServiceException {
        super(siteMapItemHandlersNode);
        
        try {
            for(NodeIterator nodeIt = siteMapItemHandlersNode.getNodes(); nodeIt.hasNext();) {
                Node child = nodeIt.nextNode();
                if(child == null) {
                    log.warn("skipping null node");
                    continue;
                }
                if(child.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMAPITEMHANDLER)) {
                    try {
                        HstSiteMapItemHandlerConfiguration siteMapItemHandler = new HstSiteMapItemHandlerConfigurationService(child);
                        siteMapItemHanderConfigurations.put(siteMapItemHandler.getId(), siteMapItemHandler);
                    } catch (ServiceException e) {
                        log.warn("Skipping handle '{}' because '{}'", child.getPath(), e.getMessage());
                    }
                    
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("Skipping node '{}' because is not of type {}", child.getPath(), HstNodeTypes.NODETYPE_HST_SITEMAPITEMHANDLER);
                    }
                }
            }
        } catch (RepositoryException e) {
            throw new ServiceException("RepositoryException during initializing handles", e);
        }
        
    }
    
    public HstSiteMapItemHandlerConfiguration getSiteMapItemHandlerConfiguration(String id) {
        return siteMapItemHanderConfigurations.get(id);
    }

    public Map<String, HstSiteMapItemHandlerConfiguration> getSiteMapItemHandlerConfigurations() {
        return Collections.unmodifiableMap(siteMapItemHanderConfigurations);
    }

    public Service[] getChildServices() {
        return siteMapItemHanderConfigurations.values().toArray(new Service[siteMapItemHanderConfigurations.values().size()]);
    }

}
