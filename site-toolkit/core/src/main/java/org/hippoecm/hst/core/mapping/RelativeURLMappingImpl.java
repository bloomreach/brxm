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
package org.hippoecm.hst.core.mapping;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.hst.core.filters.domain.RepositoryMapping;
import org.hippoecm.hst.core.template.node.PageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelativeURLMappingImpl implements URLMapping{
    
    private static final Logger log = LoggerFactory.getLogger(URLMapping.class);
    
    private String requestUriToWrap;
    private URLMapping delegatee;
    
    
    public RelativeURLMappingImpl(String requestUriToWrap, URLMapping mapping){
        this.delegatee = mapping;
        this.requestUriToWrap = requestUriToWrap;
    }
    
    public List<String> getCanonicalPathsConfiguration() {
        return delegatee.getCanonicalPathsConfiguration();
    }

    public PageNode getMatchingPageNode(String requestURI, HstRequestContext hstRequestContext) {
        return delegatee.getMatchingPageNode(requestURI, hstRequestContext);
    }

    public String rewriteLocation(Node node) {
        String absoluteLocation = delegatee.rewriteLocation(node);
        return computeRelativeUrl(absoluteLocation, requestUriToWrap);
    }


    public String rewriteLocation(String sitemapNodeName, Session jcrSession) {
        String absoluteLocation = delegatee.rewriteLocation(sitemapNodeName, jcrSession);
        return computeRelativeUrl(absoluteLocation, requestUriToWrap);
    }
    
    public String getLocation(String path) {
        path = delegatee.getLocation(path);
        return computeRelativeUrl(path, requestUriToWrap);
    }
  
    public static String computeRelativeUrl(String absoluteLocation, String currentRequestUri) {
        boolean currentUriEndsWithSlash = false;
        while(currentRequestUri.startsWith("/")) {
            currentRequestUri = currentRequestUri.substring(1);
        }
        while(currentRequestUri.endsWith("/")) {
            currentUriEndsWithSlash = true;
            currentRequestUri =  currentRequestUri.substring(0,currentRequestUri.length()-1);
        }
        while(absoluteLocation.startsWith("/")) {
            absoluteLocation = absoluteLocation.substring(1);
        }
        while(absoluteLocation.endsWith("/")) {
            absoluteLocation =  absoluteLocation.substring(0,absoluteLocation.length()-1);
        }
        
        // special case:
        if(currentRequestUri.startsWith(absoluteLocation)) {
            String leftover = currentRequestUri.substring(absoluteLocation.length());
           
            if(leftover.length()==0) {
                return "";
            } else {
                if(leftover.startsWith("/")) {
                    leftover = leftover.substring(1);
                }
                int count = leftover.split("/").length;
                String relativeUrl = "";
                if(currentUriEndsWithSlash) {
                    relativeUrl = relativeUrl+"../";
                }
                for(int i = 0; i < count; i++) {
                    if(i==(count-1)) {
                        relativeUrl = relativeUrl+ "./";
                    } else {
                        relativeUrl = relativeUrl+"../";
                    }
                }
                return relativeUrl;
            }
        }
        String[] currentRequestUriParts = currentRequestUri.split("/");
        String[] absoluteLocationParts = absoluteLocation.split("/");
        StringBuffer relativeUrl = new StringBuffer("");
        boolean diverged = false;
        int matchDepth = 0;
        for(int i = 0 ;  i < currentRequestUriParts.length && !diverged; i++) {
            if(absoluteLocationParts.length > i) {
                if(absoluteLocationParts[i].equals(currentRequestUriParts[i])) {
                    matchDepth++;
                } else {
                    diverged = true;
                }
            }
        }
        int depth = matchDepth; 
        if(currentUriEndsWithSlash) {
            relativeUrl.append("../"); 
        }
        while( (++depth) < currentRequestUriParts.length) {
            relativeUrl.append("../");
        }
        
        boolean startsWith = false;
        if(matchDepth == currentRequestUriParts.length) {        	
        		 relativeUrl.append(currentRequestUriParts[matchDepth-1]);
        		 startsWith = true;
        }
        
        while( matchDepth < absoluteLocationParts.length) {
            if(!absoluteLocationParts[matchDepth].equals("")) {
            	if(startsWith) {
            	  relativeUrl.append("/");
            	  startsWith = false;
            	}            	
                relativeUrl.append(absoluteLocationParts[matchDepth]);
                matchDepth++;
                if(matchDepth < absoluteLocationParts.length) {
                    relativeUrl.append("/");
                }
            }
        }
        return relativeUrl.toString();
    }

    public RepositoryMapping getRepositoryMapping() {
        return delegatee.getRepositoryMapping();
    }


}
