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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.hippoecm.hst.core.filters.domain.DomainMapping;
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
    private final URLMappingManager urlMappingManager;

    // a list containing all canonical paths which are used in the url mapping. These paths are used to create named events
    // on which the cache is invalidated
    private final List<String> canonicalPathConfiguration;

    private String siteMapRootNodePath;

    public URLMappingImpl(RepositoryMapping repositoryMapping, URLMappingManager urlMappingManager, Session jcrSession)
            throws URLMappingException {
        this.urlMappingManager = urlMappingManager;
        this.repositoryMapping = repositoryMapping;
        this.rewriteLRUCache = new RewriteLRUCache(500);
        this.canonicalPathConfiguration = new ArrayList<String>();
        try {
            long start = System.currentTimeMillis();

            HippoNode hstConf = (HippoNode) jcrSession.getItem(repositoryMapping.getHstConfigPath());
            Node canonical = hstConf.getCanonicalNode();
            if (canonical != null) {
                this.canonicalPathConfiguration.add(canonical.getPath());
            }

            // TODO when the configuration is a combination of multiple facetselects, we need to add all canonical path
            // configurations. currently, only the base path is added

            Node siteMapRootNode = hstConf.getNode(HstBaseFilter.SITEMAP_RELATIVE_LOCATION);
            siteMapRootNodePath = siteMapRootNode.getPath();

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

                String linkRewrite = null;
                boolean isPrefix = false;
                if (subNode.hasProperty("hst:prefixlinkrewrite") || subNode.hasProperty("hst:linkrewriteprefix")) {
                    isPrefix = true;
                    if (subNode.hasProperty("hst:linkrewriteprefix")) {
                        linkRewrite = subNode.getProperty("hst:linkrewriteprefix").getString();
                    } else {
                        linkRewrite = subNode.getProperty("hst:prefixlinkrewrite").getString();
                    }
                    if ("".equals(linkRewrite)) {
                        log.warn("Skipping empty hst:sitemapitem for linkrewriting"
                                + " because of empty hst:prefixlinkrewrite");
                        continue;
                    }
                }
                if (subNode.hasProperty("hst:linkrewrite")) {
                    if (isPrefix) {
                        log.warn("Ambiguous linkrewriting configuration because sitemap node contains both 'hst:linkrewriteprefix|hst:prefixlinkrewrite'"
                                        + "and 'hst:linkrewrite'. Using 'hst:linkrewriteprefix|hst:prefixlinkrewrite' property. If links are incorrect rewritten, remove 'hst:prefixlinkrewrite' property");
                    } else {
                        linkRewrite = subNode.getProperty("hst:linkrewrite").getString();
                    }
                }
                if (linkRewrite != null) {
                    if (subNode.hasProperty("hst:nodetype") && subNode.hasProperty("hst:nodepath")) {
                        if (subNode.getProperty("hst:nodetype").getValues().length == subNode.getProperty(
                                "hst:nodepath").getValues().length) {
                            Value[] nodetypes = subNode.getProperty("hst:nodetype").getValues();
                            Value[] nodepaths = subNode.getProperty("hst:nodepath").getValues();
                            for (int i = 0; i < nodepaths.length; i++) {
                                LinkRewriter linkRewriter = new LinkRewriter(subNode.getName(), linkRewrite, isPrefix, nodetypes[i]
                                        .getString(), nodepaths[i].getString(), repositoryMapping);
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
                        log.debug("sitemapitem is not used for linkrewriting but might be used to link directly to (hst:linkrewrite is used in that casse)");

                    }
                } else {
                    log.debug("skipping sitemap iten '" + subNode.getName()
                            + "' for linkrewriting because does not have property"
                            + "(hst:prefixlinkrewrite|hst:linkrewriteprefix) OR hst:linkrewrite");
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
        Session session = hstRequestContext.getJcrSession();
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
                    matchNode = (Node) session.getItem(matchNodePath);
                } catch (PathNotFoundException e1) {
                    log.error("Matching node not found at : '" + matchNodePath + "'");
                } catch (RepositoryException e1) {
                    log.error("RepositoryException for matching sitemap node : '" + matchNodePath + "'");
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
                        log.debug("Relative content path = '" + relativeContentPath + "'");
                        if (relativeContentPath != null) {
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

    public Link rewriteLocation(Node node, HstRequestContext hstRequestContext, boolean external) {
        return rewriteLocation(node, null, false, hstRequestContext, external, null);
    }
    
    public Link rewriteLocation(Node node, String sitemap, HstRequestContext hstRequestContext, boolean external) {
        return rewriteLocation(node, sitemap, false, hstRequestContext, external, null);
    }

    private Link rewriteLocation(Node node, String sitemap, boolean secondTry, HstRequestContext hstRequestContext, boolean external, RepositoryMapping newRepositoryMapping) {
        String rewritePath = null;
        String path = "";
        String rewrite = null;
        String cacheKey = null;
        try {
            rewritePath = node.getPath();
            cacheKey = computeCacheKey(rewritePath, external, secondTry, sitemap, hstRequestContext);
            Link rewritten = this.rewriteLRUCache.get(cacheKey);
            if (rewritten != null) {
                return rewritten;
            }
            if (!secondTry) {
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
                if (!node.getPath().equals("/") && node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                    node = node.getParent();
                    isHandle = true;
                } else if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    isHandle = true;
                }
                path = node.getPath();
                if (isHandle) {
                    path = path + "/" + node.getName();
                    node = node.getNode(node.getName());
                }
            }

            boolean isBinary = false;
            if (this.repositoryMapping.getDomainMapping().isBinary(path)) {
                isBinary = true;
            }

            // first test for 'isBinary' because all subsites/domains share the asset location at,
            if (!secondTry && !isBinary && !path.startsWith(this.repositoryMapping.getCanonicalContentPath())) {
                /*
                 * we have a node outside the scope of the current repository mapping / urlmapping. This means we need to
                 * 1) find the correct (= the best) repository mapping
                 * 2) get the URLMapping for this repository mapping through the URLMappingManager
                 * 3) translate the url to an external link with the help of the found URLMapping + the global DomainMapping, where
                 * the latter has info about displaying the ContextPath, Port number, Scheme, etc
                 */
                DomainMapping domainMapping = this.repositoryMapping.getDomainMapping();
                RepositoryMapping matchingRepositoryMapping = domainMapping.getRepositoryMapping(path, this.repositoryMapping);
                if (matchingRepositoryMapping != null) {
                    try {
                        URLMappingImpl newUrlMapping = (URLMappingImpl) this.urlMappingManager.getUrlMapping(
                                matchingRepositoryMapping, this.urlMappingManager, node.getSession());
                        
                        // if the domain belonging to the matching repository mappingis not equal to the current domain, we have to externalize (including http://hostname:port ...) the link
                    
                        if(matchingRepositoryMapping.getDomain() != this.repositoryMapping.getDomain()) {
                            external = true;
                        }
                        
                        // set second try to true to avoid recusive possible loop
                        Link link =  newUrlMapping.rewriteLocation(node, sitemap, true, hstRequestContext, external, matchingRepositoryMapping);
                        this.rewriteLRUCache.put(cacheKey, link);
                        return link;
                    } catch (URLMappingException e) {
                        log.warn("Exception while getting url mapping for '{}' : {}", matchingRepositoryMapping.getHstConfigPath(), e.getMessage());
                    }

                } else {
                    log.warn("Cannot rewrite a link to '{}' because no repository mapping at all links to one of its ancestors", path);
                }
            } else {
                
                LinkRewriter bestRewriter = null;
                int highestScore = 0;
                
                Set<String> precedenceSet = null;
                // if there is a precendence set, first try if a precedence sitemap name can rewrite the link
                if(sitemap != null) {
                    precedenceSet = new HashSet<String>();
                    precedenceSet.addAll(Arrays.asList(sitemap.split("\\|")));
                    for (LinkRewriter lrw : linkRewriters) {
                        if(precedenceSet.contains(lrw.getSiteMapItemName())) {
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
                    }
                    if(bestRewriter != null) {
                        log.debug("Found a matching linkrewriter for a 'preferred' sitemap node '{}'", bestRewriter.getSiteMapItemName());
                    }
                }
                
                highestScore = 0;
                if(bestRewriter == null) {
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
                }
                if (bestRewriter == null) {
                    log.warn("No matching linkrewriter found for path '" + path + "'. Return node path.");
                } else {
                    rewrite = bestRewriter.getLocation(node, isBinary);
                }
            }

        } catch (ItemNotFoundException e) {
            log.debug("ItemNotFoundException during link rewriting {}.", e.getMessage());
        } catch (PathNotFoundException e) {
            log.debug("PathNotFoundException during link rewriting {}.", e.getMessage());
        } catch (AccessDeniedException e) {
            log.debug("AccessDeniedException during link rewriting {}.", e.getMessage());
        } catch (RepositoryException e) {
            log.warn("RepositoryException during link rewriting {}.", e.getMessage());
        }

        if (rewrite == null) {
            log.warn("rewrite failed: Return ''");
            rewrite = "";
        }
        
        rewrite = UrlUtilities.encodeUrl(rewrite);
        
        if(external) {
            rewrite = externalize(rewrite, this.repositoryMapping.getDomainMapping(),path, hstRequestContext, newRepositoryMapping);  
        }
        
        Link link = new LinkImpl(rewrite, external, true);
        if (rewrite != null && cacheKey != null) {
            this.rewriteLRUCache.put(cacheKey, link);
        }
        return link;
    }

    public Link rewriteLocation(String sitemapNodeName, HstRequestContext hstRequestContext, boolean external) {
        long start = System.currentTimeMillis();
        String cacheKey = computeCacheKey(sitemapNodeName, false, false, null, hstRequestContext);
        Link rewritten = this.rewriteLRUCache.get(cacheKey);
        String path = "" ;
        if (rewritten != null) {
            return rewritten;
        }
        Node siteMapRootNode = null;
        if (hstRequestContext.getJcrSession() != null) {
            try {
                siteMapRootNode = (Node) hstRequestContext.getJcrSession().getItem(siteMapRootNodePath);
            } catch (PathNotFoundException e) {
                log.warn("siteMapRootNodePath '" + siteMapRootNodePath + "' not found. Cannot rewrite link");
            } catch (RepositoryException e) {
                log.warn("RepositoryException fetching '" + siteMapRootNodePath + "' not found. Cannot rewrite link");
            }
        }
        StringBuffer rewrite = null;
        if (siteMapRootNode != null && sitemapNodeName != null && !"".equals(sitemapNodeName)) {
            if (sitemapNodeName.startsWith("/")) {
                sitemapNodeName = sitemapNodeName.substring(1);
                if (sitemapNodeName.length() == 0) {
                    log.warn("Unable to rewrite link for sitemap nodename = '/' or ''.");
                    Link link = new LinkImpl("",false,true);
                    if (cacheKey != null) {
                        this.rewriteLRUCache.put(cacheKey, link);
                    }
                    return link;
                }
            }
            try {
                if (siteMapRootNode.hasNode(sitemapNodeName)) {
                    Node sitemapNode = siteMapRootNode.getNode(sitemapNodeName);
                    path = sitemapNode.getPath();
                    String newLink = null;
                    if (sitemapNode.hasProperty("hst:prefixlinkrewrite")
                            || sitemapNode.hasProperty("hst:linkrewriteprefix")) {
                        if (sitemapNode.hasProperty("hst:prefixlinkrewrite")) {
                            newLink = sitemapNode.getProperty("hst:prefixlinkrewrite").getString();
                        } else {
                            newLink = sitemapNode.getProperty("hst:linkrewriteprefix").getString();
                        }

                    }
                    if (sitemapNode.hasProperty("hst:linkrewrite")) {
                        if (newLink != null) {
                            log
                                    .warn("Unambigous linkrewriting configuration because sitemap node contains both 'hst:linkrewriteprefix|hst:prefixlinkrewrite'"
                                            + "and 'hst:linkrewrite'. Using 'hst:linkrewriteprefix|hst:prefixlinkrewrite' property. If links are incorrect rewritten, remove 'hst:prefixlinkrewrite' property");
                        } else {
                            newLink = sitemapNode.getProperty("hst:linkrewrite").getString();
                        }
                    }

                    if (newLink == null) {
                        // this happens a lot, for now set this loglevel to debug
                        log
                                .warn(
                                        "cannot rewrite path '{}' because the sitemap node does not have the property 'hst:prefixlinkrewrite|hst:linkrewriteprefix' and not hst:linkrewrite'. Node : {}",
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
                log
                        .warn(
                                "Unable to rewrite link for path = '{}'.  Prefixing path with context, but no rewrite. RepositoryException: {}",
                                sitemapNodeName, e.getMessage());
                log.debug("RepositoryException:", e);
            }
        }
        log.debug("rewriteLocation for path took " + (System.currentTimeMillis() - start) + " ms.");
        if (rewrite == null) {
            Link link = new LinkImpl("", false, true);
            log.warn("Unable to rewrite '{}' to a sitemap item link", sitemapNodeName);
            if (cacheKey != null) {
                this.rewriteLRUCache.put(cacheKey, link);
            }
            return link;
        }

        if (this.repositoryMapping.getDomainMapping().isServletContextPathInUrl()) {
            rewrite.insert(0, repositoryMapping.getDomainMapping().getServletContextPath());
        }

        String rewriteString = rewrite.toString();
        rewriteString = UrlUtilities.encodeUrl(rewriteString);
        
        if(external) {
            rewriteString = externalize(rewriteString, this.repositoryMapping.getDomainMapping(),path, hstRequestContext, null);  
        }
        
        Link link = new LinkImpl(rewriteString, external, true);
        if (cacheKey != null) {
            this.rewriteLRUCache.put(cacheKey, link);
        }
        return link;
    }

    public Link getLocation(String path, HstRequestContext hstRequestContext, boolean external) {
        if (repositoryMapping.getDomainMapping().isServletContextPathInUrl()) {
            String contextPath = repositoryMapping.getDomainMapping().getServletContextPath();
            if (contextPath != null && !contextPath.equals("")) {
                if (contextPath.endsWith("/")) {
                    if (path.startsWith("/")) {
                        path = contextPath + path.substring(1);
                    } else {
                        path = contextPath + path;
                    }
                } else {
                    if (path.startsWith("/")) {
                        path = contextPath + path;
                    } else {
                        path = contextPath + "/" + path;
                    }

                }
            }
        }
        if(external) {
            path = externalize(path, this.repositoryMapping.getDomainMapping(),path, hstRequestContext, null);  
        }
        Link link = new LinkImpl(path, external, true);
        return link;
    }

    private String externalize(String rewrite, DomainMapping domainMapping, String path, HstRequestContext hstRequestContext, RepositoryMapping newRepositoryMapping) {
        StringBuffer externalLink = new StringBuffer(domainMapping.getScheme()).append("://");
        if(newRepositoryMapping != null) {
            // we have to link to a different domain, hence we cannot get the serverName from the request
            log.debug("External link to different domain for node '{}'", path);
            log.debug("Rewriting link from domain '{}' --> domain '{}'", hstRequestContext.getRequest().getServerName(), newRepositoryMapping.getDomain().getPattern());
            externalLink.append(repositoryMapping.getDomain().getPattern());
        } else {
            log.debug("Rewrite link to an external (including hostname) link");
            externalLink.append(hstRequestContext.getRequest().getServerName());
        }
        
        if(domainMapping.isPortInUrl()) {
            externalLink.append(":").append(domainMapping.getPort());
        }
        externalLink.append(rewrite);
        return externalLink.toString();
    }
    
    /*
     * we need to account for the current repository mapping in the cachekey, because one and the same repository node can be 
     * translated into different links. Therefor we include the servername & repository mapping domain to the cachekey
     */

    private String computeCacheKey(String name, boolean externalize, boolean secondTry, String precedence, HstRequestContext hstRequestContext) {
        StringBuffer key = new StringBuffer();
        key.append(hstRequestContext.getRequest().getServerName());
        key.append("_");
        key.append(name);
        key.append("_").append(externalize);
        key.append("_").append(secondTry);
        key.append("_").append(precedence);
        key.append("_").append(this.repositoryMapping.getDomain().hashCode());
        return key.toString();
    }
    private class RewriteLRUCache {

        private final Map cache;
        private int miss;
        private int hit;

        private RewriteLRUCache(int size) {
            this.cache = Collections.synchronizedMap(new LRUMap(size));
        }

        private Link get(String key) {
            Link rewrite = (Link) cache.get(key);
            if (rewrite == null) {
                miss++;
            } else {
                hit++;
            }
            return rewrite;
        }

        private void put(String key, Link rewrite) {
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
