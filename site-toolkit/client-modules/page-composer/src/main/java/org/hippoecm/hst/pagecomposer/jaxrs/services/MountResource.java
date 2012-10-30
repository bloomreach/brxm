/*
 *  Copyright 2010 Hippo.
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
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.DocumentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.PageModelRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ToolkitRepresentation;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
@Path("/hst:mount/")
public class MountResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(MountResource.class);

    @GET
    @Path("/pagemodel/{pageId}/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPageModelRepresentation(@Context HttpServletRequest servletRequest,
                                               @Context HttpServletResponse servletResponse,
                                               @PathParam("pageId") String pageId) {
        try { 
            final HstRequestContext requestContext = getRequestContext(servletRequest);
            final HstSite editingPreviewHstSite = getEditingPreviewSite(requestContext);
            if (editingPreviewHstSite == null) {
                log.error("Could not get the editing site to create the page model representation.");
                return error("Could not get the editing site to create the page model representation.");
            }
            final PageModelRepresentation pageModelRepresentation = new PageModelRepresentation().represent(editingPreviewHstSite, pageId, getEditingPreviewMount(requestContext));
            return ok("PageModel loaded successfully", pageModelRepresentation.getComponents().toArray());
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to retrieve page model.", e);
            } else {
                log.warn("Failed to retrieve page model. {}", e.toString());
            }
            return error(e.toString());
        }
    }
    
    @GET
    @Path("/toolkit/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getToolkitRepresentation(@Context HttpServletRequest servletRequest,
                                             @Context HttpServletResponse servletResponse) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        final Mount editingMount = getEditingPreviewMount(requestContext);
        if (editingMount == null) {
            log.error("Could not get the editing site to create the toolkit representation.");
            return error("Could not get the editing site to create the toolkit representation.");
        }

        setCurrentMountCanonicalContentPath(servletRequest, editingMount.getCanonicalContentPath());

        ToolkitRepresentation toolkitRepresentation = new ToolkitRepresentation().represent(editingMount);
        return ok("Toolkit items loaded successfully", toolkitRepresentation.getComponents().toArray());
    }

    /**
     * Try to lock a mount.
     * @param servletRequest
     * @return  ok - already-locked {@link Response} when the mount was already locked.
     *          ok - lock-acquired {@link Response} when the lock was acquired
     *          error {@link Response} when something went wrong
     */
    @POST
    @Path("/lock/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setLock(@Context HttpServletRequest servletRequest) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);

        Mount editingPreviewMount = getEditingPreviewMount(requestContext);
        if (editingPreviewMount == null) {
            return error("This mount is not suitable for the template composer.");
        }

        String configPath = editingPreviewMount.getHstSite().getConfigurationPath();

        if(configPath != null) {
            try {
                Session session = requestContext.getSession();
                Node configurationNode = session.getNode(configPath);
                if (isLockedBySomeoneElse(configurationNode)) {
                    return ok("This configuration was already locked.", "already-locked");
                } else {
                    setLockProperties(configurationNode);
                    session.save();
                    return ok("This configuration lock was acquired.", "lock-acquired");
                }
            } catch (LoginException e) {
                return error("Could not get a jcr session : " + e + ".");
            } catch (RepositoryException e) {
                return error("Could not check lock : " + e );
            }
        }
        return error("Could not find the mount configuration.");
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

        servletRequest.getSession().removeAttribute(ContainerConstants.RENDER_VARIANT);

        try {
            HstSite ctxEditingPreviewSite = getEditingPreviewMount(requestContext).getHstSite();
            HstSite ctxEditingLiveMountSite = getEditingLiveMount(requestContext).getHstSite();

            Session session = requestContext.getSession();
            
            if (ctxEditingPreviewSite.hasPreviewConfiguration()) {
                String configPath = ctxEditingPreviewSite.getConfigurationPath();
                Node configurationNode = session.getNode(configPath);
                if (isLockedBySomeoneElse(configurationNode)) {
                    return error("This channel is locked.", "locked");
                }
                if (!isLockedBySession(configurationNode)) {
                    setLockProperties(configurationNode);
                    session.save();
                }
                return ok("Site can be edited now");
            }

            long liveVersion = ctxEditingLiveMountSite.getVersion();
            long newVersion = liveVersion + 1;
            String liveConfigurationPath = ctxEditingLiveMountSite.getConfigurationPath();
            StringBuilder newPreviewConfigurationPathBuilder = new StringBuilder();
            newPreviewConfigurationPathBuilder.append(StringUtils.substringBeforeLast(liveConfigurationPath, "/"));
            newPreviewConfigurationPathBuilder.append("/").append(ctxEditingLiveMountSite.getName());
            newPreviewConfigurationPathBuilder.append("-v").append(newVersion);
            String newPreviewConfigurationPath = newPreviewConfigurationPathBuilder.toString();

            session.getWorkspace().copy(liveConfigurationPath, newPreviewConfigurationPath);
            Node newPreviewConfigurationNode = session.getNode(newPreviewConfigurationPath);
            Node hstPreviewSiteNode = session.getNodeByIdentifier(ctxEditingPreviewSite.getCanonicalIdentifier());
            hstPreviewSiteNode.setProperty(HstNodeTypes.SITE_VERSION, newVersion);

            setLockProperties(newPreviewConfigurationNode);
            session.save();
        } catch (IllegalStateException e) {
            return error("Cannot start editing : " + e);
        } catch (LoginException e) {
            return error("Could not get a jcr session : " + e + ". Cannot create a  preview configuration.");
        } catch (RepositoryException e) {
            return error("Could not create a preview configuration : " + e);
        }
   
        return ok("Site can be edited now");
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
        return deletePreviewConfigurationIfNotLocked(requestContext);
    }

    /**
     * If the {@link Mount} that this request belongs to has a preview configuration, it will be unlocked and deleted.
     * @param servletRequest
     * @return ok {@link Response} when the discard completed, error {@link Response} otherwise
     */
    @POST
    @Path("/unlock/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unlock(@Context HttpServletRequest servletRequest) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        return deletePreviewConfiguration(requestContext);
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
        final Mount editingPreviewMount = getEditingPreviewMount(requestContext);

        if (!hasPreviewConfiguration(editingPreviewMount)) {
            return error("Cannot publish non preview site");
        }

        final HstSite editingLiveSite = getEditingLiveSite(requestContext);
        final HstSite editingPreviewSite = getEditingPreviewSite(requestContext);

        String previewConfigPath = editingPreviewSite.getConfigurationPath();

        try {
            Session session = requestContext.getSession();
            Node previewConfigurationNode = session.getNode(previewConfigPath);
            if (isLockedBySomeoneElse(previewConfigurationNode)) {
                return error("Locked by another user.", "locked");
            }
            
            removeLockProperties(previewConfigurationNode);
            session.removeItem(editingLiveSite.getConfigurationPath());
            Node HstSiteLiveNode = session.getNodeByIdentifier(editingLiveSite.getCanonicalIdentifier());
            HstSiteLiveNode.setProperty(HstNodeTypes.SITE_VERSION, editingPreviewSite.getVersion());
            session.save();
        } catch (LoginException e) {
            return error("Could not get a jcr session : " + e + ". Cannot publish configuration.");
        } catch (RepositoryException e) {
            return error("Could not publish preview configuration : " + e);
        }

        return ok("Site is published");

    }
    
    /**
     * Creates a document in the repository using the WorkFlowManager
     * The post parameters should contain the 'path', 'docType' and 'name' of the document.
     * @param servletRequest Servlet Request
     * @param servletResponse Servlet Response
     * @param params The POST parameters
     * @return response JSON with the status of the result
     */
    @POST
    @Path("/create/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDocument(@Context HttpServletRequest servletRequest,
                                   @Context HttpServletResponse servletResponse,
                                   MultivaluedMap<String, String> params) {

        final HstRequestContext requestContext = getRequestContext(servletRequest);
        try {
            final Mount editingPreviewMount = getEditingPreviewMount(requestContext);
            if (editingPreviewMount == null) {
                log.error("Could not get the editing mount to get the content path for creating the document.");
                return error("Could not get the editing mount to get the content path for creating the document.");
            }
            String canonicalContentPath = editingPreviewMount.getCanonicalContentPath();
            WorkflowPersistenceManagerImpl workflowPersistenceManager = new WorkflowPersistenceManagerImpl(requestContext.getSession(),
                    getObjectConverter(requestContext));
            workflowPersistenceManager.createAndReturn(canonicalContentPath + "/" + params.getFirst("docLocation"), params.getFirst("docType"), params.getFirst("docName"), true);
        } catch (RepositoryException e) {
            log.error("Exception happened while trying to create the document " + e, e);
            return error("Exception happened while trying to create the document " + e);
        } catch (ObjectBeanPersistenceException e) {
            log.error("Exception happened while trying to create the document " + e, e);
            return error("Exception happened while trying to create the document " + e);
        }
        return ok("Successfully created a document", null);
    }

    /**
     * Method that returns a {@link Response} containing the list of document of (sub)type <code>docType</code> that
     * belong to the content of the site that is currently composed.
     * @param servletRequest
     * @param servletResponse
     * @param docType         the docType the found documents must be of. The documents can also be a subType of
     *                        docType
     * @return An ok Response containing the list of documents or an error response in case an exception occurred
     */
    @POST
    @Path("/documents/{docType}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocumentsByType(@Context HttpServletRequest servletRequest,
                                       @Context HttpServletResponse servletResponse, @PathParam("docType") String docType) {

        final HstRequestContext requestContext = getRequestContext(servletRequest);
        final Mount editingHstMount = getEditingPreviewMount(requestContext);
        if (editingHstMount == null) {
            log.error("Could not get the editing mount to get the content path for listing documents.");
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
                    log.error("Unexpected document path '{}'", docPath);
                    continue;
                }
                documentLocations.add(new DocumentRepresentation(docPath.substring(canonicalContentPath.length() + 1)));
            }
        } catch (RepositoryException e) {
            log.error("Exception happened while trying to fetch documents of type '" + docType + "'", e);
            return error("Exception happened while trying to fetch documents of type '" + docType + "': " + e.getMessage());
        }
        return ok("Document list", documentLocations);
    }

    private Response deletePreviewConfigurationIfNotLocked(final HstRequestContext requestContext) {
        final Mount editingPreviewMount = getEditingPreviewMount(requestContext);
        try {
            Node previewConfigurationNode = getConfigurationNodeForMount(requestContext.getSession(), editingPreviewMount);
            if (isLockedBySomeoneElse(previewConfigurationNode)) {
                return error("Preview for '"+editingPreviewMount.getMountPoint()+"' cannot be deleted because locked by another user.", "locked");
            }
        } catch (RepositoryException e) {
            return error("Could not discard preview configuration : " + e);
        }
        return deletePreviewConfiguration(requestContext);
    }

    private Response deletePreviewConfiguration(final HstRequestContext requestContext) {

        final Mount editingPreviewMount = getEditingPreviewMount(requestContext);
        if (!hasPreviewConfiguration(editingPreviewMount)) {
            return error("Cannot discard non preview site because there is no preview configuration");
        }

        String previewConfigPath = editingPreviewMount.getHstSite().getConfigurationPath();
        try {
            Session session = requestContext.getSession();
            session.removeItem(previewConfigPath);
            resetVersionToLive(requestContext);
            session.save();
        } catch (LoginException e) {
            return error("Could not get a jcr session : " + e  + ". Cannot discard configuration.");
        } catch (RepositoryException e) {
            return error("Could not discard preview configuration : " + e);
        }
        return ok("Template is discarded");
        
    }

    private void resetVersionToLive(final HstRequestContext requestContext) throws RepositoryException {
        Session session = requestContext.getSession();
        long liveVersion = getEditingLiveMount(requestContext).getHstSite().getVersion();
        Node hstPreviewSiteNode = session.getNodeByIdentifier(getEditingPreviewMount(requestContext).getHstSite().getCanonicalIdentifier());
        hstPreviewSiteNode.setProperty(HstNodeTypes.SITE_VERSION, liveVersion);
    }

    private Node getConfigurationNodeForMount(final Session session, final Mount mount) throws RepositoryException {
        String previewConfigPath = mount.getHstSite().getConfigurationPath();
        return session.getNode(previewConfigPath);
    }

    private boolean hasPreviewConfiguration(final Mount editingPreviewMount) {
        return editingPreviewMount.getHstSite().hasPreviewConfiguration();
    }

    private boolean isLockedBySomeoneElse(Node configurationNode) throws RepositoryException {
        final String holder = getLockedBy(configurationNode);
        if (StringUtils.isEmpty(holder)) {
            return false;
        }
        return !configurationNode.getSession().getUserID().equals(holder);
    }

    private boolean isLockedBySession(Node configurationNode) throws RepositoryException {
        final String holder = getLockedBy(configurationNode);
        if (StringUtils.isEmpty(holder)) {
            return false;
        }
        return configurationNode.getSession().getUserID().equals(holder);
    }

    private String getLockedBy(Node configurationNode) throws RepositoryException {
        if (!configurationNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            return null;
        }
        return configurationNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
    }

    /**
     * sets a not yet saved lock properties
     */
    private void setLockProperties(Node configurationNode) throws RepositoryException {
        assertCorrectNodeType(configurationNode, HstNodeTypes.NODETYPE_HST_CONFIGURATION);
        if (isLockedBySomeoneElse(configurationNode)) {
            throw new IllegalStateException("Cannot lock '"+configurationNode.getPath()+"' because locked by someone else.");
        }
        configurationNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, configurationNode.getSession().getUserID());
        configurationNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON, new GregorianCalendar());
    }

    private void removeLockProperties(Node configurationNode) throws RepositoryException {
        assertCorrectNodeType(configurationNode, HstNodeTypes.NODETYPE_HST_CONFIGURATION);
        if (configurationNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            configurationNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).remove();
        }
        if (configurationNode.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
            configurationNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON).remove();
        }
    }


    private void assertCorrectNodeType(final Node node, String nodeType) throws RepositoryException {
        if (!node.isNodeType(nodeType)) {
            throw new IllegalArgumentException("Unexpected nodetype for '"+node.getPath()+"'. Expected a node" +
                    "of type '"+nodeType+"'");
        }
    }

}