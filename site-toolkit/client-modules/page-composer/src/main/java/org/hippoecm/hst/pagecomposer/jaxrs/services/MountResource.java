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
import java.util.Collections;
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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.jackrabbit.util.ISO9075;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.DocumentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtIdsRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PageModelRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PrototypePagesRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ToolkitRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.UserRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.LockHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.PagesHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMapHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.SiteMenuHelper;
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

    private SiteMapHelper siteMapHelper;
    private SiteMenuHelper siteMenuHelper;
    private PagesHelper pagesHelper;
    private LockHelper lockHelper = new LockHelper();

    public void setSiteMapHelper(final SiteMapHelper siteMapHelper) {
        this.siteMapHelper = siteMapHelper;
    }

    public void setSiteMenuHelper(final SiteMenuHelper siteMenuHelper) {
        this.siteMenuHelper = siteMenuHelper;
    }

    public void setPagesHelper(final PagesHelper pagesHelper) {
        this.pagesHelper = pagesHelper;
    }

    @GET
    @Path("/pagemodel/{pageId}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPageModelRepresentation(@PathParam("pageId") String pageId) {
        try {
            final HstSite editingPreviewHstSite = getPageComposerContextService().getEditingPreviewSite();
            if (editingPreviewHstSite == null) {
                log.error("Could not get the editing site to create the page model representation.");
                return error("Could not get the editing site to create the page model representation.");
            }
            final PageModelRepresentation pageModelRepresentation = new PageModelRepresentation().represent(
                    editingPreviewHstSite,
                    pageId,
                    getPageComposerContextService().getEditingMount());
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
    public Response getToolkitRepresentation() {
        final Mount editingMount = getPageComposerContextService().getEditingMount();
        ToolkitRepresentation toolkitRepresentation = new ToolkitRepresentation().represent(editingMount);
        log.info("Toolkit items loaded successfully");
        return ok("Toolkit items loaded successfully", toolkitRepresentation.getComponents().toArray());
    }

    @GET
    @Path("/prototypepages")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPrototypePages() {
        final HstSite editingPreviewSite = getPageComposerContextService().getEditingPreviewSite();
        PrototypePagesRepresentation prototypePagesRepresentation = new PrototypePagesRepresentation().represent(editingPreviewSite,
                 false, getPageComposerContextService().getEditingMount());
        log.info("Prototype pages loaded successfully");
        return ok("Prototype pages loaded successfully", prototypePagesRepresentation);
    }

    @GET
    @Path("/userswithchanges/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersWithChanges() {
        final Set<String> changedBySet = getPageComposerContextService().getEditingPreviewChannel().getChangedBySet();
        List<UserRepresentation> usersWithChanges = new ArrayList<>(changedBySet.size());
        final String msg = "Found " + changedBySet.size() + " users with changes : ";
        log.info(msg);
        for (String userId : changedBySet) {
            usersWithChanges.add(new UserRepresentation(userId));
        }
        return ok(msg, usersWithChanges);

    }

    /**
     * If the {@link Mount} that this request belongs to does not have a preview configuration, it will 
     * be created. If it already has a preview configuration, just an ok {@link Response} is returned.
     * @return ok {@link Response} when editing can start, and error {@link Response} otherwise
     */
    @POST
    @Path("/edit/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startEdit() {
        final HstRequestContext requestContext = getPageComposerContextService().getRequestContext();

        try {
            final HstSite editingPreviewSite = getPageComposerContextService().getEditingPreviewSite();
            Session session = requestContext.getSession();
            if (editingPreviewSite.hasPreviewConfiguration()) {
                return ok("Site can be edited now");
            }
            createPreviewChannelAndConfigurationNode();
            HstConfigurationUtils.persistChanges(session);
            log.info("Site '{}' can be edited now", editingPreviewSite.getConfigurationPath());
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

    private void createPreviewChannelAndConfigurationNode() throws RepositoryException {
        HstSite editingLiveSite = getPageComposerContextService().getEditingLiveSite();
        String liveConfigurationPath = editingLiveSite.getConfigurationPath();
        String previewConfigurationPath = liveConfigurationPath + "-preview";
        Session session = getPageComposerContextService().getRequestContext().getSession();
        JcrUtils.copy(session, liveConfigurationPath, previewConfigurationPath);

        Mount editingMount = getPageComposerContextService().getEditingMount();
        String liveChannelPath =  editingMount.getChannelPath();
        String previewChannelPath = liveChannelPath + "-preview";
        JcrUtils.copy(session, liveChannelPath, previewChannelPath);
    }

    /**
     * If the {@link Mount} that this request belongs to has a preview configuration, it will be discarded.
     * @return ok {@link Response} when the discard completed, error {@link Response} otherwise
     */
    @POST
    @Path("/discard/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response discardChanges() {
        return discardChangesOfCurrentUser();
    }

    @POST
    @Path("/userswithchanges/discard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response discardChangesOfUsers(ExtIdsRepresentation ids) {
        if (!getPageComposerContextService().hasPreviewConfiguration()) {
            log.warn("Cannot discard changes of users in a non-preview site");
            return error("Cannot discard changes of users in a non-preview site");
        }
        return discardChanges(ids.getData());
    }

    /**
     * If the {@link Mount} that this request belongs to does not have a preview configuration, it will 
     * be created. If it already has a preview configuration, just an ok {@link Response} is returned.
     * @return ok {@link Response} when editing can start, and error {@link Response} otherwise
     */
    @POST
    @Path("/publish/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response publish() {
        if (!getPageComposerContextService().hasPreviewConfiguration()) {
            return cannotPublishNotPreviewSite();
        }
        return publishChangesOfCurrentUser();
    }

    @POST
    @Path("/userswithchanges/publish")
    @Produces(MediaType.APPLICATION_JSON)
    public Response publishChangesOfUsers(ExtIdsRepresentation ids) {
        if (!getPageComposerContextService().hasPreviewConfiguration()) {
            return cannotPublishNotPreviewSite();
        }
        return publishChangesOfUsers(ids.getData());
    }

    private Response cannotPublishNotPreviewSite() {
        log.warn("Cannot publish non preview site");
        return error("Cannot publish non preview site");
    }


    private Response publishChangesOfCurrentUser() {
        try {
            HippoSession session = HstConfigurationUtils.getNonProxiedSession(getPageComposerContextService().getRequestContext().getSession(false));
            String currentUserId = session.getUserID();
            return publishChangesOfUsers(Collections.singletonList(currentUserId));
        } catch (RepositoryException e) {
            log.warn("Could not publish preview configuration of the current user : ", e);
            return error("Could not publish preview configuration of the current user: " + e);
        }
    }

    private Response publishChangesOfUsers(List<String> userIds) {
        try {
            String liveConfigurationPath = getPageComposerContextService().getEditingLiveSite().getConfigurationPath();
            String previewConfigurationPath = getPageComposerContextService().getEditingPreviewSite().getConfigurationPath();

            HippoSession session = HstConfigurationUtils.getNonProxiedSession(getPageComposerContextService().getRequestContext().getSession(false));
            List<String> mainConfigNodeNamesToPublish = findChangedMainConfigNodeNamesForUsers(session, previewConfigurationPath, userIds);
            copyChangedMainConfigNodes(session, previewConfigurationPath, liveConfigurationPath, mainConfigNodeNamesToPublish);
            publishChannelChanges(session, userIds);

            siteMapHelper.publishChanges(userIds);
            pagesHelper.publishChanges(userIds);
            siteMenuHelper.publishChanges(userIds);
            HstConfigurationUtils.persistChanges(session);
            log.info("Site is published");
            return ok("Site is published");
        } catch (RepositoryException e) {
            log.warn("Could not publish preview configuration : ", e);
            return error("Could not publish preview configuration : " + e);
        }
    }


    /**
     * Creates a document in the repository using the WorkFlowManager
     * The post parameters should contain the 'path', 'docType' and 'name' of the document.
     * @param params The POST parameters
     * @return response JSON with the status of the result
     */
    @POST
    @Path("/create/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDocument(MultivaluedMap<String, String> params) {

        final HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
        try {
            final Mount editingMount = getPageComposerContextService().getEditingMount();
            String canonicalContentPath = editingMount.getContentPath();
            WorkflowPersistenceManagerImpl workflowPersistenceManager = new WorkflowPersistenceManagerImpl(requestContext.getSession(),
                    getObjectConverter(requestContext));
            workflowPersistenceManager.createAndReturn(canonicalContentPath + "/" + params.getFirst("docLocation"), params.getFirst("docType"), params.getFirst("docName"), true);
        } catch (RepositoryException | ObjectBeanPersistenceException e) {
            log.warn("Exception happened while trying to create the document " + e, e);
            return error("Exception happened while trying to create the document " + e);
        }

        log.info("Successfully created a document");
        return ok("Successfully created a document", null);
    }

    /**
     * Method that returns a {@link Response} containing the list of document of (sub)type <code>docType</code> that
     * belong to the content of the site that is currently composed.
     * @param docType         the docType the found documents must be of. The documents can also be a subType of
     *                        docType
     * @return An ok Response containing the list of documents or an error response in case an exception occurred
     */
    @POST
    @Path("/documents/{docType}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocumentsByType(@PathParam("docType") String docType) {

        final HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
        final Mount editingMount = getPageComposerContextService().getEditingMount();
        if (editingMount == null) {
            log.warn("Could not get the editing mount to get the content path for listing documents.");
            return error("Could not get the editing mount to get the content path for listing documents.");
        }
        List<DocumentRepresentation> documentLocations = new ArrayList<DocumentRepresentation>();
        String canonicalContentPath = editingMount.getContentPath();
        try {
            Session session = requestContext.getSession();

            Node contentRoot = (Node) session.getItem(canonicalContentPath);

            String statement = "//element(*," + docType + ")[@hippo:paths = '" + contentRoot.getIdentifier() + "' and @hippo:availability = 'preview' and not(@jcr:primaryType='nt:frozenNode')]";
            QueryManager queryMngr = session.getWorkspace().getQueryManager();
            final Query query = queryMngr.createQuery(statement, "xpath");
            // currently kind of ballpark figure limit as it does not make sense to show hundreds of documents
            // as a result. Nicer would be to include a pathParam for pageSize and page number to support paging.
            query.setLimit(100);
            QueryResult result = query.execute();
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
        log.info("Document list found");
        return ok("Document list", documentLocations);
    }

    /**
     * reverts the changes for the current cms user for the channel he is working on.
     * reverting changes need to be done directly on JCR level as for the hst model it get very complex as the
     * hst model has an enhanced model on top of jcr, with for example inheritance and referencing resolved
     */
    private Response discardChangesOfCurrentUser() {
        try {
            final HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
            HippoSession session = HstConfigurationUtils.getNonProxiedSession(requestContext.getSession(false));
            String currentUserId = session.getUserID();
            return discardChanges(Collections.singletonList(currentUserId));
        } catch (RepositoryException e) {
            log.warn("Could not discard preview configuration of the current user: ", e);
            return error("Could not discard preview configuration of the current user: " + e);
        }
    }

    private Response discardChanges(List<String> userIds) {
        try {
            final HstRequestContext requestContext = getPageComposerContextService().getRequestContext();
            String liveConfigurationPath = getPageComposerContextService().getEditingLiveSite().getConfigurationPath();
            final HstSite editingPreviewSite = getPageComposerContextService().getEditingPreviewSite();
            String previewConfigurationPath = editingPreviewSite.getConfigurationPath();

            HippoSession session = HstConfigurationUtils.getNonProxiedSession(requestContext.getSession(false));
            List<String> mainConfigNodeNamesToRevert = findChangedMainConfigNodeNamesForUsers(session, previewConfigurationPath, userIds);
            copyChangedMainConfigNodes(session, liveConfigurationPath, previewConfigurationPath, mainConfigNodeNamesToRevert);
            discardChannelChanges(session, userIds);

            siteMapHelper.discardChanges(userIds);
            pagesHelper.discardChanges(userIds);
            siteMenuHelper.discardChanges(userIds);

            HstConfigurationUtils.persistChanges(session);
            log.info("Changes of user '{}' for site '{}' are discarded.", session.getUserID(), editingPreviewSite.getName());
            return ok("Changes of user '"+session.getUserID()+"' for site '"+editingPreviewSite.getName()+"' are discarded.");
        } catch (RepositoryException e) {
            log.warn("Could not discard preview configuration: ", e);
            return error("Could not discard preview configuration: " + e);
        }
    }

    private List<String> findChangedNonWorkspacePagesContainersForUsers(final HippoSession session, String previewConfigurationPath, List<String> userIds) throws RepositoryException {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        final String xpath = buildXPathQueryToFindContainersForUsers(previewConfigurationPath, userIds);
        final QueryResult result = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH).execute();

        final NodeIterable containersForUsers = new NodeIterable(result.getNodes());
        List<String> relativeContainersForUsers = new ArrayList<String>();
        for (Node containerForUsers : containersForUsers) {
            String containerPath = containerForUsers.getPath();
            if (!containerPath.startsWith(previewConfigurationPath)) {
                log.warn("Cannot discard container '{}' because does not start with preview config path '{}'.");
                continue;
            }
            final String relativeContainerPath = containerForUsers.getPath().substring(previewConfigurationPath.length());
            relativeContainersForUsers.add(relativeContainerPath);
        }
        log.info("Changed containers for configuration '{}' for users '{}' are : {}",
                new String[]{previewConfigurationPath, userIds.toString(), relativeContainersForUsers.toString()});
        return relativeContainersForUsers;
    }

    private List<String> findChangedMainConfigNodeNamesForUsers(final HippoSession session, String previewConfigurationPath, List<String> userIds) throws RepositoryException {
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }

        final String xpath = buildXPathQueryToFindMainfConfigNodesForUsers(previewConfigurationPath, userIds);
        final QueryResult result = session.getWorkspace().getQueryManager().createQuery(xpath, Query.XPATH).execute();

        final NodeIterable mainConfigNodesForUsers = new NodeIterable(result.getNodes());
        List<String> mainConfigNodeNamesForUsers = new ArrayList<String>();
        for (Node mainConfigNodeForUser : mainConfigNodesForUsers) {
            String mainConfigNodePath = mainConfigNodeForUser.getPath();
            if (!mainConfigNodePath.startsWith(previewConfigurationPath)) {
                log.warn("Cannot discard container '{}' because does not start with preview config path '{}'.");
                continue;
            }
            mainConfigNodeNamesForUsers.add(mainConfigNodeForUser.getPath().substring(previewConfigurationPath.length() + 1));
        }
        log.info("Changed main config nodes for configuration '{}' for users '{}' are : {}",
                new String[]{previewConfigurationPath, userIds.toString(), mainConfigNodeNamesForUsers.toString()});
        return mainConfigNodeNamesForUsers;
    }

    static String buildXPathQueryToFindContainersForUsers(String previewConfigurationPath, List<String> userIds) {
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("List of user IDs cannot be empty");
        }

        StringBuilder xpath = new StringBuilder("/jcr:root");
        xpath.append(ISO9075.encodePath(previewConfigurationPath));
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

    static String buildXPathQueryToFindMainfConfigNodesForUsers(String previewConfigurationPath, List<String> userIds) {
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("List of user IDs cannot be empty");
        }

        StringBuilder xpath = new StringBuilder("/jcr:root");
        xpath.append(ISO9075.encodePath(previewConfigurationPath));
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
                lockHelper.unlock(fromNode);
                fromNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY, session.getUserID());
                JcrUtils.copy(session, fromNode.getPath(), absToPath);
            } else {
                log.warn("Cannot copy node '{}' because live or preview version for '{}' is not available.",
                        absToPath, mainConfigNodeName);
            }
        }

        log.info("Main config nodes '{}' pushed succesfully from '{}' to '{}'.",
                new String[]{mainConfigNodeNames.toString(), fromConfig, toConfig});
    }


    private void discardChannelChanges(final HippoSession session,
                                       final List<String> userIds) throws RepositoryException {
        if (userIds.isEmpty()) {
            return;
        }
        final Channel previewChannel = getPageComposerContextService().getEditingPreviewChannel();
        if (previewChannel == null) {
            log.warn("Preview channel null. Cannot discard its changes");
            return;
        }
        if (previewChannel.getChannelNodeLockedBy() == null) {
            log.debug("Preview channel '{}' is not locked and has thus no changes to discard.", previewChannel.getId());
            return;
        }
        if (!userIds.contains(previewChannel.getChannelNodeLockedBy())) {
            log.debug("Preview channel '{}' is locked by '{}' but won't be discarded since not present in user id list '{}'.",
                    new String[]{previewChannel.getId(), previewChannel.getChannelNodeLockedBy(), userIds.toString()});
            return;
        }
        log.info("Discarding changes in channel '{}' for user '{}'", previewChannel.getId(), previewChannel.getChannelNodeLockedBy());
        final String previewChannelPath = getPageComposerContextService().getEditingPreviewChannelPath();
        if (previewChannelPath != null && previewChannelPath.endsWith("-preview")) {
            String liveChannelPath = previewChannelPath.substring(0, previewChannelPath.length() - "-preview".length());
            copyChannelInfoNodes(session, liveChannelPath, previewChannelPath);
        }
    }

    private void publishChannelChanges(final HippoSession session,
                                       final List<String> userIds) throws RepositoryException {
        if (userIds.isEmpty()) {
            return;
        }
        final Channel previewChannel = getPageComposerContextService().getEditingPreviewChannel();
        if (previewChannel == null) {
            log.warn("Preview channel null. Cannot publish its changes");
            return;
        }
        if (previewChannel.getChannelNodeLockedBy() == null) {
            log.debug("Preview channel '{}' is not locked and has thus no changes to publish.", previewChannel.getId());
            return;
        }
        if (!userIds.contains(previewChannel.getChannelNodeLockedBy())) {
            log.debug("Preview channel '{}' is locked by '{}' but won't be published since not present in user id list '{}'.",
                    new String[]{previewChannel.getId(), previewChannel.getChannelNodeLockedBy(), userIds.toString()});
            return;
        }
        log.info("Publishing changes in channel '{}' for user '{}'", previewChannel.getId(), previewChannel.getChannelNodeLockedBy());
        final String previewChannelPath = getPageComposerContextService().getEditingPreviewChannelPath();
        if (previewChannelPath != null && previewChannelPath.endsWith("-preview")) {
            String liveChannelPath = previewChannelPath.substring(0, previewChannelPath.length() - "-preview".length());
            copyChannelInfoNodes(session, previewChannelPath, liveChannelPath);
        }
    }

    private void copyChannelInfoNodes(final HippoSession session, final String fromConfig, final String toConfig) throws RepositoryException {
        Node channelToNode = session.getNode(toConfig);
        channelToNode.remove();
        Node channelFromNode = session.getNode(fromConfig);
        if (channelFromNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            channelFromNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).remove();
        }
        if (channelFromNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
            channelFromNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON).remove();
        }
        channelFromNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY, session.getUserID());
        JcrUtils.copy(session, fromConfig, toConfig);
    }


}
