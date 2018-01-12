/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.hippoecm.repository.api.HippoNode;
import org.onehippo.cms7.essentials.plugin.sdk.model.ContentType;
import org.onehippo.cms7.essentials.plugin.sdk.model.PluginDescriptor;
import org.onehippo.cms7.essentials.plugin.sdk.rest.BaseResource;
import org.onehippo.cms7.essentials.plugin.sdk.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.plugin.sdk.rest.RestfulList;
import org.onehippo.cms7.essentials.plugin.sdk.service.ContentTypeService;
import org.onehippo.cms7.essentials.plugin.sdk.service.JcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

@Api(value = "/documents", description = "Rest resource which provides information and actions for document types")
@CrossOriginResourceSharing(allowAllOrigins = true)
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/documents/")
public class DocumentResource extends BaseResource {

    private static final Logger log = LoggerFactory.getLogger(DocumentResource.class);
    private static final String QUERY_STATEMENT_QUERIES = "hippo:configuration/hippo:queries/hippo:templates//element(*, hippostd:templatequery)";

    @Inject private JcrService jcrService;
    @Inject private ContentTypeService contentTypeService;

    @ApiOperation(
            value = "Fetches all project document types (including compounds)",
            response = RestfulList.class)
    @GET
    @Path("/")
    public List<ContentType> getAllTypes() {
        return contentTypeService.fetchContentTypesFromOwnNamespace();
    }

    @ApiOperation(
            value = "Fetches all project document types (compounds are *excluded*)",
            response = RestfulList.class)
    @GET
    @Path("/documents")
    public List<ContentType> getDocumentTypes() {
        return contentTypeService.fetchContentTypesFromOwnNamespace()
                .stream()
                .filter(type -> !type.isCompoundType())
                .collect(Collectors.toList());
    }

    @ApiOperation(
            value = "Fetches all project compound types",
            response = RestfulList.class)
    @GET
    @Path("/compounds")
    public List<ContentType> getCompounds() {
        return contentTypeService.fetchContentTypesFromOwnNamespace()
                .stream()
                .filter(ContentType::isCompoundType)
                .collect(Collectors.toList());
    }


    @ApiOperation(
            value = "Returns all documents of the specified type",
            notes = "Specify the document type as {namespace}:{typename}.",
            response = PluginDescriptor.class)
    @ApiParam(name = "docType", value = "Document type", required = true)
    @GET
    @Path("/{docType}")
    public List<KeyValueRestful> getDocumentsByType(@PathParam("docType") String docType) {
        final List<KeyValueRestful> valueLists = new ArrayList<>();
        final Session session = jcrService.createSession();
        if (session != null) {
            try {
                final QueryManager queryManager = session.getWorkspace().getQueryManager();
                final Query xpath = queryManager.createQuery("//content/documents//element(*, " + docType + ')', "xpath");
                final NodeIterator nodes = xpath.execute().getNodes();
                while (nodes.hasNext()) {
                    Node node = nodes.nextNode();
                    final Node parent = node.getParent();
                    if (parent.isNodeType(NT_HANDLE)) {
                        node = parent;
                    }
                    final String path = node.getPath();
                    valueLists.add(new KeyValueRestful(((HippoNode) node).getDisplayName(), path));
                }
            } catch (RepositoryException e) {
                log.debug("Error fetching value lists", e);
            } finally {
                jcrService.destroySession(session);
            }
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
    public List<KeyValueRestful> getQueryCombinations() {
        final List<KeyValueRestful> templateList = new ArrayList<>();
        final Session session = jcrService.createSession();
        if (session != null) {
            try {
                final QueryManager queryManager = session.getWorkspace().getQueryManager();
                final Query query = queryManager.createQuery(QUERY_STATEMENT_QUERIES, "xpath");
                final QueryResult result = query.execute();
                final NodeIterator nodes = result.getNodes();
                while (nodes.hasNext()) {
                    final Node node = nodes.nextNode();
                    final String name = node.getName();
                    templateList.add(new KeyValueRestful(name, name));
                }
            } catch (RepositoryException e) {
                log.debug("Error fetching value lists", e);
            } finally {
                jcrService.destroySession(session);
            }
        }
        return templateList;
    }
}
