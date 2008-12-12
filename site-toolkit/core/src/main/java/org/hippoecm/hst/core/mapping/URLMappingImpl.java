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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.collections.map.LRUMap;
import org.hippoecm.hst.core.filters.base.HstBaseFilter;
import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.hst.core.filters.domain.RepositoryMapping;
import org.hippoecm.hst.core.template.node.PageNode;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLMappingImpl implements URLMapping {

    private static final Logger log = LoggerFactory.getLogger(URLMapping.class);

    private final RewriteLRUCache rewriteLRUCache;
    private final List<LinkRewriter> linkRewriters = new ArrayList<LinkRewriter>();
    private final Map<String, String> siteMapNodes = new LinkedHashMap<String, String>();
    private final RepositoryMapping repositoryMapping;
    
    // a list containing all canonical paths which are used in the url mapping. These paths are used to create named events
    // on which the cache is invalidated
    private final List<String> canonicalPathConfiguration;
    
    private String siteMapRootNodePath;

    public URLMappingImpl(HstRequestContext hstRequestContext) {
        this.repositoryMapping = hstRequestContext.getRepositoryMapping();
        this.rewriteLRUCache = new RewriteLRUCache(500);
        this.canonicalPathConfiguration = new ArrayList<String>();
        
        try {
            long start = System.currentTimeMillis();
            String virtualEntryName = null;
            String physicalEntryPath = null;
            Session session = hstRequestContext.getJcrSession();
            HippoNode hstConf = (HippoNode) session.getItem(repositoryMapping.getHstConfigPath());
            Node canonical = hstConf.getCanonicalNode();
            if(canonical != null ) {
                this.canonicalPathConfiguration.add(canonical.getPath());
            }
            
            // TODO when the configuration is a combination of multiple facetselects, we need to add all canonical path
            // configurations. currently, only the base path is added
            
            Node siteMapRootNode = hstConf.getNode(HstBaseFilter.SITEMAP_RELATIVE_LOCATION);
            siteMapRootNodePath = siteMapRootNode.getPath();
            try {
                if (siteMapRootNode.hasProperty("hst:entrypointid")
                        && !"".equals(siteMapRootNode.getProperty("hst:entrypointid").getString())) {
                    Node entryPointNode = session.getNodeByUUID(siteMapRootNode.getProperty("hst:entrypointid")
                            .getString());
                    virtualEntryName = entryPointNode.getName();
                    log.debug("virtual entry name = '" + virtualEntryName + "'");
                    try {
                        if (entryPointNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                            Node physicalPointNode = session.getNodeByUUID(entryPointNode.getProperty(
                                    HippoNodeType.HIPPO_DOCBASE).getString());
                            physicalEntryPath = physicalPointNode.getPath();
                            log.debug("physical entry path = '" + physicalEntryPath + "'");
                        }
                    } catch (ItemNotFoundException e) {
                        log
                                .warn("physical entry cannot be found because the entry point node does not have a valid docbase");
                    }
                } else {
                    log
                            .debug("'hst:sitemap' does not contain property 'hst:entrypointid', hence discarding entry node path replacement in linkrewriting");
                }
            } catch (ItemNotFoundException e) {
                log
                        .warn("hst:entrypointid in the hst:sitemap node does not have the uuid of an existing node. Discarding entry node path replacements in linkrewriting. "
                                + e.getMessage());
            } catch (PathNotFoundException e) {
                log
                        .warn("hst:entrypointid in the hst:sitemap does not exist. Discarding entry node path replacements in linkrewriting. "
                                + e.getMessage());
            } catch (RepositoryException e) {
                log.warn("RepositoryException: Discarding entry node path replacements in linkrewriting. "
                        + e.getMessage());
            }

            NodeIterator subNodes;
            subNodes = siteMapRootNode.getNodes();
            while (subNodes.hasNext()) {
                Node subNode = (Node) subNodes.next();
                if (subNode == null) {
                    continue;
                }

                if (subNode.hasProperty("hst:urlmapping")) {
                    Property urlMappingProperty = subNode.getProperty("hst:urlmapping");
                    siteMapNodes.put(urlMappingProperty.getValue().getString(), subNode.getPath());
                } else {
                    log
                            .debug("hst:sitemapitem sitemap item missing 'hst:ulrmapping' property. Item not meant for mapping, but only for binaries");
                }
                
                String  linkRewrite = null;
                boolean isPrefix = false;
                if (subNode.hasProperty("hst:prefixlinkrewrite") || subNode.hasProperty("hst:linkrewriteprefix")) {
                    isPrefix = true;
                    if(subNode.hasProperty("hst:linkrewriteprefix")) {
                         linkRewrite = subNode.getProperty("hst:linkrewriteprefix").getString();
                    } else {
                         linkRewrite = subNode.getProperty("hst:prefixlinkrewrite").getString();
                    }
                    if ("".equals( linkRewrite)) {
                        log.warn("Skipping empty hst:sitemapitem for linkrewriting"
                                + " because of empty hst:prefixlinkrewrite");
                        continue;
                    }
                }
                if(subNode.hasProperty("hst:linkrewrite")) {
                    if(isPrefix) {
                        log.warn("Unambigous linkrewriting configuration because sitemap node contains both 'hst:linkrewriteprefix|hst:prefixlinkrewrite'" +
                        		"and 'hst:linkrewrite'. Using 'hst:linkrewriteprefix|hst:prefixlinkrewrite' property. If links are incorrect rewritten, remove 'hst:prefixlinkrewrite' property");
                    } else {
                        linkRewrite = subNode.getProperty("hst:linkrewrite").getString();
                    }
                }
                if( linkRewrite != null) {
                    if (subNode.hasProperty("hst:nodetype") && subNode.hasProperty("hst:nodepath")) {
                        if (subNode.getProperty("hst:nodetype").getValues().length == subNode.getProperty(
                                "hst:nodepath").getValues().length) {
                            Value[] nodetypes = subNode.getProperty("hst:nodetype").getValues();
                            Value[] nodepaths = subNode.getProperty("hst:nodepath").getValues();
                            for (int i = 0; i < nodepaths.length; i++) {
                                LinkRewriter linkRewriter = new LinkRewriter(linkRewrite, isPrefix , nodetypes[i]
                                        .getString(), nodepaths[i].getString() , physicalEntryPath, repositoryMapping);
                                linkRewriters.add(linkRewriter);
                            }
                        } else {
                            log
                                    .warn("For sitemapitem '"
                                            + subNode.getName()
                                            + "' skipping linkrewriting because length"
                                            + " of multivalued property 'hst:nodetype' is not equal to the length of 'hst:nodepath'. This is mandatory for a proper working linkrewriting item");
                        }
                    } else {
                        log.warn("For sitemapitem '" + subNode.getName() + "' skipping linkrewriting because one of the following properties is missing:" +
                        		"\n hst:nodetype" +
                        		"\n hst:nodepath");
                        		
                    }
                } else {
                    log.debug("skipping sitemap iten '"+subNode.getName()+"' for linkrewriting because does not have property" +
                    		"(hst:prefixlinkrewrite|hst:linkrewriteprefix) OR hst:linkrewrite");
                }
            }
            log.debug("URLMappingImpl constructor took " + (System.currentTimeMillis() - start) + " ms.");
        } catch (PathNotFoundException e) {
            log.warn("URLMapping cannot be build: PathNotFoundException " + e.getMessage());
        } catch (RepositoryException e) {
            log.warn("URLMapping cannot be build:  RepositoryException " + e.getMessage());
        }

    }

    // TODO this method shouldn't be part of the url mapping
    public PageNode getMatchingPageNode(String requestURI, HstRequestContext hstRequestContext) {
        Session session =  hstRequestContext.getJcrSession();
        Iterator<String> patternIter = siteMapNodes.keySet().iterator();
        PageNode pageNode = null;
        String matchNodePath = null;
        while (patternIter.hasNext() && matchNodePath == null) {
            String pagePattern = patternIter.next();
            log.debug("trying to match " + pagePattern + " with " + requestURI);
            //try to find a mapping that matches the requestURI
            Pattern pattern = Pattern.compile(pagePattern);
            Matcher parameterMatcher = pattern.matcher(requestURI);

            if (parameterMatcher.matches()) {
                log.info("match " + pagePattern + " found " + requestURI);
                matchNodePath = siteMapNodes.get(pagePattern); // get appropriate pageNode
                Node matchNode = null;
                try {
                    matchNode = (Node)session.getItem(matchNodePath);
                } catch (PathNotFoundException e1) {
                    log.error("Matching node not found at : '" + matchNodePath +"'");
                } catch (RepositoryException e1) {
                    log.error("RepositoryException for matching sitemap node : '" + matchNodePath +"'");
                } 
                parameterMatcher.reset();
                try {
                    pageNode = new PageNode(hstRequestContext.getHstConfigurationContextBase(), matchNode);
                } catch (RepositoryException e) {
                    log.error("RepositoryException " + e.getMessage());
                }
                while (parameterMatcher.find()) {
                    if (parameterMatcher.groupCount() > 0) {
                        String relativeContentPath = parameterMatcher.group(1); // get back reference value if available
                        log.debug("Relative content path = '" + relativeContentPath +"'");
                        if(relativeContentPath!=null) {
                            pageNode.setRelativeContentPath(relativeContentPath);
                        }
                    }
                }
            }
        }
        if (pageNode != null) {
            return pageNode;
        } else {
            log.warn("no sitemap node matches the request");
            return null;
        }

    }

    public String rewriteLocation(Node node) {
        long start = System.currentTimeMillis();
        String rewritePath = null;
        String path = "";
        StringBuffer rewrite = null;
        try {
            rewritePath = node.getPath();
            String rewritten = this.rewriteLRUCache.get(rewritePath);
            if (rewritten != null) {
                return rewritten;
            }
            if (node instanceof HippoNode) {
                HippoNode hippoNode = (HippoNode) node;
                if (hippoNode.getCanonicalNode() != null && !hippoNode.getCanonicalNode().isSame(node)) {
                    // take canonical node because virtual node found
                    node = hippoNode.getCanonicalNode();
                    
                }
            }
            /*
             * if the parent is handle, we might have the wrong location because for example below the virtual /preview the 
             * nodepath is x/y/z/Foo but below the handle it might be x/y/z/Foo[2]. Therefor, use the path + name of the handle 
             * to get the link and not the location of the hippo document if the parent is a handle
             */  
            boolean isHandle = false;
            if(!node.getPath().equals("/") && node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                node = node.getParent();
                isHandle = true;
            } else if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                isHandle = true;
            }
            path = node.getPath();
            if(isHandle) {
                path = path + "/"+node.getName();
                node = node.getNode(node.getName());
            }
            LinkRewriter bestRewriter = null;
            int highestScore = 0;
            long linkScoreStart = System.currentTimeMillis();
            for (LinkRewriter lrw : linkRewriters) {
                int score = lrw.score(node);
                if (score > highestScore) {
                    if (log.isDebugEnabled()) {
                        if (highestScore == 0) {
                            log.debug("found a match for linkrewriting");
                        } else if (highestScore > 0) {
                            log.debug("found a better match for linkrewriting");
                        }
                    }
                    highestScore = score;
                    bestRewriter = lrw;
                } else if (score > 0 && log.isDebugEnabled()) {
                    log.debug("found a match but already had a better match");
                }
            }
            log.debug("Find best score took " + (System.currentTimeMillis() - linkScoreStart));
            if (bestRewriter == null) {
                log.warn("No matching linkrewriter found for path '" + path + "'. Return node path.");
            } else {
                String url = bestRewriter.getLocation(node);
                rewrite = new StringBuffer(url);
            }

        } catch (ItemNotFoundException e) {
        	log.debug("ItemNotFoundException during link rewriting {} Return node path.", e.getMessage());
        } catch (PathNotFoundException e) {
        	log.debug("PathNotFoundException during link rewriting {}. Return node path.", e.getMessage());
        } catch (AccessDeniedException e) {
        	log.debug("AccessDeniedException during link rewriting {} Return node path.", e.getMessage());
        } catch (RepositoryException e) {
            log.error("RepositoryException during link rewriting {}. Return node path.", e.getMessage());
        }
        log.debug("rewriteLocation for node took " + (System.currentTimeMillis() - start) + " ms.");

        if (rewrite == null) {
            rewrite = (new StringBuffer(repositoryMapping.getPath())).append(path);
        }
        System.out.println(repositoryMapping.getDomainMapping().getServletContextPath());
        if(repositoryMapping.getPrefix()!=null) {
            rewrite.insert(0,repositoryMapping.getPrefix());
        }
        
        if(this.repositoryMapping.getDomainMapping().isServletContextPathInUrl()) {
            rewrite.insert(0, repositoryMapping.getDomainMapping().getServletContextPath());
        }
        
        String rewriteString = rewrite.toString();
        
        rewriteString = UrlUtilities.encodeUrl(rewriteString);
        if (rewritePath != null) {
            this.rewriteLRUCache.put(rewritePath, rewriteString);
        }
        return rewriteString;
    }

    public String getLocation(String path){
        if(repositoryMapping.getDomainMapping().isServletContextPathInUrl()) {
            String contextPath = repositoryMapping.getDomainMapping().getServletContextPath();
            if(contextPath!= null && !contextPath.equals("")) {
                if(contextPath.endsWith("/")) {
                    if(path.startsWith("/")) {
                        path = contextPath + path.substring(1);
                    } else {
                        path = contextPath+path;
                    }
                } else {
                    if(path.startsWith("/")) {
                        path = contextPath+path;
                    } else {
                        path = contextPath+"/"+path;
                    }
                    
                } 
            }
        }
        return path;
    }
    public String rewriteLocation(String sitemapNodeName, Session jcrSession) {
        long start = System.currentTimeMillis();
        String rewritten = this.rewriteLRUCache.get(sitemapNodeName);
        if (rewritten != null) {
            return rewritten;
        }
        Node siteMapRootNode = null;
        if(jcrSession!=null) {
            try {
                siteMapRootNode = (Node)jcrSession.getItem(siteMapRootNodePath);
            } catch (PathNotFoundException e) {
                log.warn("siteMapRootNodePath '" + siteMapRootNodePath +"' not found. Cannot rewrite link");
            } catch (RepositoryException e) {
                log.warn("RepositoryException fetching '" + siteMapRootNodePath +"' not found. Cannot rewrite link");
            }
        }
        StringBuffer rewrite = null;
        if (siteMapRootNode != null && sitemapNodeName != null && !"".equals(sitemapNodeName)) {
            if (sitemapNodeName.startsWith("/")) {
                sitemapNodeName = sitemapNodeName.substring(1);
                if (sitemapNodeName.length() == 0) {
                    log.warn("Unable to rewrite link for sitemap nodename = '/' or ''.");
                    this.rewriteLRUCache.put(sitemapNodeName, "");
                    return "";
                }
            }
            try {
                if (siteMapRootNode.hasNode(sitemapNodeName)) {
                    Node sitemapNode = siteMapRootNode.getNode(sitemapNodeName);
                    String newLink = null;
                    if(sitemapNode.hasProperty("hst:prefixlinkrewrite") || sitemapNode.hasProperty("hst:linkrewriteprefix")) {
                        if(sitemapNode.hasProperty("hst:prefixlinkrewrite")) {
                            newLink =   sitemapNode.getProperty("hst:prefixlinkrewrite").getString();
                        } else {
                            newLink =   sitemapNode.getProperty("hst:linkrewriteprefix").getString();
                        }
                        
                    }
                    if(sitemapNode.hasProperty("hst:linkrewrite")) {
                        if(newLink != null) {
                            log.warn("Unambigous linkrewriting configuration because sitemap node contains both 'hst:linkrewriteprefix|hst:prefixlinkrewrite'" +
                            "and 'hst:linkrewrite'. Using 'hst:linkrewriteprefix|hst:prefixlinkrewrite' property. If links are incorrect rewritten, remove 'hst:prefixlinkrewrite' property");
                        } else {
                            newLink = sitemapNode.getProperty("hst:linkrewrite").getString();
                        }
                    }
                    
                    if(newLink == null) {
                        // this happens a lot, for now set this loglevel to debug
                        log.warn("cannot rewrite path '{}' because the sitemap node does not have the property 'hst:prefixlinkrewrite|hst:linkrewriteprefix' and not hst:linkrewrite'. Node : {}",
                                        sitemapNodeName, sitemapNode.getPath());
                    } else {
                        log.debug("rewriting '{}' --> '{}'", sitemapNodeName, newLink);
                        if (!"".equals(newLink) && !newLink.startsWith("/")) {
                            newLink = "/" + newLink;
                        }
                        rewrite = new StringBuffer(repositoryMapping.getPrefix()).append(newLink);
                    }
                } else {
                    log.warn("'{}' does not exist in sitemap node '{}'. Prefixing path with context, but no rewrite.",
                            sitemapNodeName, siteMapRootNode.getPath());
                }

            } catch (RepositoryException e) {
                log.warn(
                                "Unable to rewrite link for path = '{}'.  Prefixing path with context, but no rewrite. RepositoryException: {}",
                                sitemapNodeName, e.getMessage());
                log.debug("RepositoryException:", e);
            }
        }
        log.debug("rewriteLocation for path took " + (System.currentTimeMillis() - start) + " ms.");
        if (rewrite == null) {
            log.warn("Unable to rewrite '{}' to a sitemap item link", sitemapNodeName);
            this.rewriteLRUCache.put(sitemapNodeName, "");
            return "";
        }
        
        if(this.repositoryMapping.getDomainMapping().isServletContextPathInUrl()) {
            rewrite.insert(0, repositoryMapping.getDomainMapping().getServletContextPath());
        }
        
        String rewriteString = rewrite.toString();
        rewriteString = UrlUtilities.encodeUrl(rewriteString);
        this.rewriteLRUCache.put(sitemapNodeName, rewriteString);
        return rewriteString;
    }

    private class RewriteLRUCache {

        private final Map cache;
        private int miss;
        private int hit;

        private RewriteLRUCache(int size) {
            this.cache =  Collections.synchronizedMap(new LRUMap(size));
        }

        private String get(String key) {
            String rewrite = (String) cache.get(key);
            if (rewrite == null) {
                miss++;
            } else {
                hit++;
            }
            return rewrite;
        }

        private void put(String key, String rewrite) {
            cache.put(key, rewrite);
        }
    }

    public List<String> getCanonicalPathsConfiguration() {
        return this.canonicalPathConfiguration;
    }

    public RepositoryMapping getRepositoryMapping() {
        return repositoryMapping;
    }

}
