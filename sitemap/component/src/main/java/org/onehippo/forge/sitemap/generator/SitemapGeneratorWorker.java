/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.sitemap.generator;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.forge.sitemap.components.UrlInformationProvider;
import org.onehippo.forge.sitemap.components.model.ChangeFrequency;
import org.onehippo.forge.sitemap.components.model.Url;
import org.onehippo.forge.sitemap.components.model.Urlset;
import org.onehippo.forge.sitemap.components.util.RepositoryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;
import static org.onehippo.forge.sitemap.components.util.MatcherUtils.extractPlaceholderValues;
import static org.onehippo.forge.sitemap.components.util.MatcherUtils.getMatcherForIndex;
import static org.onehippo.forge.sitemap.components.util.MatcherUtils.replaceDefaultAndAnyMatchersWithMatchedNodes;
import static org.onehippo.forge.sitemap.components.util.MatcherUtils.replacePlaceholdersWithMatchedNodes;
import static org.onehippo.forge.sitemap.components.util.RepositoryUtils.indexedNodesInPathBMatchIndexedNodesInPathAWhenPathAHasThatNode;
import static org.onehippo.forge.sitemap.components.util.RepositoryUtils.localizePath;

/**
 *
 */
public class SitemapGeneratorWorker extends Thread {

    // Constants
    private static final String JCR_ROOT = "/jcr:root";
    private static final String NODE_TYPE_FOLDER_CONDITION = "["
            + "@jcr:primaryType='" + HippoStdNodeType.NT_FOLDER + "'"
            + " or @jcr:primaryType='" + HippoStdNodeType.NT_DIRECTORY + "'"
            + "]";
    private static final String ELEMENT_MATCHER_FOR_FOLDERS = "element(*, nt:base)" + NODE_TYPE_FOLDER_CONDITION;

    private static final String NODE_TYPE_CONDITION_FOR_PUBLISHED_DOCUMENTS_TEMPLATE =
            "/*[@hippo:availability='{}']/..";
    private static final String QUERY_STRING_FOR_PUBLISHED_DOCUMENTS_TEMPLATE = "element(*, " + HippoNodeType.NT_HANDLE
            + ")" + NODE_TYPE_CONDITION_FOR_PUBLISHED_DOCUMENTS_TEMPLATE;

    // Logger
    private static final Logger LOG = LoggerFactory.getLogger(SitemapGeneratorWorker.class);
    private static final long MS_TO_WAIT_FOR_NEW_TASK = 500;

    // Instance variables
    private final SitemapGenerator generator;
    private final Urlset urlset;
    private final String baseContentPath;
    private final Node baseContentNode;
    private final QueryManager queryManager;
    private final ObjectConverter objectConverter;
    private final HstLinkCreator linkCreator;
    private final HstRequestContext requestContext;
    private final UrlInformationProvider urlInformationProvider;
    private final String publishedNodeTypeCondition;
    private final String queryStringForPublishedDocuments;
    private final Mount mount;

    /**
     * Constructor.
     *
     * @param generator              the Sitemap generator
     * @param urlset                 the Urlset which contains all the Urls
     * @param requestContext         the Hst Request context
     * @param objectConverter        the Object converter converts any kind of beans into JCR nodes & properties
     * @param urlInformationProvider the Url information provider
     */
    public SitemapGeneratorWorker(final SitemapGenerator generator, final Mount mount, final Urlset urlset,
                                  final HstRequestContext requestContext, final ObjectConverter objectConverter,
                                  final UrlInformationProvider urlInformationProvider) {

        LOG.warn("mount type: {}", mount.getType());

        String mountType = mount.getType();
        publishedNodeTypeCondition = NODE_TYPE_CONDITION_FOR_PUBLISHED_DOCUMENTS_TEMPLATE.replace("{}", mountType);
        queryStringForPublishedDocuments = QUERY_STRING_FOR_PUBLISHED_DOCUMENTS_TEMPLATE.replace("{}", mountType);

        this.generator = generator;
        this.mount = mount;
        this.urlset = urlset;
        this.objectConverter = objectConverter;
        this.requestContext = requestContext;
        this.urlInformationProvider = urlInformationProvider;
        linkCreator = requestContext.getHstLinkCreator();
        try {
            Session session = requestContext.getSession();
            queryManager = session.getWorkspace().getQueryManager();
            baseContentPath = mount.getCanonicalContentPath();
            baseContentNode = session.getNode(baseContentPath);
        } catch (RepositoryException e) {
            throw new IllegalStateException("Cannot create SitemapGenerator due to a repository exception", e);
        }
    }

    /**
     * Run method for Threads.
     */
    public void run() {
        try {
            while (!isInterrupted()) {
                final WorkItem workItem = generator.getNextWorkItem();
                if (workItem == null) {
                    try {
                        synchronized (this) {
                            wait(MS_TO_WAIT_FOR_NEW_TASK);
                        }
                    } catch (InterruptedException e) {
                        // The Evil Overmind wants us to stop, let's not ignore that...
                        return;
                    }
                } else {
                    addSiteMapBranchToUrlSet(workItem);
                    generator.finishWorkItem(workItem);
                }
            }
        } catch (RuntimeException e) {
            generator.reportErrorOccurred(e);
        }
    }

    /**
     * Adds the canonical url of all the nodes that match to this sitemap item and their underlying children.
     *
     * @param workItem the work item to process
     */
    private void addSiteMapBranchToUrlSet(final WorkItem workItem) {
        final HstSiteMapItem siteMapItem = workItem.getSiteMapItem();
        final List<String> matchedNodes = workItem.getMatchedNodes();

        if (LOG.isInfoEnabled()) {
            LOG.info("Checking out site map node: {}", HstSiteMapUtils.getPath(siteMapItem));
            LOG.info("* Matched nodes: {}", matchedNodes);
        }

        if (generator.shouldIgnoreSiteMapItem(siteMapItem)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Ignoring sitemap path: {}",
                        replaceDefaultAndAnyMatchersWithMatchedNodes(siteMapItem.getId(), matchedNodes));
            }
            return;
        }

        String componentConfigurationId = siteMapItem.getComponentConfigurationId();
        String siteMapItemNodeName = siteMapItem.getValue();

        if (siteMapItemNodeName.contains(HstNodeTypes.ANY)) {
            addSiteMapAnyMatcherBranchToUrlset(siteMapItem, unmodifiableList(matchedNodes));
        } else if (siteMapItemNodeName.contains(HstNodeTypes.WILDCARD)) {
            addSiteMapDefaultMatcherBranchToUrlset(siteMapItem, unmodifiableList(matchedNodes));
        } else {
            if (!generator.componentConfigurationIdShouldBeExcluded(componentConfigurationId)) {
                String relativeContentPath = siteMapItem.getRelativeContentPath();
                if (!StringUtils.isEmpty(relativeContentPath) && relativeContentPath.matches(".*\\$\\{\\d\\}.*")) {
                    String resolvedContentPath =
                            replacePlaceholdersWithMatchedNodes(relativeContentPath, matchedNodes);
                    addResolvedContentPathToUrlset(resolvedContentPath, relativeContentPath);
                } else {
                    resolveSiteMapItemContainingDefaultOrAnyMatcherAndAddToUrlset(siteMapItem, matchedNodes);
                }
            }
            for (HstSiteMapItem siteMapItemChild : siteMapItem.getChildren()) {
                addSiteMapBranchToUrlSet(siteMapItemChild, matchedNodes);
            }
        }
    }

    /**
     * Adds the sitemap branch to the url set.
     *
     * @param siteMapItem  the {@link HstSiteMapItem} to "parse"
     * @param matchedNodes the {@link List} of node names that have already been matched (${1}, ${2}, etc..)
     */
    private void addSiteMapBranchToUrlSet(final HstSiteMapItem siteMapItem, final List<String> matchedNodes) {
        WorkItem workItem = new WorkItem(siteMapItem, matchedNodes);
        generator.addWorkItem(workItem);
    }

    /**
     * Adds the canonical url of all the nodes that  match to this sitemap item with an any matcher and their
     * underlying children.
     *
     * @param siteMapItem  the {@link HstSiteMapItem} to "parse"
     * @param matchedNodes the {@link List} of node names that have already been matched (${1}, ${2}, etc..)
     */
    private void addSiteMapAnyMatcherBranchToUrlset(final HstSiteMapItem siteMapItem, final List<String> matchedNodes) {
        addSiteMapBranchWithMatcherToUrlset(siteMapItem, matchedNodes, "");
    }

    /**
     * Adds the canonical url of all the nodes that  match to this sitemap item with a default matcher and their
     * underlying children.
     *
     * @param siteMapItem  the {@link HstSiteMapItem} to "parse"
     * @param matchedNodes the {@link List} of node names that have already been matched (${1}, ${2}, etc..)
     */
    private void addSiteMapDefaultMatcherBranchToUrlset(final HstSiteMapItem siteMapItem,
                                                        final List<String> matchedNodes) {
        addSiteMapBranchWithMatcherToUrlset(siteMapItem, matchedNodes, "*");
    }

    /**
     * Adds the canonical url of all the nodes that match to this sitemap item with a matcher and their underlying
     * children.
     *
     * @param siteMapItem        the {@link HstSiteMapItem} to "parse"
     * @param matchedNodes       the {@link List} of node names that have already been matched (${1}, ${2}, etc..)
     * @param matcherReplacement the String to replace the matcher with ('*' for default), ('' for any)
     */
    private void addSiteMapBranchWithMatcherToUrlset(final HstSiteMapItem siteMapItem, final List<String> matchedNodes,
                                                     final String matcherReplacement) {
        String componentConfigurationId = siteMapItem.getComponentConfigurationId();
        boolean ignoreByComponentConfigurationId =
                generator.componentConfigurationIdShouldBeExcluded(componentConfigurationId);
        String contentPath = siteMapItem.getRelativeContentPath();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Relative content path = {} for sitemap item = {}",
                    contentPath, HstSiteMapUtils.getPath(siteMapItem));
        }
        final int curIdx = matchedNodes.size() + 1;
        String matcher = getMatcherForIndex(curIdx);
        boolean canResolveDocuments = contentPath != null && contentPath.contains(matcher);

        if (canResolveDocuments) {
            final String resolvedContentPath = replacePlaceholdersWithMatchedNodes(contentPath, matchedNodes);
            LOG.debug("Resolved relative content path = {}", resolvedContentPath);

            // Resolve every document, add it's url and then resolve the children
            final String normalizedResolvedContentPath = PathUtils.normalizePath(resolvedContentPath);
            final String absoluteContentPath = baseContentPath + "/" + normalizedResolvedContentPath;
            final String resolvedContentPathWithMatcher = absoluteContentPath.replace(matcher, matcherReplacement);

            // Make sure that the path is jcr-encoded
            final String pathForQuery = JCR_ROOT + RepositoryUtils.encodePath(resolvedContentPathWithMatcher);
            if (LOG.isDebugEnabled()) {
                LOG.debug("My path for the query is \"{}\"", pathForQuery);
            }

            List<String> nodePaths = buildQueriesForFoldersAndPublishedNodesAndReturnNodePaths(pathForQuery,
                    absoluteContentPath, ignoreByComponentConfigurationId);

            for (HstSiteMapItem childSiteMapItem : siteMapItem.getChildren()) {
                for (String nodePath : nodePaths) {
                    List<String> newMatchedNodes = getMatchedNodes(contentPath, nodePath, matchedNodes);
                    addSiteMapBranchToUrlSet(childSiteMapItem, Collections.unmodifiableList(newMatchedNodes));
                }
            }
        } else {
            // There is no document to match, so we are matching on anything and passing that to the child nodes
            List<String> newMatchedNodes = new ArrayList<String>(matchedNodes);
            newMatchedNodes.add(matcherReplacement);
            for (HstSiteMapItem childSiteMapItem : siteMapItem.getChildren()) {
                addSiteMapBranchToUrlSet(childSiteMapItem, Collections.unmodifiableList(newMatchedNodes));
            }
        }

    }

    /**
     * Builds the queries for the folder elements and the published nodes. These will be used to retrieve the node
     * paths, which will be returned.
     *
     * @param pathForQuery        jcr-encoded path
     * @param absoluteContentPath the absolute content path
     * @param ignoreByComponentConfigurationId
     *                            true if node path must be ignored due to its component configuration id
     *                            false otherwise
     * @return the node paths based on the queries
     */
    private List<String> buildQueriesForFoldersAndPublishedNodesAndReturnNodePaths(
            final String pathForQuery,
            final String absoluteContentPath,
            final boolean ignoreByComponentConfigurationId) {

        final String queryStringForFolders = buildQueryString(
                pathForQuery,
                ELEMENT_MATCHER_FOR_FOLDERS,
                NODE_TYPE_FOLDER_CONDITION
        );
        final String queryStringForPublishedNodes = buildQueryString(
                pathForQuery,
                queryStringForPublishedDocuments,
                publishedNodeTypeCondition
        );

        return returnNodePathsBasedOnQueries(Arrays.asList(queryStringForFolders, queryStringForPublishedNodes),
                absoluteContentPath, ignoreByComponentConfigurationId);
    }

    /**
     * Returns the node paths based on the queries that have been built.
     *
     * @param queryStrings        a {@link List} of queries to execute
     * @param absoluteContentPath the absolute content path
     * @param ignoreByComponentConfigurationId
     *                            true if node path must be ignored due to its component configuration id
     *                            false otherwise
     * @return the node paths based on the queries
     */
    private List<String> returnNodePathsBasedOnQueries(List<String> queryStrings,
                                                       final String absoluteContentPath,
                                                       final boolean ignoreByComponentConfigurationId) {

        List<String> nodePaths = new ArrayList<String>();
        for (String queryString : queryStrings) {
            List<String> nodePathsForQuery =
                    addToUrlSetAndReturnNodePaths(queryString, absoluteContentPath, ignoreByComponentConfigurationId);
            nodePaths.addAll(nodePathsForQuery);
        }

        return nodePaths;
    }

    /**
     * Checks if the query is already cached, if not it adds Urls and its children to the {@link Urlset}. It returns
     * the node paths based on the query.
     *
     * @param queryString         the query
     * @param absoluteContentPath the absolute content path
     * @param ignoreByComponentConfigurationId
     *                            true if node path must be ignored due to its component configuration id
     *                            false otherwise
     * @return the node paths based on the queries
     */
    private List<String> addToUrlSetAndReturnNodePaths(final String queryString, final String absoluteContentPath,
                                                       final boolean ignoreByComponentConfigurationId) {
        if (!generator.queryIsCached(queryString)) {
            return addUrlsAndChildrenToUrlSetForQuery(queryString, absoluteContentPath,
                    !ignoreByComponentConfigurationId);
        } else {
            return generator.getNodePathsForQueryFromCache(queryString);
        }
    }

    /**
     * Adds the canonical url of the node, which is resolved by the resolved content path.
     *
     * @param resolvedContentPath the relative content path where the place holders are replaced with the matched nodes
     * @param relativeContentPath the path of a site map item containing placeholders
     */
    private void addResolvedContentPathToUrlset(final String resolvedContentPath, final String relativeContentPath) {
        try {
            if (baseContentNode.hasNode(resolvedContentPath)) {
                Node node = baseContentNode.getNode(resolvedContentPath);
                HippoBean hippoBean = obtainHippoBeanForNode(node);
                if (hippoBean == null) {
                    return;
                }
                Url url = createUrlBasedOnNodeWithCanonicalLoc(hippoBean);
                if (url == null) {
                    return;
                }
                urlset.addUrlThatDoesntExistInTheListYet(url);
            } else {
                LOG.debug("Ignoring content path \"{}\"", relativeContentPath);
            }
        } catch (RepositoryException e) {
            throw new IllegalStateException("Repository error occured while resolving content path", e);
        }
    }

    /**
     * Replaces the default or any mather in the path of the site map item and adds the site map item to the urlset.
     *
     * @param siteMapItem  the {@link HstSiteMapItem} to "parse"
     * @param matchedNodes the {@link List} of node names that have already been matched (${1}, ${2}, etc..)
     */
    private void resolveSiteMapItemContainingDefaultOrAnyMatcherAndAddToUrlset(final HstSiteMapItem siteMapItem,
                                                                               final List<String> matchedNodes) {
        Url url = new Url();
        String loc;
        if (matchedNodes.isEmpty()
                && !siteMapItem.getId().contains(HstNodeTypes.WILDCARD)
                && !siteMapItem.getId().contains(HstNodeTypes.ANY)) {
            loc = createLocForSitemapItem(siteMapItem);
        } else {
            String path = replaceDefaultAndAnyMatchersWithMatchedNodes(siteMapItem.getId(), matchedNodes);
            loc = linkCreator.create(path, mount).toUrlForm(requestContext, true);
        }
        url.setLoc(loc);
        urlset.addUrlThatDoesntExistInTheListYet(url);
    }

    /**
     * Creates a query based upon the ending character of the resolved content path.
     *
     * @param pathForQuery      the encoded path of the resolved content path containing a matcher
     * @param matcherCondition  query when pathForQuery ends with a default or any matcher
     * @param nodeTypeCondition query when pathForQuery does not end with a default or any matcher
     * @return The query that can be executed
     */
    private static String buildQueryString(final String pathForQuery, final String matcherCondition,
                                           final String nodeTypeCondition) {
        if (pathForQuery.endsWith("*")) {
            // default matcher at the end of a path
            return pathForQuery.substring(0, pathForQuery.length() - 1) + matcherCondition;
        } else if (pathForQuery.endsWith("/")) {
            // any matcher at the end of a path
            return pathForQuery + "/" + matcherCondition;
        } else {
            return pathForQuery + nodeTypeCondition;
        }
    }

    /**
     * Executes the given query and adds the result to the urlset.
     *
     * @param queryString                   the query the be executed
     * @param absoluteContentPath           the content path containing '/' at the beginning and end of the path
     * @param createLinksForThisSiteMapItem <code>true</code> if this method should add links to the urlest for the
     *                                      resolved nodes, <code>false</code> otherwise
     * @return the {@link List} of node paths
     */
    private List<String> addUrlsAndChildrenToUrlSetForQuery(final String queryString, final String absoluteContentPath,
                                                            final boolean createLinksForThisSiteMapItem) {
        List<String> nodePaths = new ArrayList<String>();
        try {
            @SuppressWarnings("deprecation")
            Query query = queryManager.createQuery(queryString, Query.XPATH);
            QueryResult result = query.execute();
            NodeIterator resultIterator = result.getNodes();
            while (resultIterator.hasNext()) {
                Node node = resultIterator.nextNode();
                if (!indexedNodesInPathBMatchIndexedNodesInPathAWhenPathAHasThatNode(absoluteContentPath,
                        node.getPath())) {
                    // We will find this node later
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Ignoring node \"{}\" for absoluteContentPath \"{}\"", node.getPath(),
                                absoluteContentPath);
                    }
                    continue;
                }
                HippoBean bean = obtainHippoBeanForNode(node);
                if (bean == null) {
                    // Not able to map to HippoBean, go to next node
                    LOG.error("Cannot map node \"{}\" to a HippoBean", node.getPath());
                    continue;
                }

                if (createLinksForThisSiteMapItem && urlInformationProvider.includeDocumentInSiteMap(bean)) {
                    Url url = createUrlBasedOnNodeWithCanonicalLoc(bean);
                    if (url != null) {
                        urlset.addUrlThatDoesntExistInTheListYet(url);
                    }
                }
                if (urlInformationProvider.includeChildrenInSiteMap(bean)) {
                    nodePaths.add(node.getPath());
                }
            }
            generator.addNodePathsForQueryToCache(queryString, nodePaths);
        } catch (RepositoryException e) {
            throw new IllegalStateException("Error when obtaining nodes", e);
        }

        return nodePaths;
    }

    /**
     * @param pathWithPlaceholders the relative content path of a site map item containing placeholders.
     * @param pathToParse          the path of a node which needs to eb parsed
     * @param matchedNodes         the {@link List} of node names that have already been matched (${1}, ${2}, etc..)
     * @return A new List containing an update of the matched nodes
     */
    private List<String> getMatchedNodes(final String pathWithPlaceholders, final String pathToParse,
                                         final List<String> matchedNodes) {
        int curIdx = matchedNodes.size() + 1;

        String localizedPath = localizePath(baseContentPath, pathToParse);
        String contentPathWithPlaceholders =
                replacePlaceholdersWithMatchedNodes(pathWithPlaceholders, matchedNodes, false);

        Map<Integer, String> placeholderValues = extractPlaceholderValues(contentPathWithPlaceholders, localizedPath);

        List<String> newMatchedNodes = new ArrayList<String>(matchedNodes);
        for (Map.Entry<Integer, String> entry : placeholderValues.entrySet()) {
            Integer placeholderNumber = entry.getKey();
            if (placeholderNumber == curIdx) {
                // this is the current level of the site map, which is not in the list yet
                newMatchedNodes.add(entry.getValue());
            } else if (placeholderNumber < curIdx) {
                // this is a replacement for a * matcher
                newMatchedNodes.set(placeholderNumber - 1, entry.getValue());
            } else {
                // There should not be a parameter defined which is not applicable yet
                throw new IllegalStateException("Found a placeholder number that should not exist");
            }
        }

        return newMatchedNodes;
    }

    /**
     * Tries to map the passed Node to a {@link HippoBean}, if that doesn't work, it returns null.
     *
     * @param node the node to map
     * @return {@link HippoBean} representing the node
     */
    private HippoBean obtainHippoBeanForNode(final Node node) {
        Object obj;
        try {
            obj = objectConverter.getObject(node);
        } catch (ObjectBeanManagerException e) {
            String nodePath;
            try {
                nodePath = node.getPath();
            } catch (RepositoryException e1) {
                nodePath = "UNRESOLVABLE PATH";
            }
            LOG.error("Cannot convert node \"{}\"", nodePath);
            throw new IllegalArgumentException("passed node cannot be converted by the object converter", e);
        }
        if (obj instanceof HippoBean) {
            return (HippoBean) obj;
        } else {
            try {
                String nodePath = node.getPath();
                String nodeType = node.getPrimaryNodeType().getName();
                LOG.error("Found a corrupt node. Skipped the node. It is not added to the sitemap. "
                        + "Path = {}, Type = {}", nodePath, nodeType);
            } catch (RepositoryException e) {
                throw new IllegalStateException("Repository Exception when trying to resolve node path and node name.");
            }
            return null;
        }
    }

    /**
     * Creates a {@link Url} based on the passed document.
     *
     * @param hippoBean the HippoBean of the resulting query or based upon the resolved content path
     * @return An empty url or a url with a canonical location tag
     */
    private Url createUrlBasedOnNodeWithCanonicalLoc(final HippoBean hippoBean) {
        if (hippoBean == null) {
            return null;
        }

        if (!urlInformationProvider.includeDocumentInSiteMap(hippoBean)) {
            return null;
        }

        String loc = urlInformationProvider.getLoc(hippoBean, requestContext, mount);
        ChangeFrequency changeFreq = urlInformationProvider.getChangeFrequency(hippoBean);
        Calendar lastMod = urlInformationProvider.getLastModified(hippoBean);
        BigDecimal priority = urlInformationProvider.getPriority(hippoBean);
        Url url = new Url();
        url.setLoc(loc);
        url.setChangeFrequency(changeFreq);
        url.setLastmod(lastMod);
        url.setPriority(priority);

        return url;
    }

    /**
     * Creates the location tag for a sitemap item.
     *
     * @param siteMapItem the {@link HstSiteMapItem} to "parse"
     * @return the location tag for the sitemap item
     */
    private String createLocForSitemapItem(final HstSiteMapItem siteMapItem) {
        return linkCreator.create(siteMapItem, mount).toUrlForm(requestContext, true);
    }
}