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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLMappingImpl implements URLMapping {

    private static final Logger log = LoggerFactory.getLogger(URLMapping.class);

    private List<LinkRewriter> linkRewriters = new ArrayList<LinkRewriter>();
    private Session session;
    private String contextPrefix;
    private String contextPath;
    private Node siteMapRootNode;
    
    public URLMappingImpl(Session session, String contextPath,  String contextPrefix, String path) {
        this.session = session;
        this.contextPrefix = contextPrefix;
        this.contextPath = contextPath;
        try {

            Node hstConf = (Node) session.getItem(path);
            siteMapRootNode = hstConf.getNode(HstFilterBase.SITEMAP_RELATIVE_LOCATION);
            NodeIterator subNodes;

            subNodes = siteMapRootNode.getNodes();
            while (subNodes.hasNext()) {
                Node subNode = (Node) subNodes.next();
                if (subNode == null) {
                    continue;
                }
                
                if (subNode.hasProperty("hst:urlmapping")) {
                    if (subNode.hasProperty("hst:prefixlinkrewrite")) {
                        String prefixLinkRewrite = subNode.getProperty("hst:prefixlinkrewrite").getString();
                        if ("".equals(prefixLinkRewrite)) {
                            log.warn("Skipping empty hst:sitemapitem for linkrewriting" +
                            		" because of empty hst:prefixlinkrewrite");
                            continue;
                        }

                        if (subNode.hasProperty("hst:nodetype") && subNode.hasProperty("hst:nodepath")) {
                            if (subNode.getProperty("hst:nodetype").getValues().length == subNode.getProperty(
                                    "hst:nodepath").getValues().length) {
                                Value[] nodetypes = subNode.getProperty("hst:nodetype").getValues();
                                Value[] nodepaths = subNode.getProperty("hst:nodepath").getValues();
                                for (int i = 0; i < nodepaths.length; i++) {
                                    LinkRewriter linkRewriter = new LinkRewriter(prefixLinkRewrite, nodetypes[i]
                                            .getString(), nodepaths[i].getString());
                                    linkRewriters.add(linkRewriter);
                                }
                            } else {
                                log.warn("Skipping hst:sitemapitem for linkrewriting because length" +
                                		" of multivalued property 'hst:nodetype' is not equal to the length of 'hst:nodepath'. This is mandatory for a proper working linkrewriting item");
                            }
                        } else {
                            log.warn("Skipping hst:sitemapitem for linkrewriting because " +
                            		"'hst:nodetype' property or 'hst:nodepath' property is missing");
                        }
                    }

                } else {
                    log.warn("hst:sitemapitem sitemap item missing 'hst:ulrmapping' property. " +
                    		"Disregard item in the url mappings");
                }
            }
        } catch (PathNotFoundException e) {
            log.error("PathNotFoundException " + e.getMessage());
        }
        catch (RepositoryException e) {
            log.error("RepositoryException " + e.getMessage());
        }

    }

    public String rewriteLocation(Node node) {
        String path = "";
        try {
            if(node instanceof HippoNode) {
                HippoNode hippoNode = (HippoNode)node;
                if(hippoNode.getCanonicalNode()!= null && !hippoNode.getCanonicalNode().isSame(node)) {
                    // take canonical node because virtual node found
                    node = hippoNode.getCanonicalNode();
                }
            }
            path = node.getPath();
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                try {
                    node = node.getNode(node.getName());
                    path = node.getPath();
                } catch (PathNotFoundException e) {
                    log.warn("hippo:handle does not contain a child node of the same name as the handle." +
                    		" Use the handle itself for rewriting the link.");
                }
            }

            LinkRewriter bestRewriter = null;
            int highestScore = 0;
            for (LinkRewriter lrw : linkRewriters) {
                int score = lrw.score(node);
                if (score > highestScore) {
                    if(log.isDebugEnabled()) {
                        if(highestScore == 0) {
                            log.debug("found a match for linkrewriting");
                        } else if (highestScore > 0) {
                            log.debug("found a better match for linkrewriting");
                        }
                    }
                    highestScore = score;
                    bestRewriter = lrw;
                } else if(score > 0 && log.isDebugEnabled()) {
                    log.debug("found a match but already had a better match");
                }
            }

            if (bestRewriter == null) {
                log.warn("No matching linkrewriter found for path '" + path + "'. Return node path.");
            } else {
                String url = bestRewriter.getUrl(node);
                return contextPath+contextPrefix+url;
            }
            
        } catch (RepositoryException e) {
            log.error("RepositoryException during link rewriting " + e.getMessage() + " Return node path.");
        }
        return contextPath+contextPrefix+path;
    }

    public String rewriteLocation(String path) {
        if(siteMapRootNode != null && path != null && !"".equals(path)) {
            if(path.startsWith("/")) {
                path = path.substring(1);
                if(path.length()==0) {
                    log.warn("Unable to rewrite link for path = '/' .  Prefixing path with context, but no rewrite");
                    return contextPath+contextPrefix;
                }
            }
            try {
                if(siteMapRootNode.hasNode(path)) {
                    Node sitemapNode = siteMapRootNode.getNode(path);
                    if(sitemapNode.hasProperty("hst:prefixlinkrewrite")) {
                        String newLink = sitemapNode.getProperty("hst:prefixlinkrewrite").getString();
                        log.debug("rewriting '"+path+"' --> '" + newLink);
                        if(!"".equals(newLink) && !newLink.startsWith("/")) {
                            newLink = "/"+newLink;
                        }
                        return contextPath+contextPrefix+newLink;
                    } else {
                        log.warn("cannot rewrite path '" + path + "' because the sitemap node does not have the property 'hst:prefixlinkrewrite'. Node : " + sitemapNode.getPath());
                    }
                } else {
                    log.warn("'" +path+"' does not exist in sitemap node '" + siteMapRootNode.getPath() +"'. Prefixing path with context, but no rewrite ");
                }
                
            } catch (RepositoryException e) {
                log.warn("Unable to rewrite link for path = '" + path +"'.  Prefixing path with context, but no rewrite. RepositoryException " + e.getMessage());
            }
        } 
        
        return contextPath+contextPrefix+"/"+path;
        
    }

}
