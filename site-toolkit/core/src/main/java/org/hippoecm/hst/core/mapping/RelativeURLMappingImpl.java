package org.hippoecm.hst.core.mapping;

import java.util.List;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.node.PageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelativeURLMappingImpl implements URLMapping{
    
    private static final Logger log = LoggerFactory.getLogger(URLMapping.class);
    
    private String currentRequestUri;
    private URLMapping delegatee;
    
    public RelativeURLMappingImpl(String currentRequestUri, URLMapping mapping){
        this.delegatee = mapping;
        if(currentRequestUri.startsWith("/")) {
            currentRequestUri.substring(1);
        }
        if(currentRequestUri.endsWith("/")) {
            currentRequestUri.substring(0, currentRequestUri.length()-1);
        }
        this.currentRequestUri = currentRequestUri;
    }
    
    public List<String> getCanonicalPathsConfiguration() {
        return delegatee.getCanonicalPathsConfiguration();
    }

    public PageNode getMatchingPageNode(HttpServletRequest request, ContextBase contextBase) {
        return delegatee.getMatchingPageNode(request, contextBase);
    }

    public String rewriteLocation(Node node) {
        String absoluteLocation = delegatee.rewriteLocation(node);
        return computeRelativeUrl(absoluteLocation);
    }


    public String rewriteLocation(String path) {
        String absoluteLocation = delegatee.rewriteLocation(path);
        return computeRelativeUrl(absoluteLocation);
    }
    
    public String getRelativeLocation(String path) {
        return computeRelativeUrl(path);
    }
  
    private String computeRelativeUrl(String absoluteLocation) {
        if(absoluteLocation.startsWith("/")) {
            absoluteLocation.substring(1);
        }
        if(absoluteLocation.endsWith("/")) {
            absoluteLocation.substring(0,absoluteLocation.length()-1);
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
        while( (++depth) < currentRequestUriParts.length) {
            relativeUrl.append("../");
        }
        while( matchDepth < absoluteLocationParts.length) {
            if(!absoluteLocationParts[matchDepth].equals("")) {
                relativeUrl.append(absoluteLocationParts[matchDepth]);
                matchDepth++;
                if(matchDepth < absoluteLocationParts.length) {
                    relativeUrl.append("/");
                }
            }
        }
        return relativeUrl.toString();
    }

}
