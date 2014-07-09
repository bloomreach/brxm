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

package org.onehippo.cms7.essentials.plugins.contentblocks;

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

import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.dashboard.rest.exc.RestException;
import org.onehippo.cms7.essentials.plugins.contentblocks.model.RestList;
import org.onehippo.cms7.essentials.plugins.contentblocks.model.contentblocks.AllDocumentMatcher;
import org.onehippo.cms7.essentials.plugins.contentblocks.model.contentblocks.CBPayload;
import org.onehippo.cms7.essentials.plugins.contentblocks.model.contentblocks.Compound;
import org.onehippo.cms7.essentials.plugins.contentblocks.model.contentblocks.ContentBlockModel;
import org.onehippo.cms7.essentials.plugins.contentblocks.model.contentblocks.DocumentType;
import org.onehippo.cms7.essentials.plugins.contentblocks.model.contentblocks.HasProviderMatcher;
import org.onehippo.cms7.essentials.plugins.contentblocks.utils.RestWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
// TODO mm: move this to own directory (as part of the plugin)
@CrossOriginResourceSharing(allowAllOrigins = true)
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("contentblocks")
public class ContentBlocksResource extends BaseResource {
    private static Logger log = LoggerFactory.getLogger(ContentBlocksResource.class);

    @GET
    @Path("/")
    public RestfulList<DocumentType> getControllers(@Context ServletContext servletContext) {
        final RestfulList<DocumentType> types = new RestList<>();
        final Session session = GlobalUtils.createSession();
        final PluginContext context = getContext(servletContext);
        final String projectNamespacePrefix = context.getProjectNamespacePrefix();
        String prefix = projectNamespacePrefix + ':';

        try {
            final List<String> primaryTypes = HippoNodeUtils.getPrimaryTypes(session, new AllDocumentMatcher(), "new-document");
            final Map<String, Compound> compoundMap = getCompoundMap(servletContext);

            for (String primaryType : primaryTypes) {
                final RestList<KeyValueRestful> keyValueRestfulRestfulList = new RestList<>();
                final NodeIterator it = executeQuery(MessageFormat.format("{0}//element(*, frontend:plugin)[@cpItemsPath]",
                        HippoNodeUtils.resolvePath(primaryType).substring(1)), session);
                while (it.hasNext()) {
                    final String name = it.nextNode().getName();
                    String namespaceName = MessageFormat.format("{0}{1}", prefix, name);
                    if (compoundMap.containsKey(namespaceName)) {
                        final Compound compound = compoundMap.get(namespaceName);
                        keyValueRestfulRestfulList.add(compound);
                    }
                }

                types.add(new DocumentType(HippoNodeUtils.getDisplayValue(session, primaryType), primaryType, keyValueRestfulRestfulList));
            }
        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve document types from repository {}", e);
            GlobalUtils.refreshSession(session, false);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        return types;
    }

    private NodeIterator executeQuery(String queryString, final Session session) throws RepositoryException {
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery(queryString, EssentialConst.XPATH);
        final QueryResult execute = query.execute();
        return execute.getNodes();
    }

    private Map<String, Compound> getCompoundMap(final ServletContext servletContext) {
        final RestfulList<Compound> compounds = getCompounds(servletContext);
        Map<String, Compound> compoundMap = new HashMap<>();
        for (Compound compound : compounds.getItems()) {
            compoundMap.put(compound.getValue(), compound);
        }
        return compoundMap;
    }

    @GET
    @Path("/compounds")
    public RestfulList<Compound> getCompounds(@Context ServletContext servletContext) {
        final RestfulList<Compound> types = new RestList<>();
        final Session session = GlobalUtils.createSession();
        try {
            final Set<String> primaryTypes = HippoNodeUtils.getCompounds(session, new HasProviderMatcher());
            for (String primaryType : primaryTypes) {
                types.add(new Compound(HippoNodeUtils.getDisplayValue(session, primaryType), primaryType, HippoNodeUtils.resolvePath(primaryType)));
            }
        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve document types from repository {}", e);
        }finally{
            GlobalUtils.cleanupSession(session);
        }
        //example if empty
        return types;
    }

    @PUT
    @Path("/compounds/create/{name}")
    public MessageRestful createCompound(@PathParam("name") String name, @Context ServletContext servletContext) {
        if (Strings.isNullOrEmpty(name)) {
            throw new RestException("Content block name was empty", Response.Status.NOT_ACCEPTABLE);
        }
        final Session session = GlobalUtils.createSession();
        try {
            final PluginContext context = getContext(servletContext);
            final RestWorkflow workflow = new RestWorkflow(session, context);
            workflow.addContentBlockCompound(name);
            return new MessageRestful("Successfully created compound with name: " + name);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
    }

    @DELETE
    @Path("/compounds/delete/{name}")
    public MessageRestful deleteCompound(@PathParam("name") String name, @Context ServletContext servletContext) {
        final Session session = GlobalUtils.createSession();
        try {

            final PluginContext context = getContext(servletContext);
            final RestWorkflow workflow = new RestWorkflow(session, context);
            workflow.removeDocumentType(name);
            return new MessageRestful("Document type for name: " + name + " successfully deleted. You'll have to manually delete " + name + " entry from project CND file");
        } finally {

            GlobalUtils.cleanupSession(session);
        }
    }


    //see org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentResource.updateContainer()
    @POST
    @Path("/compounds/contentblocks/create")
    public MessageRestful createContentBlocks(CBPayload body, @Context ServletContext servletContext) {
        final List<DocumentType> docTypes = body.getDocumentTypes().getItems();
        final Session session = GlobalUtils.createSession();
        try {
            final RestWorkflow workflow = new RestWorkflow(session, getContext(servletContext));
            for (DocumentType documentType : docTypes) {
                final List<KeyValueRestful> providers = documentType.getProviders().getItems();
                if (providers.isEmpty()) {
                    log.debug("DocumentType {} had no providers", documentType.getKey());
                    // TODO: remove them....
                    continue;
                }
                for (KeyValueRestful item : providers) {
                    ContentBlockModel model = new ContentBlockModel(item.getValue(), ContentBlockModel.Prefer.LEFT, ContentBlockModel.Type.LINKS, item.getKey(), documentType.getValue());
                    workflow.addContentBlockToType(model);
                }
            }
            return new MessageRestful("Successfully updated content blocks settings");
        } finally {
            GlobalUtils.cleanupSession(session);
        }
    }
}
