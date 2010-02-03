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
package org.hippoecm.hst.site.request;

import org.hippoecm.hst.core.hosting.Mapping;
import org.hippoecm.hst.core.request.MatchedMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MatchedMappingImpl
 * 
 * @version $Id$
 */
public class MatchedMappingImpl implements MatchedMapping{


    private final static Logger log = LoggerFactory.getLogger(MatchedMappingImpl.class);
    private Mapping mapping; 
    private String siteName;
    private boolean uriMapped; 
    
    public MatchedMappingImpl(Mapping mapping){
        this.mapping = mapping;
        this.siteName = mapping.getVirtualHost().getSiteName();
    }
    
    public Mapping getMapping(){
        return this.mapping;
    }
    
    public String getSiteName() {
        return this.siteName;
    }
    
    public String mapToInternalURI(String pathInfo) {
        if(pathInfo.startsWith(mapping.getUriPrefix())) {
            String mapped = mapping.getRewrittenPrefix() + pathInfo.substring(mapping.getUriPrefix().length());;
            log.debug("Mapped to internal URI:  '{}' --> '{}'", pathInfo, mapped);
            if(!mapped.equals(pathInfo)){
                this.uriMapped = true;
            }
            return mapped; 
        } else {
            log.warn("Cannot map '{}' to internal uri because it does not start with '{}'. Return original pathInfo", pathInfo, mapping.getUriPrefix());
            return pathInfo;
        }
    }
    
    public boolean isURIMapped() {
        return this.uriMapped;
    }
    
}
