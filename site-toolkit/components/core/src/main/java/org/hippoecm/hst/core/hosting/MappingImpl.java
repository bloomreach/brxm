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
package org.hippoecm.hst.core.hosting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingImpl implements Mapping{
    
    private final static String WILDCARD = "*";
    private final static String PROPERTYHOLDER = "${1}";
    
    private VirtualHost virtualHost;
    // depth is an indicator for the number of slashes in the prefix
    private int depth = 0;
    private String uriPrefix;
    private String rewrittenPrefix;
    
    public MappingImpl(String mapping, VirtualHost virtualHost) throws MappingException{
        this.virtualHost = virtualHost;
        
        if(mapping == null) {
            throw new MappingException("Mapping not allowed to be null");
        } 
        if(mapping.indexOf("-->") < 0) {
            throw new MappingException("Mapping cannot be valid if it does not contains '-->'");
        }
        
        String[] fromTo = mapping.split("-->");
        if(fromTo.length != 2) {
            throw new MappingException("Mapping can have only a single '-->'");
        }

        fromTo[0] = fromTo[0].trim();
        fromTo[1] = fromTo[1].trim();
        
        if("".endsWith(fromTo[0]) || "".equals(fromTo[1])) {
            throw new MappingException("Mapping must have non empty value before and after '-->'");
        }
        
        if(fromTo[0].indexOf("/"+WILDCARD) < fromTo[0].length() - ("/"+WILDCARD).length() ) {
            throw new MappingException("Mapping before '-->' must end with /* and cannot contain /* somewhere else ");
        }
        
        if(fromTo[1].indexOf("/"+PROPERTYHOLDER) < fromTo[1].length() - ("/"+PROPERTYHOLDER).length()) {
            throw new MappingException("Mapping before '-->' must end with /${1} and cannot contain /${1} somewhere else ");
        }

        uriPrefix = fromTo[0].substring(0, (fromTo[0].length() - WILDCARD.length()));
        rewrittenPrefix = fromTo[1].substring(0, (fromTo[1].length() - PROPERTYHOLDER.length()));
        depth = uriPrefix.split("/").length;
        
    }

    public boolean match(String pathInfo) {
        return pathInfo.startsWith(uriPrefix);
    }


    public String getRewrittenPrefix() {
        return this.rewrittenPrefix;
    }

    public String getUriPrefix() {
        return this.uriPrefix;
    }
    
    public int compareTo(Mapping mapping) {
        if(mapping instanceof MappingImpl) {
            MappingImpl repositoryMappingImpl = (MappingImpl) mapping;
            if(this.depth > repositoryMappingImpl.depth) {
                return -1;
            } else if(this.depth < repositoryMappingImpl.depth) {
                return 1;
            }
        }
        return 0;
    }

    public VirtualHost getVirtualHost() {
        return this.virtualHost;
    }


}
