/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.rest.exc.RestException;
import org.onehippo.cms7.essentials.rest.model.KeyValueRestful;
import org.onehippo.cms7.essentials.rest.model.MessageRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.onehippo.cms7.essentials.rest.model.contentblocks.AllDocumentMatcher;
import org.onehippo.cms7.essentials.rest.model.contentblocks.CBPayload;
import org.onehippo.cms7.essentials.rest.model.contentblocks.Compounds;
import org.onehippo.cms7.essentials.rest.model.contentblocks.ContentBlockModel;
import org.onehippo.cms7.essentials.rest.model.contentblocks.DocumentTypes;
import org.onehippo.cms7.essentials.rest.model.contentblocks.HasProviderMatcher;
import org.onehippo.cms7.essentials.rest.utils.RestWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/documenttypes/")
public class ContentBlocksResource extends BaseResource {


    private static Logger log = LoggerFactory.getLogger(ContentBlocksResource.class);


    @GET
    @Path("/")
    public RestfulList<DocumentTypes> getControllers(@Context ServletContext servletContext) {
        final RestfulList<DocumentTypes> types = new RestfulList<>();

        final Session session = GlobalUtils.createSession();
        final PluginContext context = getContext(servletContext);
        final String projectNamespacePrefix = context.getProjectNamespacePrefix();
        String prefix = projectNamespacePrefix + ':';

        try {
            final List<String> primaryTypes = HippoNodeUtils.getPrimaryTypes(session, new AllDocumentMatcher(), "new-document");
            final Map<String, Compounds> compoundMap = getCompoundMap(servletContext);

            for (String primaryType : primaryTypes) {
                final RestfulList<KeyValueRestful> keyValueRestfulRestfulList = new RestfulList<>();
                final NodeIterator it = executeQuery(MessageFormat.format("{0}//element(*, frontend:plugin)[@cpItemsPath]", HippoNodeUtils.resolvePath(primaryType).substring(1)));
                while (it.hasNext()) {
                    final String name = it.nextNode().getName();
                    String namespaceName = MessageFormat.format("{0}{1}", prefix, name);
                    if (compoundMap.containsKey(namespaceName)) {
                        final Compounds compounds = compoundMap.get(namespaceName);
                        keyValueRestfulRestfulList.add(compounds);
                    }
                }

                types.add(new DocumentTypes(HippoNodeUtils.getDisplayValue(session, primaryType), primaryType, keyValueRestfulRestfulList));
            }
        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve document types from repository {}", e);
        }

        return types;
    }

    private NodeIterator executeQuery(String queryString) throws RepositoryException {
        final Session session = GlobalUtils.createSession();
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery(queryString, EssentialConst.XPATH);
        final QueryResult execute = query.execute();
        return execute.getNodes();

    }

    private Map<String, Compounds> getCompoundMap(final ServletContext servletContext) {
        final RestfulList<Compounds> compounds = getCompounds(servletContext);
        Map<String, Compounds> compoundMap = new HashMap<>();
        for (Compounds compound : compounds.getItems()) {
            compoundMap.put(compound.getValue(), compound);
        }
        return compoundMap;
    }

    @GET
    @Path("/compounds")
    public RestfulList<Compounds> getCompounds(@Context ServletContext servletContext) {
        final RestfulList<Compounds> types = new RestfulList<>();
        final Session session = GlobalUtils.createSession();
        try {
            final Set<String> primaryTypes = HippoNodeUtils.getCompounds(session, new HasProviderMatcher());
            for (String primaryType : primaryTypes) {
                types.add(new Compounds(HippoNodeUtils.getDisplayValue(session, primaryType), primaryType, HippoNodeUtils.resolvePath(primaryType)));
            }
        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve document types from repository {}", e);
        }
        //example if empty
        return types;
    }

    @PUT
    @Path("/compounds/create/{name}")
    public MessageRestful createCompound(@PathParam("name") String name, @Context ServletContext servletContext) throws RestException {
        if (Strings.isNullOrEmpty(name)) {
            throw new RestException("Content block name was empty", Response.Status.NOT_ACCEPTABLE);
        }
        final Session session = GlobalUtils.createSession();
        final PluginContext context = getContext(servletContext);
        final RestWorkflow workflow = new RestWorkflow(session, context);
        workflow.addContentBlockCompound(name);
        return new MessageRestful("Successfully created compound with name: " + name);
    }

    @DELETE
    @Path("/compounds/delete/{name}")
    public MessageRestful deleteCompound(@PathParam("name") String name, @Context ServletContext servletContext) throws RestException {
        final Session session = GlobalUtils.createSession();
        final PluginContext context = getContext(servletContext);
        final RestWorkflow workflow = new RestWorkflow(session, context);
        workflow.removeDocumentType(name);
        return new MessageRestful("Document type for name: " + name + " successfully deleted. You'll have to manually delete " + name + " entry from project CND file");
    }


    //see org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentResource.updateContainer()
    @POST
    @Path("/compounds/contentblocks/create")

    public MessageRestful createContentBlocks(CBPayload body, @Context ServletContext servletContext) {
        final List<DocumentTypes> docTypes = body.getItems().getItems();
        final RestWorkflow workflow = new RestWorkflow(GlobalUtils.createSession(), getContext(servletContext));
        for (DocumentTypes documentType : docTypes) {
            final List<KeyValueRestful> providers = documentType.getProviders().getItems();
            if (providers.isEmpty()) {
                log.debug("DocumentType {} had no providers", documentType.getKey());
                continue;
            }
            for (KeyValueRestful item : providers) {
                ContentBlockModel model = new ContentBlockModel(item.getValue(), ContentBlockModel.Prefer.LEFT, ContentBlockModel.Type.LINKS, item.getKey(), documentType.getValue());
                workflow.addContentBlockToType(model);
            }
        }
        return new MessageRestful("Successfully added content blocks");
    }


}
