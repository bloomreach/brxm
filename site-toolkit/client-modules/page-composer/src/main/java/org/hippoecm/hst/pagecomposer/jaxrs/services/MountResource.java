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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
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

import org.apache.cxf.common.util.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
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
            final HstSite editingHstSite = getEditingHstSite(requestContext);
            if (editingHstSite == null) {
                log.error("Could not get the editing site to create the page model representation.");
                return error("Could not get the editing site to create the page model representation.");
            }
            final PageModelRepresentation pageModelRepresentation = new PageModelRepresentation().represent(editingHstSite, pageId, getEditingHstMount(requestContext));
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
        final Mount editingMount = getEditingHstMount(requestContext);
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

        ContextualizableMount ctxEditingMount = getPreviewMount(requestContext);
        if (ctxEditingMount == null) {
            return error("This mount is not suitable for the template composer.");
        }

        String configPath = ctxEditingMount.getPreviewHstSite().getConfigurationPath();

        if(configPath != null) {
            try {
                Session jcrSession = requestContext.getSession();
                if (isLocked(jcrSession, configPath)) {
                    return ok("This configuration was already locked.", "already-locked");
                } else {
                    lock(jcrSession, configPath);
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
     * @param servletResponse
     * @return ok {@link Response} when editing can start, and error {@link Response} otherwise
     */
    @POST
    @Path("/edit/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startEdit(@Context HttpServletRequest servletRequest,
                                             @Context HttpServletResponse servletResponse) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);

        servletRequest.getSession().removeAttribute("RENDER_VARIANT");

        ContextualizableMount ctxEditingMount = getPreviewMount(requestContext);
        if (ctxEditingMount == null) {
            return error("This mount is not suitable for the template composer.");
        }
        
        String configPath = ctxEditingMount.getPreviewHstSite().getConfigurationPath();

        if(configPath != null) {
            // preview configuration is the same as live configuration. We need to create a preview now
            try {
                Session jcrSession = requestContext.getSession();

                // check if the configuration is not locked by another user
                if (isLocked(jcrSession, configPath)) {
                    return error("This channel is locked.");
                }

                if(!configPath.endsWith("-" + Mount.PREVIEW_NAME)) {
                    final String previewConfiguration = new StringBuilder().append(configPath).append("-").append(Mount.PREVIEW_NAME).toString();
                    jcrSession.getWorkspace().copy(configPath, previewConfiguration);
                    lock(jcrSession, previewConfiguration);
                } else {
                    lock(jcrSession, configPath);
                }
            } catch (LoginException e) {
                return error("Could not get a jcr session : " + e + ". Cannot create a  preview configuration.");
            } catch (RepositoryException e) {
                return error("Could not create a preview configuration : " + e );
            }
        } else {
            // preview configuration already exists
            return error("Config path cannot be null");
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
        final Mount editingMount = getEditingHstMount(requestContext);
        if(editingMount.getType().equals(Mount.PREVIEW_NAME)) {
            return error("Cannot publish preview mounts. Template composer should work with live mounts decorated as preview.");
        }
        return deletePreviewMount(requestContext, (ContextualizableMount) editingMount, true);
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
        final Mount editingMount = getEditingHstMount(requestContext);
        if(editingMount.getType().equals(Mount.PREVIEW_NAME)) {
            return error("Cannot publish preview mounts. Template composer should work with live mounts decorated as preview.");
        }
        return deletePreviewMount(requestContext, (ContextualizableMount) editingMount, false);
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
        final Mount editingMount = getEditingHstMount(requestContext); 
        
        if(editingMount.getType().equals(Mount.PREVIEW_NAME)) {
            return error("Cannot publish preview mounts. Template composer should work with live mounts decorated as preview.");
        }
         
       ContextualizableMount ctxEditingMount = (ContextualizableMount) editingMount;
       
       String previewConfigPath = ctxEditingMount.getPreviewHstSite().getConfigurationPath();
       if(previewConfigPath != null && previewConfigPath.endsWith("-" + Mount.PREVIEW_NAME)) {
           // preview configuration exists: Remove now live and rename preview
           String liveConfigPath = previewConfigPath.substring(0, previewConfigPath.length() - (Mount.PREVIEW_NAME.length() + 1) );
           try {
               Session jcrSession = requestContext.getSession();
               // Don't publish when the configuration is locked
               if (isLocked(jcrSession, previewConfigPath)) {
                   return error("Locked by another user.", "locked");
               }
               // Remove the lock properties
               final Node node = jcrSession.getNode(previewConfigPath);
               if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
                   node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).remove();
               }
               if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
                   node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON).remove();
               }
               jcrSession.removeItem(liveConfigPath);
               jcrSession.move(previewConfigPath, liveConfigPath);
               jcrSession.save();
            } catch (LoginException e) {
                return error("Could not get a jcr session : " + e  + ". Cannot publish configuration.");
            } catch (RepositoryException e) {
                return error("Could not publish preview configuration : " + e );
            }

            return ok("Site is published");
        } else {
            return error("Cannot publish non preview site");
        }
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
            final Mount editingHstMount = getEditingHstMount(requestContext);
            if (editingHstMount == null) {
                log.error("Could not get the editing mount to get the content path for creating the document.");
                return error("Could not get the editing mount to get the content path for creating the document.");
            }
            String canonicalContentPath = editingHstMount.getCanonicalContentPath();
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
        final Mount editingHstMount = getEditingHstMount(requestContext);
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

    private boolean isLocked(Session session, String path) throws RepositoryException {
        final String holder = getLockedBy(session, path);
        if (StringUtils.isEmpty(holder)) {
            return false;
        }
        return !session.getUserID().equals(holder);
    }

    private String getLockedBy(Session session, String path) throws RepositoryException {
        final Node node = session.getNode(path);
        if (!node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, "");
        }
        final Property holderProperty = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY);
        return holderProperty.getString();
    }

    private Calendar getLockedOn(final Session session, final String path) throws RepositoryException {
        final Node node = session.getNode(path);
        if (!node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
            node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON, new GregorianCalendar());
        }
        final Property holderProperty = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON);
        return holderProperty.getDate();
    }

    private void lock(Session session, String path) throws RepositoryException {
        final Node node = session.getNode(path);
        node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, session.getUserID());
        node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON, new GregorianCalendar());
        session.save();
    }

    private ContextualizableMount getPreviewMount(final HstRequestContext requestContext) {
        final Mount editingMount = getEditingHstMount(requestContext);
        if (editingMount.getType().equals(Mount.PREVIEW_NAME)) {
            log.error("The mount is configured as PREVIEW. Template composer works against live mounts decorated to preview.");
            return null;
        }
        if (editingMount == null || !(editingMount instanceof ContextualizableMount)) {
            log.error("Could not get the editing site to create the toolkit representation.");
            return null;
        }
        return (ContextualizableMount) editingMount;
    }

    private Response deletePreviewMount(final HstRequestContext requestContext,
                                        final ContextualizableMount ctxEditingMount, boolean lockCheck) {
        String previewConfigPath = ctxEditingMount.getPreviewHstSite().getConfigurationPath();
        if (previewConfigPath != null && previewConfigPath.endsWith("-" + Mount.PREVIEW_NAME)) {
            try {
                Session jcrSession = requestContext.getSession();
                if (lockCheck && isLocked(jcrSession, previewConfigPath)) {
                    return error("Locked by another user.", "locked");
                }
                jcrSession.removeItem(previewConfigPath);
                jcrSession.save();
            } catch (LoginException e) {
                return error("Could not get a jcr session : " + e  + ". Cannot discard configuration.");
            } catch (RepositoryException e) {
                return error("Could not discard preview configuration : " + e);
            }
            return ok("Template is discarded");
        } else {
            return error("Cannot discard non preview site");
        }
    }
}