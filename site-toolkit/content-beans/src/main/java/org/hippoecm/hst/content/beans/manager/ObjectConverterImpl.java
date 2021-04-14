/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.manager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.hippoecm.hst.campaign.DocumentCampaignService;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.NodeAware;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.version.HippoBeanFrozenNode;
import org.hippoecm.hst.content.beans.version.HippoBeanFrozenNodeUtils;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.service.ServiceFactory;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.campaign.Campaign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.collections4.MapUtils.isNotEmpty;
import static org.apache.jackrabbit.JcrConstants.JCR_VERSIONLABELS;
import static org.hippoecm.hst.core.container.ContainerConstants.BR_VERSION_UUID_REQUEST_PARAMETER;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_VERSION_HISTORY_PROPERTY;
import static org.hippoecm.repository.api.HippoNodeType.NT_COMPOUND;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;
import static org.hippoecm.repository.api.HippoNodeType.NT_HIPPO_VERSION_INFO;
import static org.hippoecm.repository.util.JcrUtils.getStringProperty;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;
import static org.onehippo.repository.util.JcrConstants.JCR_FROZEN_NODE;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;
import static org.onehippo.repository.util.JcrConstants.NT_VERSION_HISTORY;

public class ObjectConverterImpl implements ObjectConverter {

    private static final Logger log = LoggerFactory.getLogger(ObjectConverterImpl.class);

    final Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeBeanPairs = new ConcurrentHashMap<>();
    final Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeCompileTimeBeanPairs;
    private List<String> fallBackJcrNodeTypes;

    public ObjectConverterImpl(final Map<String, Class<? extends HippoBean>> jcrPrimaryNodeTypeBeanPairs, final String[] fallBackJcrNodeTypes) {

        if (isNotEmpty(jcrPrimaryNodeTypeBeanPairs)) {
            this.jcrPrimaryNodeTypeBeanPairs.putAll(jcrPrimaryNodeTypeBeanPairs);
        }

        jcrPrimaryNodeTypeCompileTimeBeanPairs = Collections.unmodifiableMap(jcrPrimaryNodeTypeBeanPairs);

        if (fallBackJcrNodeTypes != null) {
            this.fallBackJcrNodeTypes = Arrays.asList(fallBackJcrNodeTypes);
        }
    }

    /**
     * Add bean class definition and its underlying node type to cache
     *
     * @param documentType name of the document type
     * @param beanClass of {@link HippoBean}
     */
    protected void addBeanDefinition(@Nonnull final String documentType, @Nonnull final Class<? extends HippoBean> beanClass) {
        jcrPrimaryNodeTypeBeanPairs.put(documentType, beanClass);
    }

    public Object getObject(Session session, String path) throws ObjectBeanManagerException {
        if (StringUtils.isEmpty(path) || !path.startsWith("/")) {
            log.warn("Illegal argument for '{}' : not an absolute path", path);
            return null;
        }
        final String relPath = path.substring(1);
        try {
            return getObject(session.getRootNode(), relPath);
        } catch (RepositoryException re) {
            throw new ObjectBeanManagerException("Impossible to get the object at " + path, re);
        }

    }

    public Object getObject(Node node, String relPath) throws ObjectBeanManagerException {
        if (StringUtils.isEmpty(relPath) || relPath.startsWith("/")) {
            log.info("'{}' is not a valid relative path. Return null.", relPath);
            return null;
        }
        if (node == null) {
            log.info("Node is null. Cannot get document with relative path '{}'", relPath);
            return null;
        }
        String nodePath = null;
        try {
            if (node instanceof HippoBeanFrozenNode) {
                nodePath = ((HippoBeanFrozenNode) node).getFrozenNode().getPath();
            } else {
                nodePath = node.getPath();
            }
            final Node relNode = JcrUtils.getNodeIfExists(node, relPath);
            if (relNode == null) {
                log.info("Cannot get object for node '{}' with relPath '{}'", nodePath, relPath);
                return null;
            }
            if (relNode.isNodeType(NT_HANDLE)) {
                // if its a handle, we want the child node. If the child node is not present,
                // this node can be ignored
                final Node document = JcrUtils.getNodeIfExists(relNode, relNode.getName());
                if (document == null) {
                    log.info("Cannot get object for node '{}' with relPath '{}'", nodePath, relPath);
                    return null;
                } else {
                    return getObject(document);
                }
            } else {
                return getObject(relNode);
            }

        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.info("Cannot get object for node '{}' with relPath '{}'", nodePath, relPath, e);
            } else {
                log.info("Cannot get object for node '{}' with relPath '{}'", nodePath, relPath);
            }
            return null;
        }
    }

    public Object getObject(String uuid, Session session) throws ObjectBeanManagerException {
        checkUUID(uuid);
        try {
            Node node = session.getNodeByIdentifier(uuid);
            return this.getObject(node);
        } catch (ItemNotFoundException e) {
            log.info("ItemNotFoundException for uuid '{}'. Return null.", uuid);
        } catch (RepositoryException e) {
            log.info("RepositoryException for uuid '{}' : {}. Return null.", uuid, e);
        }
        return null;
    }

    public Object getObject(String uuid, Node node) throws ObjectBeanManagerException {
        try {
            return this.getObject(uuid, node.getSession());
        } catch (RepositoryException e) {
            log.info("Failed to get object for uuid '{}'. Return null.", uuid, e);
        }
        return null;
    }

    public Object getObject(final Node node) throws ObjectBeanManagerException {
        final String jcrPrimaryNodeType;
        final String path;
        try {

            final Node useNode = getActualNode(node);

            if (useNode.isSame(useNode.getSession().getRootNode()) && getAnnotatedClassFor("rep:root") == null) {
                log.debug("Root useNode is not mapped to be resolved to a bean.");
                return null;
            }

            if (useNode.isNodeType(NT_HANDLE)) {
                if (useNode.hasNode(useNode.getName())) {
                    return getObject(useNode.getNode(useNode.getName()));
                } else {
                    return null;
                }
            }
            jcrPrimaryNodeType = useNode.getPrimaryNodeType().getName();
            Class<? extends HippoBean> delegateeClass = this.jcrPrimaryNodeTypeBeanPairs.get(jcrPrimaryNodeType);

            if (delegateeClass == null) {
                if (jcrPrimaryNodeType.equals("hippotranslation:translations")) {
                    log.info("Encountered node of type 'hippotranslation:translations' : This nodetype is completely deprecated and should be " +
                            "removed from all content including from prototypes.");
                    return null;
                }
                // no exact match, try a fallback type
                delegateeClass = getFallbackClass(jcrPrimaryNodeType, useNode);
            }

            if (delegateeClass != null) {
                return instantiateObject(delegateeClass, useNode);
            }
            path = useNode.getPath();
        } catch (RepositoryException e) {
            throw new ObjectBeanManagerException("Impossible to get the object from the repository", e);
        } catch (Exception e) {
            throw new ObjectBeanManagerException("Impossible to convert the useNode", e);
        }
        log.info("No Descriptor found for useNode '{}'. Cannot return a Bean for '{}'.", path, jcrPrimaryNodeType);
        return null;
    }

    /**
     * Instantiate given class, and if applicable attach node & converter instance to it.
     * @param clazz HippoBean class to instantiate
     * @param node Underlying JCR node
     * @return Object instance
     * @throws Exception
     */
    Object instantiateObject(final Class<? extends HippoBean> clazz, final Node node) throws Exception {
        Object object = ServiceFactory.create(clazz);
        if (object != null) {
            if (object instanceof NodeAware) {
                ((NodeAware) object).setNode(node);
            }
            if (object instanceof ObjectConverterAware) {
                ((ObjectConverterAware) object).setObjectConverter(this);
            }
        }
        return object;
    }

    Class<? extends HippoBean> getFallbackClass(final String jcrPrimaryNodeType, final Node node) throws RepositoryException {

        Class<? extends HippoBean> clazz = null;

        for (final String fallBackJcrPrimaryNodeType : this.fallBackJcrNodeTypes) {

            if (!node.isNodeType(fallBackJcrPrimaryNodeType)) {
                continue;
            }

            // take the first fallback type
            clazz = this.jcrPrimaryNodeTypeBeanPairs.get(fallBackJcrPrimaryNodeType);
            if (clazz != null) {
                log.debug("No bean found for {}, using fallback class  {} instead", jcrPrimaryNodeType, clazz);
                break;
            }
        }
        return clazz;
    }

    boolean isDocumentType(final Node node) throws RepositoryException {
        return node.isNodeType(NT_DOCUMENT) && node.getParent().isNodeType(NT_HANDLE);
    }

    boolean isCompoundType(final Node node) throws RepositoryException {
        return node.isNodeType(NT_COMPOUND);
    }

    Node getActualNode(final Node node) throws RepositoryException {
        if (node instanceof HippoBeanFrozenNode) {
            return node;
        }

        if (node.isNodeType(NT_FROZEN_NODE)) {
            log.error("Unexpected {} node for path {} since we always expect a decorated node of type '{}'.", NT_FROZEN_NODE,
                    node.getPath(), HippoBeanFrozenNode.class.getName());
            return HippoBeanFrozenNodeUtils.getWorkspaceFrozenNode(node, node.getPath(), node.getName());
        }

        final Node canonicalNode;
        if ((node instanceof HippoNode) && ((HippoNode) node).isVirtual()) {
            final Node canonical = ((HippoNode) node).getCanonicalNode();
            if (canonical == null) {
                // virtual only, there is never a versioned node for it
                return node;
            }
            if (!(canonical.isNodeType(NT_DOCUMENT) && canonical.getParent().isNodeType(NT_HANDLE))) {
                // not a document
                return node;
            }
            canonicalNode = canonical;
        } else {
            canonicalNode = node;
        }

        final Node handle  = canonicalNode.getParent();

        if (!handle.isNodeType(NT_HANDLE)) {
            // node is not a variant below the handle
            return node;
        }

        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext == null) {
            return node;
        }

        final String branchId = HstRequestUtils.getBranchIdFromContext(requestContext);

        final String renderVersionId = HstRequestUtils.getRenderFrozenNodeId(requestContext, canonicalNode, branchId);
        final Session session = node.getSession();
        if (renderVersionId != null) {

            final Node renderVersion = session.getNodeByIdentifier(renderVersionId);
            if (!renderVersion.isNodeType(JcrConstants.NT_FROZENNODE)) {
                log.info("Explicit query param '{}={}' points to workspace jcr node and not a versioned node, just " +
                        "render the workspace node", BR_VERSION_UUID_REQUEST_PARAMETER, renderVersionId);
                return renderVersion;
            }
            return HippoBeanFrozenNodeUtils.getWorkspaceFrozenNode(renderVersion,
                    canonicalNode.getPath(), canonicalNode.getName());
        }

        if (!handle.isNodeType(NT_HIPPO_VERSION_INFO)) {
            // no version history information on handle, return
            return node;
        }

        try {
            final DocumentCampaignService documentCampaignService;
            if (!requestContext.isChannelManagerPreviewRequest()
                    && (documentCampaignService = HippoServiceRegistry.getService(DocumentCampaignService.class)) != null) {
                // only for live website we potentially serve a campaign version of a document based on whether it
                // has a matching date range in which the campaign version should be rendered
                // Note although initially the UI only supports active campaigns for the MASTER branch, the BE is
                // agnostic about this, and works for any branchId : since when running a project campaign, there for
                // now won't be a 'document campaign', in general when a project campaign runs, never an 'activeCampaign'
                // for a document will be found (since never set). For performance reasons we could decide to skip this
                // test when running a project campaign, but the lookup is fairly inexpensive so just always run it
                final Optional<Campaign> activeCampaign = documentCampaignService.findActiveCampaign(handle, branchId);

                if (activeCampaign.isPresent()) {
                    log.info("Found frozenNode uuid '{}' to render for '{}'", activeCampaign.get().getUuid(), handle.getPath());
                    try {
                        return HippoBeanFrozenNodeUtils.getWorkspaceFrozenNode(session.getNodeByIdentifier(activeCampaign.get().getUuid()), canonicalNode.getPath(), canonicalNode.getName());
                    } catch (RepositoryException e) {
                        // in this case, just continue the code below to see whether there is a branch to be rendered
                        // instead. This scenario could happen because of truncated version history in JCR or because
                        // of invalid campaign data on the handle
                        log.info("Failed to return a frozen node version for active campaign '{}' for handle '{}'. Fallback to serve the " +
                                "right branch.", activeCampaign.get().getUuid(), handle.getPath());
                    }
                }
            }

            final String branchIdOfNode = getStringProperty(node, HippoNodeType.HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);
            if (branchIdOfNode.equals(branchId)) {
                return node;
            }

            // should we serve a versioned history node or just workspace.


            if (!handle.hasProperty(HIPPO_VERSION_HISTORY_PROPERTY)) {
                // Without a version history identifier we can't find it in version history
                return node;
            }

            final Node versionHistory = session.getNodeByIdentifier(handle.getProperty(HIPPO_VERSION_HISTORY_PROPERTY).getString());
            if (!versionHistory.isNodeType(NT_VERSION_HISTORY)) {
                log.warn("'{}/@{}' does not point to a node of type '{}' which is not allowed. Correct the handle manually.",
                        handle.getPath(), HIPPO_VERSION_HISTORY_PROPERTY, NT_VERSION_HISTORY);
                return node;
            }

            final boolean preview = requestContext.getResolvedMount().getMount().isPreview();
            Optional<Node> version = getVersionForLabel(versionHistory, branchId, preview);
            if (!version.isPresent() || !version.get().hasNode(JCR_FROZEN_NODE)) {
                // lookup master revision in absence of a branch version
                if (branchIdOfNode.equals(MASTER_BRANCH_ID)) {
                    // current node is for master, thus return current one
                    return node;
                }
                version = getVersionForLabel(versionHistory, MASTER_BRANCH_ID, preview);
            }
            if (!version.isPresent() || !version.get().hasNode(JCR_FROZEN_NODE)) {
                // return current (published or unpublished) in absence of a branch and master version
                return node;
            }

            log.info("Found version '{}' to use for rendering.", version.get().getPath());

            final Node frozenNode = version.get().getNode(JCR_FROZEN_NODE);
            // we can only decorate a frozen node to the canonical location
            return HippoBeanFrozenNodeUtils.getWorkspaceFrozenNode(frozenNode, canonicalNode.getPath(), canonicalNode.getName());

        } catch (ItemNotFoundException e) {
            log.warn("Version history node with id stored on '{}/@{}' does not exist. Correct the handle manually.",
                    handle.getPath(), HIPPO_VERSION_HISTORY_PROPERTY);
            return node;
        } catch (RepositoryException e) {
            log.warn("Failed to get frozen node from version history for handle '{}', returning '{}' instead.",
                    handle.getPath(), node.getPath(), e);
            return node;
        }

    }

    private Optional<Node> getVersionForLabel(final Node versionHistory, final String branchId, final boolean preview) throws RepositoryException {
        final String versionLabel;
        if (preview) {
            versionLabel = branchId + "-" + HippoStdNodeType.UNPUBLISHED;
        } else {
            versionLabel = branchId + "-" + HippoStdNodeType.PUBLISHED;
        }
        if (versionHistory.hasProperty(JCR_VERSIONLABELS + "/" + versionLabel)) {
            return Optional.of(versionHistory.getProperty(JCR_VERSIONLABELS + "/" + versionLabel).getNode());
        }
        return Optional.empty();
    }

    public String getPrimaryObjectType(final Node node) throws ObjectBeanManagerException {
        String jcrPrimaryNodeType;
        String path;
        try {

            if (node.isNodeType(NT_HANDLE)) {
                if (node.hasNode(node.getName())) {
                    return getPrimaryObjectType(node.getNode(node.getName()));
                } else {
                    return null;
                }
            }
            jcrPrimaryNodeType = node.getPrimaryNodeType().getName();
            boolean isObjectType = jcrPrimaryNodeTypeBeanPairs.containsKey(jcrPrimaryNodeType);

            if (!isObjectType) {
                if (jcrPrimaryNodeType.equals("hippotranslation:translations")) {
                    log.info("Encountered node of type 'hippotranslation:translations' : This nodetype is completely deprecated and should be " +
                            "removed from all content including from prototypes.");
                    return null;
                }
                // no exact match, try a fallback type
                for (String fallBackJcrPrimaryNodeType : this.fallBackJcrNodeTypes) {

                    if (!node.isNodeType(fallBackJcrPrimaryNodeType)) {
                        continue;
                    }
                    // take the first fallback type
                    isObjectType = jcrPrimaryNodeTypeBeanPairs.containsKey(fallBackJcrPrimaryNodeType);
                    if (isObjectType) {
                        log.debug("No primary node type found for {}, using fallback type {} instead", jcrPrimaryNodeType, fallBackJcrPrimaryNodeType);
                        jcrPrimaryNodeType = fallBackJcrPrimaryNodeType;
                        break;
                    }
                }
            }

            if (isObjectType) {
                return jcrPrimaryNodeType;
            }
            path = node.getPath();
        } catch (RepositoryException e) {
            throw new ObjectBeanManagerException("Impossible to get the node from the repository", e);
        } catch (Exception e) {
            throw new ObjectBeanManagerException("Impossible to determine node type for node", e);
        }
        log.info("No Descriptor found for node '{}'. Cannot return a matching node type for '{}'.", path, jcrPrimaryNodeType);
        return null;
    }

    private void checkUUID(final String uuid) throws ObjectBeanManagerException {
        try {
            UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new ObjectBeanManagerException("Uuid is not parseable to a valid uuid: '" + uuid + "'");
        }
    }

    public Class<? extends HippoBean> getAnnotatedClassFor(final String jcrPrimaryNodeType) {
        return this.jcrPrimaryNodeTypeBeanPairs.get(jcrPrimaryNodeType);
    }

    public String getPrimaryNodeTypeNameFor(final Class<? extends HippoBean> hippoBean) {
        // both check jcrPrimaryNodeTypeCompileTimeBeanPairs as well as the potentially runtime replaced classes in
        // jcrPrimaryNodeTypeBeanPairs
        return jcrPrimaryNodeTypeCompileTimeBeanPairs.entrySet().stream().filter(e -> e.getValue() == hippoBean)
                .findFirst().map(Map.Entry::getKey)
                .orElse(jcrPrimaryNodeTypeBeanPairs.entrySet().stream().filter(e -> e.getValue() == hippoBean)
                        .findFirst().map(Map.Entry::getKey).orElse(null));
    }
}
