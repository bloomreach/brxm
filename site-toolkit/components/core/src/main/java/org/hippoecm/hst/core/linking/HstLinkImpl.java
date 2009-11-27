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
package org.hippoecm.hst.core.linking;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.hosting.VirtualHost;
import org.hippoecm.hst.core.request.MatchedMapping;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstLinkImpl implements HstLink{

    private final static Logger log = LoggerFactory.getLogger(HstLinkImpl.class);
    
    private String path;
    private HstSite hstSite;
    private boolean containerResource;
    private boolean notFound = false;
    
    public HstLinkImpl(String path, HstSite hstSite){
         this(path, hstSite,false);
    }
    
    public HstLinkImpl(String path, HstSite hstSite, boolean containerResource) {
        this.path = PathUtils.normalizePath(path);
        this.hstSite = hstSite;
        this.containerResource = containerResource;
    }
    
    public HstSite getHstSite() {
        return this.hstSite;
    }

    public String getPath() {
        return this.path;
    }
    
    public void setPath(String path) {
        this.path = PathUtils.normalizePath(path);
    }
    
    public boolean getContainerResource() {
        return this.containerResource;
    }

    public void setContainerResource(boolean containerResource) {
       this.containerResource = containerResource;
    }
    
    public String[] getPathElements() {
        if(this.path == null) {
            return null;
        }
        return this.path.split("/");
    }

    public String toUrlForm(HstRequest request, HstResponse response, boolean external) {
        String characterEncoding = response.getCharacterEncoding();
        
        if (characterEncoding == null) {
            characterEncoding = "UTF-8";
        }
        
        String[] pathElements = this.getPathElements();
        
        if(pathElements == null) {
            log.warn("Unable to rewrite link. Return EVAL_PAGE");
            return null;
        }
        
        String urlString = null;
        
        if (this.containerResource) {
            HstURL hstUrl = response.createResourceURL(ContainerConstants.CONTAINER_REFERENCE_NAMESPACE);
            hstUrl.setResourceID(path);
            urlString = hstUrl.toString();
        } else {
            urlString = response.createNavigationalURL(path).toString();
        }
        
        if(external) {
            MatchedMapping mapping = request.getRequestContext().getMatchedMapping();
            
            if( mapping != null && mapping.getMapping() != null) {
                
                VirtualHost vhost = mapping.getMapping().getVirtualHost();
                urlString = vhost.getBaseURL(request) + urlString;
            } else {
                log.warn("Cannot create external link because there is no virtual host to use");
            }
        }
        
        return urlString;
    }

    public boolean isNotFound() {
        return notFound;
    }

    public void setNotFound(boolean notFound) {
        this.notFound = notFound;
    }

    

}
