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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.beans.ObjectBeanPersistenceException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.workflow.WorkflowPersistenceManagerImpl;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.util.AnnotatedContentBeanClassesScanner;
import org.hippoecm.hst.pagecomposer.jaxrs.model.Document;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ToolkitRepresentation;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */

@Path("/hst:site/")
public class SiteResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(SiteResource.class);


    @GET
    @Path("/toolkit/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getToolkitRepresentation(@Context HttpServletRequest servletRequest,
                                             @Context HttpServletResponse servletResponse) {

        HstRequestContext requestContext = getRequestContext(servletRequest);
        Mount parentMount = requestContext.getResolvedMount().getMount().getParent();
        if (parentMount == null) {
            log.warn("Page Composer only work when there is a parent Mount");
            return error("Page Composer only work when there is a parent Mount");
        }
        ToolkitRepresentation toolkitRepresentation = new ToolkitRepresentation().represent(parentMount);
        return ok("Toolkit items loaded successfully", toolkitRepresentation.getComponents().toArray());

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

        HstRequestContext requestContext = getRequestContext(servletRequest);
        String canonicalContentPath = requestContext.getResolvedMount().getMount().getParent().getCanonicalContentPath();
        try {
            WorkflowPersistenceManagerImpl workflowPersistenceManager = new WorkflowPersistenceManagerImpl(requestContext.getSession(),
                    getObjectConverter(requestContext));
            workflowPersistenceManager.create(canonicalContentPath + "/" + params.getFirst("path"), params.getFirst("docType"), params.getFirst("name"), true);

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

        HstRequestContext requestContext = getRequestContext(servletRequest);
        Mount parentMount = requestContext.getResolvedMount().getMount().getParent();

        List<Document> documentLocations = new ArrayList<Document>();
        String canonicalContentPath = parentMount.getCanonicalContentPath();
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

    @GET
    @Path("/keepalive/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response keepAlive(@Context HttpServletRequest servletRequest,
                              @Context HttpServletResponse servletResponse) {
        HttpSession session = servletRequest.getSession(false);
        return ok("Keepalive successful", null);
    }

    @GET
    @Path("/logout/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest servletRequest,
                           @Context HttpServletResponse servletResponse) {

        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            // logout
            session.invalidate();
        }
        return ok("You are logged out", null);
    }




}
