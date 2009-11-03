/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.hippoecm.hst.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstSitesService extends AbstractJCRService implements HstSites, Service{

    private static final long serialVersionUID = 1L;
    
    private Logger log = LoggerFactory.getLogger(HstSitesService.class) ;
    private Map<String, HstSite> hstSites = new HashMap<String, HstSite>();
    
    private String sitesContentPath;
    
    public HstSitesService(Node node) throws ServiceException {
        super(node);
        try {
            if(node.isNodeType(HstNodeTypes.NODETYPE_HST_SITES)) {
                this.sitesContentPath = node.getPath();
                init(node);
                /*
                 * After initialization, all needed jcr properties and nodes have to be loaded. The underlying jcr nodes in 
                 * the value providers now will all be closed.
                 */
                this.closeValueProvider(true);
            } 
            else {
                throw new ServiceException("Cannot instantiate a HstSites object for a node of type '"+node.getPrimaryNodeType().getName()+"' for node '"+node.getPath()+"'");
            }
        } catch (RepositoryException e) {
            throw new ServiceException("Repository Exception while creating HstSites object: " + e);
        }
    }
    
    public Service[] getChildServices() {
        return hstSites.values().toArray(new HstSiteService[hstSites.size()]);
    }
    
    private void init(Node node) throws RepositoryException {
       QueryManager qryMng = node.getSession().getWorkspace().getQueryManager();
       String query = "/jcr:root" + node.getPath()+"//element(*,"+HstNodeTypes.NODETYPE_HST_SITE+")";
       QueryResult result = qryMng.createQuery(query, "xpath").execute();
       for(NodeIterator nodeIt = result.getNodes(); nodeIt.hasNext();) {
           Node site = nodeIt.nextNode();
           try {
               HstSite hstSite = new HstSiteService(site, this);
               hstSites.put(hstSite.getName(), hstSite);
           } catch (ServiceException e){
               
               log.error("Skipping subsite: {}", site.getName() ,e );
           }
       }
    }

    public HstSite getSite(String name) {
        HstSite site = getSites().get(name);
        if(site == null) {
            if (log.isWarnEnabled()) {
                log.warn("Cannot load site with name '{}' because not present at '{}'", name, this.sitesContentPath);
            }
        }
        return site;
    }

    public Map<String, HstSite> getSites() {
        return Collections.unmodifiableMap(hstSites);
    }

    public String getSitesContentPath() {
        return this.sitesContentPath;
    }
 
    
}
