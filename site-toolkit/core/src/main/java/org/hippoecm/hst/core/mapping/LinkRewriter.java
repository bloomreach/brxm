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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkRewriter {

    private static final Logger log = LoggerFactory.getLogger(URLMapping.class);

    private String prefixLinkRewrite;
    private String nodeTypeName;
    private String path;
    private int depth;
    private boolean isExactMatch;
    private String virtualEntryName;
    private String physicalEntryPath;
    private String contextPrefix;
   

    public LinkRewriter(String prefixLinkRewrite, String nodeTypeName, String path2, String virtualEntryName, String physicalEntryPath, String contextPrefix) {
        this.prefixLinkRewrite = prefixLinkRewrite;
        this.nodeTypeName = nodeTypeName;
        this.virtualEntryName = virtualEntryName;
        this.physicalEntryPath = physicalEntryPath;
        this.contextPrefix = contextPrefix;

        if (!path2.startsWith("/")) {
            log.warn("hst:nodepath should be absolute so should start with a '/'. Prepending '/' now");
            path2 = "/" + path2;
        }
        if (path2.endsWith("/*")) {
            this.path = path2.substring(0, path2.length() - 1);
            depth = path.split("/").length;
        } else {
            this.isExactMatch = true;
            this.path = path2;
        }
    }

    public String getLocation(Node node) throws RepositoryException{
        if(isExactMatch) {
            return contextPrefix + prefixLinkRewrite;
        } 
        String path = node.getPath();
        if(virtualEntryName != null && physicalEntryPath != null) {
            if(path.startsWith(physicalEntryPath)) {
                
                String newLocation = contextPrefix+ prefixLinkRewrite + "/" + virtualEntryName + path.substring(physicalEntryPath.length());
                log.debug("Translated phyiscal entry path '" +node.getPath() + "' into virtual entry path '" + newLocation + "'");
                return newLocation;
            } else {
                log.debug("node path ('" +path+ "')  does not start with the physicalEntryPath ('"+physicalEntryPath+"') so no rewriting.");
            }
        }
        return contextPrefix + prefixLinkRewrite + path;
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
}
