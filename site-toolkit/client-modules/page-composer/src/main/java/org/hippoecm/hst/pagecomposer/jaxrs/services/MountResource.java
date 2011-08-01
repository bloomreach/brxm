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
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.composer.ComposerInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.composer.ComposerInfoImpl;
import org.hippoecm.hst.pagecomposer.jaxrs.model.Document;
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
                log.error("Could not get the editing site to create the pate model representation.");
                return error("Could not get the editing site to create the pate model representation.");
            }
            final PageModelRepresentation pageModelRepresentation = new PageModelRepresentation().represent(editingHstSite, pageId);
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
        ToolkitRepresentation toolkitRepresentation = new ToolkitRepresentation().represent(editingMount);
        return ok("Toolkit items loaded successfully", toolkitRepresentation.getComponents().toArray());
    }
    
    @POST
    @Path("/toggle/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response toggle(@Context HttpServletRequest servletRequest,
                                             @Context HttpServletResponse servletResponse) {
        final HstRequestContext requestContext = getRequestContext(servletRequest);
        final Mount editingMount = getEditingHstMount(requestContext);
        
        HttpSession session = servletRequest.getSession(false);
        if(session == null) {
            return error("Cannot toggle because no httpSession");
        }
        
        ComposerInfoImpl composerInfo = (ComposerInfoImpl)session.getAttribute(ContainerConstants.COMPOSER_INFO_ATTR_NAME);
        if(composerInfo == null) {
            composerInfo = new ComposerInfoImpl();
            session.setAttribute(ContainerConstants.COMPOSER_INFO_ATTR_NAME, composerInfo);
        }
        composerInfo.toggleInComposerMode(editingMount.getIdentifier());        
        return ok("Toggled successful", null);
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
     * method that returns a {@link Response} containing the list of document of (sub)type <code>docType</code> that
     * belong to the content of the site that is currently composed.
     *
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
        List<Document> documentLocations = new ArrayList<Document>();
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
                documentLocations.add(new Document(docPath.substring(canonicalContentPath.length() + 1)));
            }
        } catch (RepositoryException e) {
            log.error("Exception happened while trying to fetch documents of type '" + docType + "'", e);
            return error("Exception happened while trying to fetch documents of type '" + docType + "': " + e.getMessage());
        }
        return ok("Document list", documentLocations);
    }

    private HstSite getEditingHstSite(final HstRequestContext requestContext) {
        final Mount mount = getEditingHstMount(requestContext);
        if (mount == null) {
            log.warn("No mount found for identifier '{}'", getRequestConfigIdentifier(requestContext));
            return null;
        }
        return mount.getHstSite();
    }

    private Mount getEditingHstMount(final HstRequestContext requestContext) {
        final String hstMountIdentifier = getRequestConfigIdentifier(requestContext);
        return requestContext.getVirtualHost().getVirtualHosts().getMountByIdentifier(hstMountIdentifier);
    }

}
