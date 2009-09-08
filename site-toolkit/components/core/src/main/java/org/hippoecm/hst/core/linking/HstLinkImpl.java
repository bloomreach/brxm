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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstLinkImpl implements HstLink{

    private final static Logger log = LoggerFactory.getLogger(HstLinkImpl.class);
    
    private String path;
    private HstSite hstSite;
    private boolean containerResource;
    
    public HstLinkImpl(String path, HstSite hstSite){
         this(path, hstSite,false);
    }
    
    public HstLinkImpl(String path, HstSite hstSite, boolean containerResource) {
        this.path = path;
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
        this.path = path;
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
        StringBuilder url = new StringBuilder();
        
        String characterEncoding = response.getCharacterEncoding();
        
        if (characterEncoding == null) {
            characterEncoding = "UTF-8";
        }
        
        String[] pathElements = this.getPathElements();
        
        if(pathElements == null) {
            log.warn("Unable to rewrite link. Return EVAL_PAGE");
            return null;
        }
        
        for(String elem : pathElements) {
            String enc = response.encodeURL(elem);
            url.append("/").append(enc);
        }
        
        String urlString = null;
        
        if (this.containerResource) {
            HstURL hstUrl = response.createResourceURL(ContainerConstants.CONTAINER_REFERENCE_NAMESPACE);
            hstUrl.setResourceID(url.toString());
            urlString = hstUrl.toString();
        } else {
            urlString = response.createNavigationalURL(url.toString()).toString();
        }
        
        if(external) {
            MatchedMapping mapping = request.getRequestContext().getMatchedMapping();
            
            if( mapping != null && mapping.getMapping() != null) {
                StringBuilder builder = new StringBuilder();
               
                VirtualHost vhost = mapping.getMapping().getVirtualHost();
                
                String protocol = vhost.getProtocol();
                
                if (protocol == null) {
                    protocol = "http";
                }
                
                String serverName = request.getServerName();
                
                int port = vhost.getPortNumber();
                
                if (port == 0) {
                    port = request.getServerPort();
                }
                
                if ((port == 80 && "http".equals(protocol)) || (port == 443 && "https".equals(protocol))) {
                    port = 0;
                }
                
                builder.append(protocol);
                builder.append("://").append(serverName);
                
                if (vhost.isPortVisible() && port != 0) {
                    builder.append(":").append(port);
                }
                
                urlString = builder.toString() + urlString;
            } else {
                log.warn("Cannot create external link because there is no virtual host to use");
            }
        }
        
        return urlString;
    }

    

}
