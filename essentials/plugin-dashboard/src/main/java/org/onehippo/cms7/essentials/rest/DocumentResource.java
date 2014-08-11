/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.hippoecm.repository.api.HippoNode;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.model.PluginRestful;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.ContentTypeServiceUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.model.DocumentRestful;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

/**
 * @version "$Id$"
 */
@Api(value = "/documents", description = "Rest resource which provides information and actions for document types")
@CrossOriginResourceSharing(allowAllOrigins = true)
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/documents/")
public class DocumentResource extends BaseResource {

    private static final Logger log = LoggerFactory.getLogger(DocumentResource.class);
    private static final String QUERY_STATEMENT_QUERIES = "hippo:configuration/hippo:queries/hippo:templates//element(*, hippostd:templatequery)";

    @ApiOperation(
            value = "Fetches all project document types (including compounds)",
            response = RestfulList.class)
    @GET
    @Path("/")
    public List<DocumentRestful> getAllTypes(@Context ServletContext servletContext) {
        return ContentTypeServiceUtils.fetchDocuments(ContentTypeServiceUtils.Type.ALL);
    }

    @ApiOperation(
            value = "Fetches all project document types (compounds are *excluded*)",
            response = RestfulList.class)
    @GET
    @Path("/documents")
    public List<DocumentRestful> getDocumentTypes(@Context ServletContext servletContext) {
        return ContentTypeServiceUtils.fetchDocuments(ContentTypeServiceUtils.Type.DOCUMENT);
    }

    @ApiOperation(
            value = "Fetches all project compound types",
            response = RestfulList.class)
    @GET
    @Path("/compounds")
    public List<DocumentRestful> getCompounds(@Context ServletContext servletContext) {
        return ContentTypeServiceUtils.fetchDocuments(ContentTypeServiceUtils.Type.COMPOUND);
    }


    @ApiOperation(
            value = "Returns all documents of the specified type",
            notes = "Specify the document type as {namespace}:{typename}.",
            response = PluginRestful.class)
    @ApiParam(name = "docType", value = "Document type", required = true)
    @GET
    @Path("/{docType}")
    public List<KeyValueRestful> getDocumentsByType(@Context ServletContext servletContext, @PathParam("docType") String docType) {
        final List<KeyValueRestful> valueLists = new ArrayList<>();
        final PluginContext context = PluginContextFactory.getContext();
        final Session session = context.createSession();

        try {
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query xpath = queryManager.createQuery("//content/documents//element(*, " + docType + ')', "xpath");
            final NodeIterator nodes = xpath.execute().getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                final Node parent = node.getParent();
                if ("hippo:handle".equals(parent.getPrimaryNodeType().getName())) {
                    node = parent;
                }
                final String path = node.getPath();
                valueLists.add(new KeyValueRestful(((HippoNode) node).getLocalizedName(), path));
            }
        } catch (RepositoryException e) {
            log.debug("Error fetching value lists", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        Collections.sort(valueLists);
        return valueLists;
    }

    @ApiOperation(
            value = "Returns all document / folder query types",
            notes = "No pairing is done (e.g.: news-folder + news-document combinations. This is left to users themselves)",
            response = KeyValueRestful.class)
    @ApiParam(name = "docType", value = "Document type", required = true)
    @GET
    @Path("/templatequeries")
    public List<KeyValueRestful> getQueryCombinations(@Context ServletContext servletContext) {
        final List<KeyValueRestful> templateList = new ArrayList<>();
        final PluginContext context = PluginContextFactory.getContext();
        final Session session = context.createSession();
        try {
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query query = queryManager.createQuery(QUERY_STATEMENT_QUERIES, "xpath");
            final QueryResult result = query.execute();
            final NodeIterator nodes = result.getNodes();
            while(nodes.hasNext()){
                final Node node = nodes.nextNode();
                final String name = node.getName();
                templateList.add(new KeyValueRestful(name, name));
            }


        } catch (RepositoryException e) {
            log.debug("Error fetching value lists", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        return templateList;
    }
}
