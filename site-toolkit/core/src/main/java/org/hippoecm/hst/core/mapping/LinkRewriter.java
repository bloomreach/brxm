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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.filters.domain.RepositoryMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkRewriter {

    private static final Logger log = LoggerFactory.getLogger(URLMapping.class);

    private String linkRewrite;
    private String nodeTypeName;
    private String path;
    private int depth;
    private boolean isExactMatch;
    private boolean isPrefix;
    private RepositoryMapping repositoryMapping;
    private String siteMapItemName;
   

    public LinkRewriter(String siteMapItemName, String linkRewrite, boolean isPrefix, String nodeTypeName, String path, RepositoryMapping repositoryMapping) {
        this.siteMapItemName = siteMapItemName;
        this.linkRewrite = linkRewrite;
        this.nodeTypeName = nodeTypeName;
        this.repositoryMapping = repositoryMapping;
        this.isPrefix = isPrefix;
        
        if (path.endsWith("*")) {
            this.path = path.substring(0, path.length() - 1);
        } else {
            this.isExactMatch = true;
            this.path = path;
        }
        
        if (path.startsWith("/")) {
            log.debug("hst:nodepath starts with a '/' the configured hst:nodepath will be treated compared to the jcr root");
        } else {
            log.debug("hst:nodepath is relative because it does not start with a '/'. path will be taken relative to the canonical path of the contentContextBase");
            if("".equals(this.path)) {
                this.path = repositoryMapping.getCanonicalContentPath();
            } else {
                this.path = repositoryMapping.getCanonicalContentPath()+"/" + this.path;
            }
        }
        depth = this.path.split("/").length;
    }

    public String getLocation(Node node) throws RepositoryException{
        if(isExactMatch || !isPrefix) {
            StringBuffer newLocation = new StringBuffer(repositoryMapping.getPrefix());
            if(this.repositoryMapping.getDomainMapping().isServletContextPathInUrl()) {
                newLocation.insert(0, repositoryMapping.getDomainMapping().getServletContextPath());
            }
            newLocation.append(linkRewrite);
            return newLocation.toString();
        } 
        String path = node.getPath();
        String pathPrefix = repositoryMapping.getCanonicalContentPath();
        if(pathPrefix != null) {
            if(path.startsWith(pathPrefix)) {
                
                StringBuffer newLocation = new StringBuffer(repositoryMapping.getPrefix());
                if(this.repositoryMapping.getDomainMapping().isServletContextPathInUrl()) {
                    newLocation.insert(0, repositoryMapping.getDomainMapping().getServletContextPath());
                }
                newLocation.append(linkRewrite).append(path.substring(pathPrefix.length()));
                
                log.debug("Translated canonical path '{}' into virtual relative path '{}'", node.getPath(), newLocation);
                return newLocation.toString();
            } else {
                log.debug("node path ('{}')  does not start with the canonical path of repository mapping ('{}') so no rewriting to content context path. Most like we are dealing with a binary.", path, pathPrefix);
            }
        }
        String prefix = "";
        if(this.repositoryMapping.getDomainMapping().isServletContextPathInUrl()) {
            prefix =  repositoryMapping.getDomainMapping().getServletContextPath();
        }
        
        return prefix + linkRewrite + path;
    }
    
    /**
     * This method implements a linkrewriting scoring algorithm. The LinkRewriter object
     * with the highest score should naturally be used to rewrite the document path to a url with.
     * 
     * TODO : explain Algorithm
     * 
     * @param Node
     * @return score 
     */
 
    public int score(Node node) {
        try {
            String nodePath = node.getPath();
            if (isExactMatch) {
                if (nodePath.equals(path)) {
                    log.debug("node path matches exact");
                    if (node.isNodeType(nodeTypeName)) {
                        if (node.getPrimaryNodeType().getName().equals(nodeTypeName)) {
                            // maximum possible match
                            log.debug("Maximum possible score for linkrewriting. Score = " + Score.MAXIMUNSCORE);
                            return Score.MAXIMUNSCORE;
                        } else {
                            log.debug("Exact path match and nodetype is a supertype of this node. Score = "
                                    + (Score.EXACTPATH * Score.SUPERTYPE));
                            return Score.EXACTPATH * Score.SUPERTYPE;
                        }
                    } else {
                        log.debug("path matched but incorrect type returning score 0");
                        return 0;
                    }
                }
            } else if (nodePath.startsWith(path)){
                log.debug("Node path starts with correct path : " + nodePath + " <--> " + path );
                if (node.isNodeType(nodeTypeName)) {
                    if (node.getPrimaryNodeType().getName().equals(nodeTypeName)) {
                        // maximum possible match
                        log.debug("Node starts with correct path and primary type matches. Depth of match = " + depth + ". Score = " + (Score.ISTYPE * Score.PARTIALPATH) + depth);
                        return (Score.ISTYPE * Score.PARTIALPATH) + depth;
                    } else {
                        log.debug("Node starts with correct path and nodetype is a supertype of this node. Depth of match = " + depth + ". Score = "
                                + Score.SUPERTYPE * Score.PARTIALPATH + depth);
                        return Score.SUPERTYPE * Score.PARTIALPATH + depth;
                    }
                } else {
                    log.debug("path matched but incorrect type returning score 0");
                    return 0;
                }
            }
            
        } catch (RepositoryException e) {
            log.error("RepositoryException " + e.getMessage());
            return 0;
        }
        return 0;
    }

    public String getSiteMapItemName() {
        return siteMapItemName;
    }
}
