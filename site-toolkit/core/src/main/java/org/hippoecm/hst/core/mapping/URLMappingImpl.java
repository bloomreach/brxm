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
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLMappingImpl implements URLMapping {

    private static final Logger log = LoggerFactory.getLogger(URLMapping.class);

    private List<LinkRewriter> linkRewriters = new ArrayList<LinkRewriter>();
    private Session session;
    private String contextPrefix;

    public URLMappingImpl(Session session, String contextPrefix, String path) {
        this.session = session;
        this.contextPrefix = contextPrefix;
        try {

            Node hstConf = (Node) session.getItem(path);
            Node siteMapRootNode = hstConf.getNode(HstFilterBase.SITEMAP_RELATIVE_LOCATION);
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
        } catch (RepositoryException e) {
            log.error("RepositoryException " + e.getMessage());
        }

    }

    public String rewriteLocation(Node node) {
        String path = "";
        try {
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
                log.warn("No matching linkrewriter found.");
            } else {
                String url = bestRewriter.getUrl(node);
                return contextPrefix+url;
            }
            
        } catch (RepositoryException e) {
            log.error("RepositoryException " + e.getMessage());
        }
        return contextPrefix+path;
    }

    public String rewriteLocation(String documentPath) {
        try {
            Item item = session.getItem(documentPath);
            if (item.isNode()) {
                Node node = (Node) item;
                return rewriteLocation(node);
            } else {
                log.warn("Not possible to rewrite a link to a property");
            }
        } catch (PathNotFoundException e) {
            log.warn("item " + documentPath + " does not exist (anymore) in the repository. Return  a score of 0");
        } catch (RepositoryException e) {
            log.error("RepositoryException " + e.getMessage());
        }

        return documentPath;
    }

}
