/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_REFERECENCECOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_ABSTRACT_COMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.PROTOTYPE_META_PROPERTY_PRIMARY_CONTAINER;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;

public class PagesHelper extends AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(PagesHelper.class);

    @SuppressWarnings("unchecked")
    @Override
    public Object getConfigObject(final String itemId) {
        throw new UnsupportedOperationException("not supported");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getConfigObject(final String itemId, final Mount mount) {
        throw new UnsupportedOperationException("not supported");
    }

    @Override
    protected String getNodeType() {
        return NODETYPE_HST_ABSTRACT_COMPONENT;
    }

    public Node create(final Node prototype, final String targetPageNodeName) throws RepositoryException {
        return create(prototype, targetPageNodeName, null, false);
    }


    public Node create(final Node pageOrPrototype,
                       final String targetPageNodeName,
                       final HstComponentConfiguration pageInstance,
                       final boolean skipContainerItems) throws RepositoryException {
        final String previewWorkspacePagesPath = getPreviewWorkspacePagesPath();
        return create(pageOrPrototype, targetPageNodeName, pageInstance, skipContainerItems, previewWorkspacePagesPath);
    }

    public Node create(final Node pageOrPrototype,
                       final String targetPageNodeName,
                       final HstComponentConfiguration pageInstance,
                       final boolean skipContainerItems,
                       final String previewWorkspacePagesPath) throws RepositoryException {
        final Session session = pageComposerContextService.getRequestContext().getSession();
        if (!session.nodeExists(previewWorkspacePagesPath)) {
            createWorkspacePagesInPreviewAndLive(previewWorkspacePagesPath, session);
        }
        final String validTargetPageNodeName = getValidTargetPageNodeName(previewWorkspacePagesPath, targetPageNodeName, session);
        final Node newPage = JcrUtils.copy(session, pageOrPrototype.getPath(), previewWorkspacePagesPath + "/" + validTargetPageNodeName);

        if (newPage.isNodeType(HstNodeTypes.MIXINTYPE_HST_PROTOTYPE_META)) {
            newPage.removeMixin(HstNodeTypes.MIXINTYPE_HST_PROTOTYPE_META);
        }
        if (skipContainerItems) {
            removeContainerItems(newPage);
        }
        if (pageInstance != null) {
            // copy has been done from a page, not from a prototype. We need to check whether there
            // are no hst:containercomponentreference used. If so, we need to denormalize them. For that, we need the
            // pageInstance
            denormalizeContainerComponentReferences(newPage, pageInstance);
        }

        lockHelper.acquireLock(newPage, 0);
        // lock all available containers below newPage
        for (Node eachContainer : findContainers(newPage)) {
            lockHelper.acquireSimpleLock(eachContainer, 0);
        }

        return newPage;
    }

    private void createWorkspacePagesInPreviewAndLive(final String previewWorkspacePagesPath, final Session session) throws RepositoryException {
        final String previewWorkspacePath = StringUtils.substringBeforeLast(previewWorkspacePagesPath, "/");
        session.getNode(previewWorkspacePath).addNode(NODENAME_HST_PAGES, NODETYPE_HST_PAGES);
        final String liveWorkspacePath = previewWorkspacePath.replace("-preview/","/");
        if (!session.nodeExists(liveWorkspacePath + "/" + NODENAME_HST_PAGES)) {
            session.getNode(liveWorkspacePath).addNode(NODENAME_HST_PAGES, NODETYPE_HST_PAGES);
        }
    }

    public Node copy(final Session session, final String targetPageName, final HstComponentConfiguration sourcePage,
                     final Mount sourceMount, final Mount targetMount)
            throws RepositoryException {
        Node newPageNode = create(session.getNodeByIdentifier(sourcePage.getCanonicalIdentifier()),
                targetPageName, sourcePage, false, getWorkspacePath(targetMount) + "/" + NODENAME_HST_PAGES);

        if (sourceMount.getIdentifier().equals(targetMount.getIdentifier())) {
            // copy within same channel. We do not need to check all referenced components
            return newPageNode;
        }
        log.info("Cross channel copy between source '{}' and target '{}' requires all component references to be available in the target " +
                "mount as well.", sourceMount, targetMount);
        // check references and whether they can be resolved in target mount
        HashSet<String> checkedNodes = new HashSet<>();
        checkReferencesRecursive(newPageNode, sourceMount.getHstSite(), targetMount.getHstSite(), checkedNodes);

        return newPageNode;
    }

    private void checkReferencesRecursive(final Node node, final HstSite sourceSite, final HstSite targetSite,
                                          final HashSet<String> checkedNodes) throws RepositoryException {
        if (checkedNodes.contains(node.getIdentifier())) {
            log.warn("Reference recursion found for node '{}'. Skip further checking", node.getPath());
        }
        checkedNodes.add(node.getIdentifier());
        if (node.hasProperty(COMPONENT_PROPERTY_REFERECENCECOMPONENT)) {
            String reference = node.getProperty(COMPONENT_PROPERTY_REFERECENCECOMPONENT).getString();
            if (isNotBlank(reference)) {
                copyReferenceIfMissing(node, sourceSite, targetSite, reference, checkedNodes);
            }
        }
        // check all references of all the descendants as well
        for (Node child : new NodeIterable(node.getNodes())) {
            checkReferencesRecursive(child, sourceSite, targetSite, checkedNodes);
        }
    }

    private void copyReferenceIfMissing(final Node node, final HstSite sourceSite, final HstSite targetSite,
                                        final String reference, final HashSet<String> checkedNodes) throws RepositoryException {
        final HstComponentConfiguration targetReference = targetSite.getComponentsConfiguration().getComponentConfiguration(reference);
        final Session session = node.getSession();
        if (targetReference != null) {
            // if the main config in the target is locked by someone else, it might be that the reference is not available in live
            // configuration. In that case, a lock exception should be triggered
            assertTargetReferenceAvailable(targetSite, reference, targetReference, session);
            return;
        } else {
            log.info("Referenced component '{}' is missing in targetMount '{}'. Try to copy it from the sourceMount '{}'.",
                    reference, targetSite, sourceSite);
            final HstComponentConfiguration sourceReference = sourceSite.getComponentsConfiguration().getComponentConfiguration(reference);
            if (sourceReference == null) {
                log.warn("Referenced component '{}' is missing in targetMount '{}' but also missing in sourceMount '{}'. Skip it.");
                return;
            }
            final Node referencedNode = session.getNodeByIdentifier(sourceReference.getCanonicalIdentifier());
            final List<String> configurationRelativePathSegments = getConfigurationRelativePathSegments(referencedNode);
            final String configurationRelativePath = getConfigurationRelativePath(configurationRelativePathSegments);
            final String targetAbsolutePath = targetSite.getConfigurationPath() + "/" + configurationRelativePath;
            if (session.nodeExists(targetAbsolutePath)) {
                log.debug("Target reference '{}' exists already in '{}'. Most likely it was not yet part of the model before. " +
                        "Nothing needs to be copied.", targetAbsolutePath, targetSite);
                return;
            }
            final String mainConfigNodeName = configurationRelativePathSegments.get(0);
            final String mainConfigNodePath = targetSite.getConfigurationPath() + "/" + mainConfigNodeName;
            if (!session.nodeExists(mainConfigNodePath)) {
                session.getNode(targetSite.getConfigurationPath()).addNode(mainConfigNodeName);
            }
            // try to acquire the lock first
            final Node mainConfigNode = session.getNode(mainConfigNodePath);
            lockHelper.acquireSimpleLock(mainConfigNode, 0L);

            // copy the first missing node
            Node currentNode = mainConfigNode;
            for (int i = 1; i < configurationRelativePathSegments.size(); i++) {
                if (!currentNode.hasNode(configurationRelativePathSegments.get(i))) {
                    // found the missing node : This one needs to be copied
                    // first find the original
                    final String existingRelativePath = getConfigurationRelativePath(configurationRelativePathSegments.subList(0, i));
                    final Node copy = JcrUtils.copy(session, sourceSite.getConfigurationPath() + "/" + existingRelativePath + "/" + configurationRelativePathSegments.get(i),
                            targetSite.getConfigurationPath() + "/" + existingRelativePath + "/" + configurationRelativePathSegments.get(i));
                    // repeat the same reference checking for the copied node
                    checkReferencesRecursive(copy, sourceSite, targetSite, checkedNodes);
                }
            }
        }
    }

    /**
     * @throws RepositoryException
     * @throws ClientException in case the targetReference is available only in preview but has been locked by someone else: In
     * that case, the targetReference is not available in live yet
     */
    private void assertTargetReferenceAvailable(final HstSite targetSite,
                                                final String reference,
                                                final HstComponentConfiguration targetReference,
                                                final Session session) throws RepositoryException, ClientException {
        if (targetReference.isInherited()) {
            // inherited implies it must be live
            log.debug("No need to copy referenced component '{}' because referenced component '{}' already is available in targetMount '{}'",
                    reference, targetReference, targetSite);
            return;
        }

        final String absPreviewPath = targetReference.getCanonicalStoredLocation();
        final String absLivePath = absPreviewPath.replace("-preview/", "/");
        if (session.nodeExists(absLivePath)) {
            // configuration is already live, so no problem for sure
            log.debug("No need to copy referenced component '{}' because referenced component '{}' already is available in live targetMount '{}'",
                    reference, targetReference, targetSite);
            return;
        }
        // targetReference is present in preview, but not in live. We need to check whether the current user also owns the
        // lock on the 'main config node'. Otherwise, it implies someone else has the lock, in which case, this action cannot
        // be completed (because if this user would publish, the live still wouldn't have the referenced node)

        final List<String> configurationRelativePathSegments = getConfigurationRelativePathSegments(session.getNode(absPreviewPath));
        // if current user does not own the lock, the acquireSimpleLock will throw an exception which also should be the case
        // if the lock is not owned on the main config node
        final String mainConfigNodeName = configurationRelativePathSegments.get(0);
        final String mainConfigNodePath = targetSite.getConfigurationPath() + "/" + mainConfigNodeName;
        final Node mainConfigNode = session.getNode(mainConfigNodePath);
        lockHelper.acquireSimpleLock(mainConfigNode, 0L);
        log.debug("reference '{}' is already present in preview configuration and the lock on '{}' is also owned.",
                absPreviewPath, mainConfigNodePath);
        return;
    }

    /**
     * returns a path like  {@code hst:abstractpages/base } for a list like {@code {hst:abstractpages,base} }
     */
    private String getConfigurationRelativePath(final List<String> configurationRelativePathSegments) {
        StringBuilder builder = new StringBuilder();
        for (String configurationRelativePathSegment : configurationRelativePathSegments) {
            builder.append("/").append(configurationRelativePathSegment);
        }
        // remove first slash
        return builder.substring(1);
    }

    /**
     * returns a list like  {@code {hst:abstractpages,base} } for a node at
     * {@code /hst:hst/hst:configurations/myproject/hst:abstractpages/base}
     */
    private List<String> getConfigurationRelativePathSegments(final Node referencedNode) throws RepositoryException {
        List<String> relativeReferencePathSegments = new ArrayList<>();
        Node component = referencedNode;
        while (component.isNodeType(NODETYPE_HST_ABSTRACT_COMPONENT)) {
            relativeReferencePathSegments.add(0, component.getName());
            component = component.getParent();
        }
        // component now is hst:abstractpages, hst:components or hst:pages
        relativeReferencePathSegments.add(0, component.getName());
        return relativeReferencePathSegments;
    }

    public Node rename(final Node oldPage, final String targetPageNodeName) throws RepositoryException {
        // check if not locked
        lockHelper.acquireLock(oldPage, 0);
        Node newPage = create(oldPage, targetPageNodeName, null, false);
        deletePageNodeIfNotReferencedAnyMore(oldPage);
        return newPage;
    }

    public Node reapply(final Node newPrototypePage,
                        final Node oldPage,
                        final String targetPageNodeName) throws RepositoryException {

        // check if not locked
        lockHelper.acquireLock(oldPage, 0);
        Node newPage = create(newPrototypePage, targetPageNodeName, null, true);

        Node primaryContainer = findPrimaryContainer(newPage, getStringProperty(newPrototypePage, PROTOTYPE_META_PROPERTY_PRIMARY_CONTAINER, null));
        if (primaryContainer == null) {
            noContainerLeftInfoLog(newPrototypePage, oldPage);
            deletePageNodeIfNotReferencedAnyMore(oldPage);
            return newPage;
        }

        String newPagePathPrefix = "/" + NODENAME_HST_PAGES + "/" + newPage.getName() + "/";
        String oldPagePathPrefix = "/" + NODENAME_HST_PAGES + "/" + oldPage.getName() + "/";

        List<Node> existingContainers = findContainers(oldPage);
        Session session = oldPage.getSession();
        List<Node> nonRelocatedContainers = new ArrayList<>();
        for (Node existingContainer : existingContainers) {
            String targetContainerPath = existingContainer.getPath().replace(oldPagePathPrefix, newPagePathPrefix);
            if (session.nodeExists(targetContainerPath)) {
                Node targetNode = session.getNode(targetContainerPath);
                if (targetNode.isNodeType(NODETYPE_HST_CONTAINERCOMPONENT)) {
                    moveContainerItems(existingContainer, targetNode);
                    continue;
                }
            }
            nonRelocatedContainers.add(existingContainer);
        }

        if (!nonRelocatedContainers.isEmpty()) {
            log.info("Re-applying prototype results in moving container items that could not be" +
                    " relocated to the same containers:");
            for (Node container : nonRelocatedContainers) {
                log.info("Moving container items from '{}' to '{}'", container.getPath(),
                        primaryContainer.getPath());
                moveContainerItems(container, primaryContainer);
            }
        }

        deletePageNodeIfNotReferencedAnyMore(oldPage);
        return newPage;
    }


    private void removeContainerItems(final Node page) throws RepositoryException {
        final List<Node> containers = findContainers(page);
        for (Node container : containers) {
            for (Node child : new NodeIterable(container.getNodes())) {
                log.debug("Remove container item '{}' for container '{}'.", child.getName(), container.getPath());
                child.remove();
            }
        }
    }

    private void moveContainerItems(final Node from, final Node to) throws RepositoryException {
        Session session = from.getSession();
        for (Node fromChild : new NodeIterable(from.getNodes())) {
            String newName = fromChild.getName();
            int counter = 0;
            while (to.hasNode(newName)) {
                newName = fromChild.getName() + ++counter;
            }
            session.move(fromChild.getPath(), to.getPath() + "/" + newName);
        }
    }

    /**
     * @return the container that is marked to be the primary one or if none marked or found for the primary, return the
     * first container. If <code>newPage</code> does not contain any container, <code>null</code> is returned
     */
    private Node findPrimaryContainer(final Node page, final String primaryContainerRelPath) throws RepositoryException {
        final List<Node> pageContainers = findContainers(page);
        if (pageContainers.isEmpty()) {
            log.debug("No containers available on page '{}'", page.getPath());
            return null;
        } else if (primaryContainerRelPath != null) {
            int pageNodePathPrefixLength = page.getPath().length() + 1;
            for (Node pageContainer : pageContainers) {
                String relContainerPath = pageContainer.getPath().substring(pageNodePathPrefixLength);
                if (primaryContainerRelPath.equals(relContainerPath)) {
                    return pageContainer;
                }
            }
            log.warn("Could not find primary container '{}' for page '{}'. Return first container instead.",
                    primaryContainerRelPath, page.getPath());
            return pageContainers.get(0);
        } else {
            log.debug("No primary container configured on prototype. Return first container for page '{}' " +
                    "as primary container.", page.getPath());
            return pageContainers.get(0);
        }
    }

    private List<Node> findContainers(final Node node) throws RepositoryException {
        List<Node> existingContainers = new ArrayList<>();
        findContainers(node, existingContainers);
        return existingContainers;
    }

    private void findContainers(final Node node, List<Node> containers) throws RepositoryException {
        if (node.isNodeType(NODETYPE_HST_CONTAINERCOMPONENT)) {
            containers.add(node);
            // container component nodes never have container component children
            return;
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            findContainers(child, containers);
        }
    }


    private void noContainerLeftInfoLog(final Node newPrototypePage, final Node oldPage) throws RepositoryException {
        log.info("Re-applying prototype '{}' that does not have container components. Container items " +
                "in the previous page definition will be deleted.", newPrototypePage.getPath());
        if (log.isInfoEnabled()) {
            List<Node> existingContainers = findContainers(oldPage);
            for (Node existingContainer : existingContainers) {
                for (Node containerItem : new NodeIterable(existingContainer.getNodes())) {
                    log.info("Container item '{}' will be removed since the new prototype does not " +
                            "have any containers.", containerItem.getPath());
                }
            }
        }
    }

    /**
     * de-normalizes referenced workspace containers that are *part* of the canonical <code>newPage</code> node structure: Inherited
     * references (for example hst:abstractpages/base/footer) do not need to be de-normalized
     */
    private void denormalizeContainerComponentReferences(final Node newPage,
                                                         final HstComponentConfiguration pageInstance) throws RepositoryException {

        final List<Node> containerComponentReferenceNodes = new ArrayList<>();
        populateComponentReferenceNodes(newPage, containerComponentReferenceNodes);

        if (containerComponentReferenceNodes.size() > 0) {
            final String pagePath = newPage.getPath();
            // start denormalization
            for (Node containerComponentReferenceNode : containerComponentReferenceNodes) {
                // at the location of containerComponentReferenceNode, in the pageConfig the HST has
                // used the referenced node: Hence, we can use the canonical identifier from that to
                // denormalize. The canonical identifier can even belong to inherited configurations
                final String absPath = containerComponentReferenceNode.getPath();
                final String relPath = absPath.substring(pagePath.length() + 1);
                containerComponentReferenceNode.remove();
                String[] elems = relPath.split("/");
                HstComponentConfiguration current = pageInstance;
                for (String elem : elems) {
                    if (current == null) {
                        break;
                    }
                    current = current.getChildByName(elem);
                }
                if (current == null) {
                    log.warn("Could not find hst component configuration for component reference node '{}', hence we " +
                            "cannot denormalize the reference. Instead, replace the reference with a empty container node.", absPath);
                    Node container = newPage.addNode(relPath, NODETYPE_HST_CONTAINERCOMPONENT);
                    container.setProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE, "HST.vBox");
                } else {
                    // current now contains the component that we need to denormalize
                    JcrUtils.copy(newPage.getSession(), current.getCanonicalStoredLocation(), absPath);
                    log.info("Succesfully denormalized '{}'", absPath);
                }
            }
        }
    }

    private void populateComponentReferenceNodes(final Node node,
                                                 final List<Node> containerComponentReferenceNodes) throws RepositoryException {
        if (node.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTREFERENCE)) {
            containerComponentReferenceNodes.add(node);
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            populateComponentReferenceNodes(child, containerComponentReferenceNodes);
        }
    }

    public void delete(final Node sitemapItemNodeToDelete) throws RepositoryException {
        for (Node child : new NodeIterable(sitemapItemNodeToDelete.getNodes())) {
            delete(child);
        }
        final String componentConfigId = getStringProperty(sitemapItemNodeToDelete, SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, null);
        if (componentConfigId == null) {
            log.debug("No component id configured for '{}'. No page to delete.", sitemapItemNodeToDelete.getPath());
            return;
        }
        final String pageNodePath = getPreviewWorkspacePath() + "/" + componentConfigId;
        final Session session = pageComposerContextService.getRequestContext().getSession();
        if (!session.nodeExists(pageNodePath)) {
            log.info("No page found in hst:workspace for '{}' which is referenced by sitemap item '{}'. Skip deleting the page",
                    pageNodePath, sitemapItemNodeToDelete.getPath());
            return;
        }
        Node pageNode = session.getNode(pageNodePath);
        lockHelper.acquireLock(pageNode, 0);
        deletePageNodeIfNotReferencedAnyMore(pageNode);
    }

    private void deletePageNodeIfNotReferencedAnyMore(final Node pageNode) throws RepositoryException {
        String componentConfigurationIdForPage = NODENAME_HST_PAGES + "/" + pageNode.getName();
        final Session session = pageNode.getSession();
        Node workspaceSiteMapNode = session.getNode(getPreviewWorkspacePath()).getNode(HstNodeTypes.NODENAME_HST_SITEMAP);
        Node nonWorkspaceSiteMapNode = null;
        final String nonWorkspaceSiteMapPath = pageComposerContextService.getEditingPreviewSite().getConfigurationPath()
                + "/" + HstNodeTypes.NODENAME_HST_SITEMAP;
        if (session.nodeExists(nonWorkspaceSiteMapPath)) {
            nonWorkspaceSiteMapNode = session.getNode(nonWorkspaceSiteMapPath);
        }

        final int inUseCounter = countIdReferencedByNumberOfSiteMapItems(workspaceSiteMapNode, nonWorkspaceSiteMapNode, componentConfigurationIdForPage);
        if (inUseCounter < 2) {
            deleteOrMarkDeletedIfLiveExists(pageNode);
        } else {
            log.info("Cannot delete page as it is in use by {} sitemap items.", inUseCounter);
        }
    }

    private int countIdReferencedByNumberOfSiteMapItems(final Node workspaceSiteMapNode,
                                                        final Node nonWorkspaceSiteMapNode,
                                                        final String componentConfigurationIdForPage) throws RepositoryException {
        int refCounter = countReferences(new NodeIterable(workspaceSiteMapNode.getNodes()), componentConfigurationIdForPage);
        if (nonWorkspaceSiteMapNode != null) {
            refCounter += countReferences(new NodeIterable(nonWorkspaceSiteMapNode.getNodes()), componentConfigurationIdForPage);
        }
        return refCounter;
    }

    private int countReferences(final NodeIterable nodes, final String componentConfigurationIdForPage) throws RepositoryException {
        int refCounter = 0;
        for (Node node : nodes) {
            final String compId = JcrUtils.getStringProperty(node, HstNodeTypes.SITEMAPITEM_PROPERTY_COMPONENTCONFIGURATIONID, null);
            if (componentConfigurationIdForPage.equals(compId)) {
                refCounter++;
            }
            refCounter += countReferences(new NodeIterable(node.getNodes()), componentConfigurationIdForPage);
        }
        return refCounter;
    }

    private String getValidTargetPageNodeName(final String previewWorkspacePagesPath, final String targetPageNodeName, final Session session) throws RepositoryException {
        String testTargetNodeName = targetPageNodeName;
        for (int counter = 1; !isValidTarget(session, testTargetNodeName, previewWorkspacePagesPath, getLivePagesPath()); counter++) {
            log.info("targetPageNodeName '{}' not valid. Trying next one.", targetPageNodeName);
            testTargetNodeName = targetPageNodeName + "-" + counter;
        }
        return testTargetNodeName;
    }

    private boolean isValidTarget(final Session session,
                                  final String testTargetNodeName,
                                  final String previewWorkspacePagesPath,
                                  final String livePagesPath) throws RepositoryException {
        final String testWorkspaceTargetNodePath = previewWorkspacePagesPath + "/" + testTargetNodeName;
        if (session.nodeExists(testWorkspaceTargetNodePath)) {
            Node targetNode = session.getNode(testWorkspaceTargetNodePath);
            if (isMarkedDeleted(targetNode)) {
                // see if we own the lock
                try {
                    lockHelper.acquireLock(targetNode, 0);
                } catch (ClientException e) {
                    return false;
                }
                targetNode.remove();
            } else {
                return false;
            }
        }
        // the targetNodeName does not yet exist in workspace pages. Confirm it does not exist non workspace pages
        return !session.nodeExists(livePagesPath + "/" + testTargetNodeName);
    }

    private String getPreviewWorkspacePagesPath() {
        return getPreviewWorkspacePath() + "/" + NODENAME_HST_PAGES;
    }

    private String getLivePagesPath() {
        String liveConfigurationPath = pageComposerContextService.getEditingPreviewSite().getConfigurationPath();
        if (liveConfigurationPath.endsWith("-preview")) {
            liveConfigurationPath = StringUtils.substringBeforeLast(liveConfigurationPath, "-preview");
        }
        return liveConfigurationPath
                + "/" + NODENAME_HST_PAGES;
    }

}
