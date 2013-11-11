/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.DocumentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtIdsRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PageModelRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ToolkitRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.UserRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/hst:mount/")
public class MountResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(MountResource.class);

    @GET
    @Path("/pagemodel/{pageId}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPageModelRepresentation(@Context HttpServletRequest servletRequest,
                                               @PathParam("pageId") String pageId) {
        try { 
            final HstRequestContext requestContext = getRequestContext(servletRequest);
            final HstSite editingPreviewHstSite = getEditingPreviewSite(requestContext);
            if (editingPreviewHstSite == null) {
                log.error("Could not get the editing site to create the page model representation.");
                return error("Could not get the editing site to create the page model representation.");
            }
            final PageModelRepresentation pageModelRepresentation = new PageModelRepresentation().represent(editingPreviewHstSite, pageId, getEditingPreviewMount(requestContext));
            log.info("PageModel loaded successfully");
            return ok("PageModel loaded successfully", pageModelRepresentation.getComponents().toArray());
        } catch (Exception e) {
            log.warn("Failed to retrieve page model.", e);
            return error("Failed to retrieve page model: " + e.toString());
        }
    }
    
    @GET
    @Path("/toolkit/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getToolkitRepresentation(@Context HttpServletRequest servletRequest) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        final Mount editingMount = getEditingPreviewMount(requestContext);
        if (editingMount == null) {
            log.error("Could not get the editing site to create the toolkit representation.");
            return error("Could not get the editing site to create the toolkit representation.");
        }

        setCurrentMountCanonicalContentPath(servletRequest, editingMount.getCanonicalContentPath());

        ToolkitRepresentation toolkitRepresentation = new ToolkitRepresentation().represent(editingMount);
        log.info("Toolkit items loaded successfully");
        return ok("Toolkit items loaded successfully", toolkitRepresentation.getComponents().toArray());
    }

    @GET
    @Path("/userswithchanges/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersWithChanges(@Context HttpServletRequest servletRequest) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        try {
            HippoSession session = HstConfigurationUtils.getNonProxiedSession(requestContext.getSession(false));
            String previewConfigurationPath = getEditingPreviewMount(requestContext).getHstSite().getConfigurationPath();

            Set<String> usersWithLockedMainConfigNode = findUsersWithLockedMainConfigNodes(session, previewConfigurationPath);
            Set<String> usersWithLockedContainers = findUsersWithLockedContainers(session, previewConfigurationPath);
            Set<String> usersWithLocks = new HashSet<String>();
            usersWithLocks.addAll(usersWithLockedMainConfigNode);
            usersWithLocks.addAll(usersWithLockedContainers);
            List<UserRepresentation> usersWithChanges = new ArrayList<UserRepresentation>(usersWithLocks.size());
            for (String userId : usersWithLocks) {
                usersWithChanges.add(new UserRepresentation(userId));
            }
            log.info("Found " + usersWithChanges.size() + " users with changes");
            return ok("Found " + usersWithChanges.size() + " users with changes", usersWithChanges);
        } catch (LoginException e) {
            log.warn("Could not get a JCR session. Cannot retrieve users with changes.", e);
            return error("Could not get a JCR session: " + e + ". Cannot retrieve users with changes.");
        } catch (RepositoryException e) {
            log.warn("Could not retrieve users with changes: ", e);
            return error("Could not retrieve users with changes: " + e);
        }
    }

    Set<String> findUsersWithLockedMainConfigNodes(final HippoSession session, String previewConfigurationPath) throws RepositoryException {
        final String xpath = buildXPathQueryToFindLockedMainConfigNodesForUsers(previewConfigurationPath);
        return collectFromQueryUsersForLockedBy(session, xpath);
    }

    Set<String> findUsersWithLockedContainers(final HippoSession session, String previewConfigurationPath) throws RepositoryException {
        final String xpath = buildXPathQueryToFindLockedContainersForUsers(previewConfigurationPath);
        return collectFromQueryUsersForLockedBy(session, xpath);
    }

    private String buildXPathQueryToFindLockedMainConfigNodesForUsers(String previewConfigurationPath) {
        return "/jcr:root" + previewConfigurationPath + "/*[@" + HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY + " != '']";
    }

    private String buildXPathQueryToFindLockedContainersForUsers(String previewConfigurationPath) {
        return "/jcr:root" + previewConfigurationPath + "//element(*," + HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT + ")"
                + "[@" + HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY + " != '']";
    }

    private Set<String> collectFromQueryUsersForLockedBy(final HippoSession session, final String xpath) throws RepositoryException {
        final QueryResult result = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH).execute();
        final NodeIterable lockedContainers = new NodeIterable(result.getNodes());
        Set<String> userIds = new HashSet<String>();
        for (Node lockedContainer : lockedContainers) {
            String userId = JcrUtils.getStringProperty(lockedContainer, HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, null);
            if (userId != null) {
                userIds.add(userId);
            }
        }
        log.info("For query '{}' collected '{}' users that have a lock", xpath, userIds.toString());
        return userIds;
    }

    /**
     * If the {@link Mount} that this request belongs to does not have a preview configuration, it will 
     * be created. If it already has a preview configuration, just an ok {@link Response} is returned.
     * @param servletRequest
     * @return ok {@link Response} when editing can start, and error {@link Response} otherwise
     */
    @POST
    @Path("/edit/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startEdit(@Context HttpServletRequest servletRequest) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);

        try {
            final MutableMount editingPreviewMount = (MutableMount)getEditingPreviewMount(requestContext);
            final HstSite ctxEditingPreviewSite = editingPreviewMount.getHstSite();
            
            Session session = requestContext.getSession();
            
            if (ctxEditingPreviewSite.hasPreviewConfiguration()) {
                return ok("Site can be edited now");
            }

            createPreviewConfigurationNode(requestContext);
            HstConfigurationUtils.persistChanges(session);
            log.info("Site '{}' can be edited now", ctxEditingPreviewSite.getConfigurationPath());
            return ok("Site can be edited now");
        } catch (IllegalStateException e) {
            log.warn("Cannot start editing : ", e);
            return error("Cannot start editing : " + e);
        } catch (LoginException e) {
            log.warn("Could not get a jcr session. Cannot create a  preview configuration.", e);
            return error("Could not get a jcr session : " + e + ". Cannot create a  preview configuration.");
        } catch (RepositoryException e) {
            log.warn("Could not create a preview configuration : ", e);
            return error("Could not create a preview configuration : " + e);
        }
    }

    private void createPreviewConfigurationNode(final HstRequestContext requestContext) throws RepositoryException {
        HstSite ctxEditingLiveMountSite = getEditingLiveMount(requestContext).getHstSite();
        String liveConfigurationPath = ctxEditingLiveMountSite.getConfigurationPath();
        String previewConfigurationPath = liveConfigurationPath + "-preview";
        Session session = requestContext.getSession();
        JcrUtils.copy(session, liveConfigurationPath, previewConfigurationPath);
    }

    /**
     * If the {@link Mount} that this request belongs to has a preview configuration, it will be discarded.
     * @param servletRequest
     * @return ok {@link Response} when the discard completed, error {@link Response} otherwise
     */
    @POST
    @Path("/discard/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response discardChanges(@Context HttpServletRequest servletRequest) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        return discardChangesOfCurrentUser(requestContext);
    }

    @POST
    @Path("/userswithchanges/discard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response discardChangesOfUsers(@Context HttpServletRequest servletRequest, ExtIdsRepresentation ids) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        final MutableMount editingPreviewMount = (MutableMount)getEditingPreviewMount(requestContext);
        if (!hasPreviewConfiguration(editingPreviewMount)) {
            log.warn("Cannot discard changes of users in a non-preview site");
            return error("Cannot discard changes of users in a non-preview site");
        }
        return discardChanges(requestContext, ids.getData());
    }

    /**
     * If the {@link Mount} that this request belongs to does not have a preview configuration, it will 
     * be created. If it already has a preview configuration, just an ok {@link Response} is returned.
     * @param servletRequest
     * @return ok {@link Response} when editing can start, and error {@link Response} otherwise
     */
    @POST
    @Path("/publish/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response publish(@Context HttpServletRequest servletRequest) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        final MutableMount editingPreviewMount = (MutableMount)getEditingPreviewMount(requestContext);

        if (!hasPreviewConfiguration(editingPreviewMount)) {
            log.warn("Cannot publish non preview site");
            return error("Cannot publish non preview site");
        }
        return publishChangesOfCurrentUser(requestContext);
    }

    @POST
    @Path("/userswithchanges/publish")
    @Produces(MediaType.APPLICATION_JSON)
    public Response publishChangesOfUsers(@Context HttpServletRequest servletRequest, ExtIdsRepresentation ids) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        final MutableMount editingPreviewMount = (MutableMount)getEditingPreviewMount(requestContext);

        if (!hasPreviewConfiguration(editingPreviewMount)) {
            log.warn("Cannot publish non preview site");
            return error("Cannot publish non preview site");
        }
        return publishChangesOfUsers(requestContext, ids.getData());
    }

    private Response publishChangesOfCurrentUser(final HstRequestContext requestContext) {
        try {
            HippoSession session = HstConfigurationUtils.getNonProxiedSession(requestContext.getSession(false));
            String currentUserId = session.getUserID();
            return publishChangesOfUsers(requestContext, Collections.singletonList(currentUserId));
        } catch (RepositoryException e) {
            log.warn("Could not publish preview configuration of the current user : ", e);
            return error("Could not publish preview configuration of the current user: " + e);
        }
    }

    private Response publishChangesOfUsers(final HstRequestContext requestContext, List<String> userIds) {
        try {
            String liveConfigurationPath = getEditingLiveMount(requestContext).getHstSite().getConfigurationPath();
            String previewConfigurationPath = getEditingPreviewMount(requestContext).getHstSite().getConfigurationPath();

            HippoSession session = HstConfigurationUtils.getNonProxiedSession(requestContext.getSession(false));
            List<String> relativeContainerPathsToPublish = findChangedContainersForUsers(session, previewConfigurationPath, userIds);

            List<String> mainConfigNodeNamesToPublish = findChangedMainConfigNodeNamesForUsers(session, previewConfigurationPath, userIds);
            pushContainerChildrenNodes(session, previewConfigurationPath, liveConfigurationPath, relativeContainerPathsToPublish);
            copyChangedMainConfigNodes(session, previewConfigurationPath, liveConfigurationPath, mainConfigNodeNamesToPublish);

            HstConfigurationUtils.persistChanges(session);
            return ok("Site is published");
        } catch (RepositoryException e) {
            log.warn("Could not publish preview configuration : ", e);
            return error("Could not publish preview configuration : " + e);
        }
    }

    /**
     * Creates a document in the repository using the WorkFlowManager
     * The post parameters should contain the 'path', 'docType' and 'name' of the document.
     * @param servletRequest Servlet Request
     * @param params The POST parameters
     * @return response JSON with the status of the result
     */
    @POST
    @Path("/create/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDocument(@Context HttpServletRequest servletRequest,
                                   MultivaluedMap<String, String> params) {

        final HstRequestContext requestContext = getRequestContext(servletRequest);
        try {
            final Mount editingPreviewMount = getEditingPreviewMount(requestContext);
            if (editingPreviewMount == null) {
                log.warn("Could not get the editing mount to get the content path for creating the document.");
                return error("Could not get the editing mount to get the content path for creating the document.");
            }
            String canonicalContentPath = editingPreviewMount.getCanonicalContentPath();
            WorkflowPersistenceManagerImpl workflowPersistenceManager = new WorkflowPersistenceManagerImpl(requestContext.getSession(),
                    getObjectConverter(requestContext));
            workflowPersistenceManager.createAndReturn(canonicalContentPath + "/" + params.getFirst("docLocation"), params.getFirst("docType"), params.getFirst("docName"), true);
        } catch (RepositoryException e) {
            log.warn("Exception happened while trying to create the document " + e, e);
            return error("Exception happened while trying to create the document " + e);
        } catch (ObjectBeanPersistenceException e) {
            log.warn("Exception happened while trying to create the document " + e, e);
            return error("Exception happened while trying to create the document " + e);
        }
        return ok("Successfully created a document", null);
    }

    /**
     * Method that returns a {@link Response} containing the list of document of (sub)type <code>docType</code> that
     * belong to the content of the site that is currently composed.
     * @param servletRequest
     * @param docType         the docType the found documents must be of. The documents can also be a subType of
     *                        docType
     * @return An ok Response containing the list of documents or an error response in case an exception occurred
     */
    @POST
    @Path("/documents/{docType}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocumentsByType(@Context HttpServletRequest servletRequest,
                                       @PathParam("docType") String docType) {

        final HstRequestContext requestContext = getRequestContext(servletRequest);
        final Mount editingHstMount = getEditingPreviewMount(requestContext);
        if (editingHstMount == null) {
            log.warn("Could not get the editing mount to get the content path for listing documents.");
            return error("Could not get the editing mount to get the content path for listing documents.");
        }
        List<DocumentRepresentation> documentLocations = new ArrayList<DocumentRepresentation>();
        String canonicalContentPath = editingHstMount.getCanonicalContentPath();
        try {
            Session session = requestContext.getSession();

            Node contentRoot = (Node) session.getItem(canonicalContentPath);

            String statement = "//element(*," + docType + ")[@hippo:paths = '" + contentRoot.getIdentifier() + "' and @hippo:availability = 'preview' and not(@jcr:primaryType='nt:frozenNode')]";
            QueryManager queryMngr = session.getWorkspace().getQueryManager();
            QueryResult result = queryMngr.createQuery(statement, "xpath").execute();
            NodeIterator documents = result.getNodes();
            while (documents.hasNext()) {
                Node doc = documents.nextNode();
                if (doc == null) {
                    continue;
                }
                if (doc.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                    // take the handle
                    doc = doc.getParent();
                }
                String docPath = doc.getPath();
                if (!docPath.startsWith(canonicalContentPath + "/")) {
                    log.warn("Unexpected document path '{}'", docPath);
                    continue;
                }
                documentLocations.add(new DocumentRepresentation(docPath.substring(canonicalContentPath.length() + 1)));
            }
        } catch (RepositoryException e) {
            log.warn("Exception happened while trying to fetch documents of type '" + docType + "'", e);
            return error("Exception happened while trying to fetch documents of type '" + docType + "': " + e.getMessage());
        }
        return ok("Document list", documentLocations);
    }

    /**
     * reverts the changes for the current cms user for the channel he is working on.
     * reverting changes need to be done directly on JCR level as for the hst model it get very complex as the
     * hst model has an enhanced model on top of jcr, with for example inheritance and referencing resolved
     */
    private Response discardChangesOfCurrentUser(final HstRequestContext requestContext) {
        try {
            HippoSession session = HstConfigurationUtils.getNonProxiedSession(requestContext.getSession(false));
            String currentUserId = session.getUserID();
            return discardChanges(requestContext, Collections.singletonList(currentUserId));
        } catch (RepositoryException e) {
            return error("Could not discard preview configuration of the current user: " + e);
        }
    }

    private Response discardChanges(final HstRequestContext requestContext, List<String> userIds) {
        try {
            String liveConfigurationPath = getEditingLiveMount(requestContext).getHstSite().getConfigurationPath();
            String previewConfigurationPath = getEditingPreviewMount(requestContext).getHstSite().getConfigurationPath();

            HippoSession session = HstConfigurationUtils.getNonProxiedSession(requestContext.getSession(false));
            List<String> relativeContainerPathsToRevert = findChangedContainersForUsers(session, previewConfigurationPath, userIds);
            List<String> mainConfigNodeNamesToRevert = findChangedMainConfigNodeNamesForUsers(session, previewConfigurationPath, userIds);
            pushContainerChildrenNodes(session, liveConfigurationPath, previewConfigurationPath, relativeContainerPathsToRevert);
            copyChangedMainConfigNodes(session, liveConfigurationPath, previewConfigurationPath, mainConfigNodeNamesToRevert);

            HstConfigurationUtils.persistChanges(session);

            log.info("Changes of user '{}' for site '{}' are discarded.", session.getUserID(), getEditingPreviewMount(requestContext).getHstSite().getName());
            return ok("Changes of user '"+session.getUserID()+"' for site '"+getEditingPreviewMount(requestContext).getHstSite().getName()+"' are discarded.");
        } catch (RepositoryException e) {
            log.warn("Could not discard preview configuration: ", e);
            return error("Could not discard preview configuration: " + e);
        }
    }

    private List<String> findChangedContainersForUsers(final HippoSession session, String previewConfigurationPath, List<String> userIds) throws RepositoryException {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        final String xpath = buildXPathQueryToFindContainersForUsers(previewConfigurationPath, userIds);
        final QueryResult result = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH).execute();

        final NodeIterable containersToRevert = new NodeIterable(result.getNodes());
        List<String> relativePathsToRevert = new ArrayList<String>();
        for (Node containerToRevert : containersToRevert) {
            String containerPath = containerToRevert.getPath();
            if (!containerPath.startsWith(previewConfigurationPath)) {
                log.warn("Cannot discard container '{}' because does not start with preview config path '{}'.");
                continue;
            }
            relativePathsToRevert.add(containerToRevert.getPath().substring(previewConfigurationPath.length()));
        }
        log.info("Changed containers for configuration '{}' for users '{}' are : {}",
                new String[]{previewConfigurationPath, userIds.toString(), relativePathsToRevert.toString()});
        return relativePathsToRevert;
    }

    private List<String> findChangedMainConfigNodeNamesForUsers(final HippoSession session, String previewConfigurationPath, List<String> userIds) throws RepositoryException {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        final String xpath = buildXPathQueryToFindMainfConfigNodesForUsers(previewConfigurationPath, userIds);
        final QueryResult result = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH).execute();

        final NodeIterable mainConfigNodesToRevert = new NodeIterable(result.getNodes());
        List<String> mainConfigNodeNamesToRevert = new ArrayList<String>();
        for (Node mainConfigNodeToRevert : mainConfigNodesToRevert) {
            String mainConfigNodePath = mainConfigNodeToRevert.getPath();
            if (!mainConfigNodePath.startsWith(previewConfigurationPath)) {
                log.warn("Cannot discard container '{}' because does not start with preview config path '{}'.");
                continue;
            }
            mainConfigNodeNamesToRevert.add(mainConfigNodeToRevert.getPath().substring(previewConfigurationPath.length() + 1));
        }log.info("Changed main config nodes for configuration '{}' for users '{}' are : {}",
                new String[]{previewConfigurationPath, userIds.toString(), mainConfigNodeNamesToRevert.toString()});
        return mainConfigNodeNamesToRevert;
    }

    private String buildXPathQueryToFindContainersForUsers(String previewConfigurationPath, List<String> userIds) {
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("List of user IDs cannot be empty");
        }

        StringBuilder xpath = new StringBuilder("/jcr:root");
        xpath.append(previewConfigurationPath);
        xpath.append("//element(*,");
        xpath.append(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
        xpath.append(")[");

        String concat = "";
        for (String userId : userIds) {
            xpath.append(concat);
            xpath.append('@');
            xpath.append(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY);
            xpath.append(" = '");
            xpath.append(userId);
            xpath.append("'");
            concat = " or ";
        }
        xpath.append("]");

        return xpath.toString();
    }

    private String buildXPathQueryToFindMainfConfigNodesForUsers(String previewConfigurationPath, List<String> userIds) {
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("List of user IDs cannot be empty");
        }

        StringBuilder xpath = new StringBuilder("/jcr:root");
        xpath.append(previewConfigurationPath);
        xpath.append("/*[");

        String concat = "";
        for (String userId : userIds) {
            xpath.append(concat);
            xpath.append('@');
            xpath.append(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY);
            xpath.append(" = '");
            xpath.append(userId);
            xpath.append("'");
            concat = " or ";
        }
        xpath.append("]");

        return xpath.toString();
    }

    private void pushContainerChildrenNodes(final HippoSession session,
                                            final String fromConfig,
                                            final String toConfig,
                                            final List<String> relativeContainerPaths) throws RepositoryException {

        for (String relativeContainerPath : relativeContainerPaths) {
            String absFromPath = fromConfig + relativeContainerPath;
            String absToContainerPath = toConfig + relativeContainerPath;
            final Node rootNode = session.getRootNode();
            if (rootNode.hasNode(absFromPath.substring(1)) && rootNode.hasNode(absToContainerPath.substring(1))) {
                final Node containerToRelaceChildrenFrom = rootNode.getNode(absToContainerPath.substring(1));
                Node fromNode = rootNode.getNode(absFromPath.substring(1));
                if (!containerToRelaceChildrenFrom.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT) ||
                        !fromNode.isNodeType(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT)) {
                    log.warn("Cannot publish/discard nodes that are not of type hst:containercomponent. Cannot push " +
                            " '{}' to '{}'.", containerToRelaceChildrenFrom.getPath(), fromNode.getPath());
                    continue;
                }
                // WARN DO NOT JUST DELETE OLD CONTAINER AS THIS INTRODUCES ORDERING ISSUES IN CASE OF SIBBLING CONTAINERS
                // WHEN THE NEW CONTAINER IS COPIED BACK. INSTEAD, REMOVE CHILDREN AND ADD
                for (Node oldNode : new NodeIterable(containerToRelaceChildrenFrom.getNodes())) {
                    log.debug("Removing old node '{}'", oldNode.getPath());
                    oldNode.remove();
                }

                if (fromNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
                    fromNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).remove();
                }
                if (fromNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
                    fromNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON).remove();
                }
                if (containerToRelaceChildrenFrom.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
                    containerToRelaceChildrenFrom.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).remove();
                }
                if (containerToRelaceChildrenFrom.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
                    containerToRelaceChildrenFrom.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON).remove();
                }
                fromNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED, Calendar.getInstance());
                fromNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY, session.getUserID());
                containerToRelaceChildrenFrom.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED, Calendar.getInstance());
                containerToRelaceChildrenFrom.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY, session.getUserID());
                for (Node newNode : new NodeIterable(fromNode.getNodes())) {
                    session.copy(newNode, absToContainerPath + "/" + newNode.getName());
                    log.debug("Added new node '{}'", absToContainerPath + "/" + newNode.getName());
                }
                log.info("Containers '{}' pushed succesfully from '{}' to '{}'.",
                        new String[]{relativeContainerPaths.toString(), fromConfig, toConfig});
            } else {
                log.warn("Cannot push node path '{}' because live or preview version for '{}' is not available.",
                        absToContainerPath, relativeContainerPath);
            }
        }
    }

    private void copyChangedMainConfigNodes(final HippoSession session,
                                            final String fromConfig,
                                            final String toConfig,
                                            final List<String> mainConfigNodeNames) throws RepositoryException {
        for (String mainConfigNodeName : mainConfigNodeNames) {
            String absFromPath = fromConfig + "/" + mainConfigNodeName;
            String absToPath = toConfig + "/" + mainConfigNodeName;
            final Node rootNode = session.getRootNode();
            if (rootNode.hasNode(absFromPath.substring(1)) && rootNode.hasNode(absToPath.substring(1))) {
                final Node nodeToReplace = rootNode.getNode(absToPath.substring(1));
                Node fromNode = rootNode.getNode(absFromPath.substring(1));
                if (!fromNode.getParent().isNodeType(HstNodeTypes.NODETYPE_HST_CONFIGURATION) ||
                        !nodeToReplace.getParent().isNodeType(HstNodeTypes.NODETYPE_HST_CONFIGURATION)) {
                    log.warn("Node '{}' or '{]' is not a main node below hst:configuration. Cannot be published or revered",
                            fromNode.getPath(), nodeToReplace.getPath());
                    continue;
                }

                nodeToReplace.remove();

                if (fromNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
                    fromNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).remove();
                }
                if (fromNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
                    fromNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON).remove();
                }

                fromNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED, Calendar.getInstance());
                fromNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY, session.getUserID());
                session.copy(fromNode, absToPath);
            } else {
                log.warn("Cannot copy node '{}' because live or preview version for '{}' is not available.",
                        absToPath, mainConfigNodeName);
            }
        }

        log.info("Main config nodes '{}' pushed succesfully from '{}' to '{}'.",
                new String[]{mainConfigNodeNames.toString(), fromConfig, toConfig});
    }

    private boolean hasPreviewConfiguration(final Mount editingPreviewMount) {
        return editingPreviewMount.getHstSite().hasPreviewConfiguration();
    }

}