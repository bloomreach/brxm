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

import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.editor.repository.impl.NamespaceWorkflowImpl;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.cms7.essentials.dashboard.contentblocks.matcher.HasProviderMatcher;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.rest.model.KeyValueRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.onehippo.cms7.essentials.rest.model.contentblocks.Compounds;
import org.onehippo.cms7.essentials.rest.model.contentblocks.DocumentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/documenttypes/")
public class DocumentTypeResource extends BaseResource {

    @Inject
    private EventBus eventBus;
    private static Logger log = LoggerFactory.getLogger(DocumentTypeResource.class);


    @GET
    @Path("/")
    public RestfulList<DocumentTypes> getControllers(@Context ServletContext servletContext) {
        final RestfulList<DocumentTypes> types = new RestfulList<>();
        // TODO implement

        final Session session = GlobalUtils.createSession();
        try {
            final List<String> primaryTypes = HippoNodeUtils.getPrimaryTypes(session, new HasProviderMatcher(), "new-document");
            for (String primaryType : primaryTypes) {
                final RestfulList<KeyValueRestful> keyValueRestfulRestfulList = new RestfulList();
                types.add(new DocumentTypes(HippoNodeUtils.getDisplayValue(session, primaryType), primaryType, keyValueRestfulRestfulList));
            }
        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve document types from repository {}", e);
        }

        //example  if empty
        final RestfulList<KeyValueRestful> keyValueRestfulRestfulList = new RestfulList();
        keyValueRestfulRestfulList.add(new KeyValueRestful("Provider 1", "Provider 1"));
        keyValueRestfulRestfulList.add(new KeyValueRestful("Provider 2", "Provider 2"));
        types.add(new DocumentTypes("News document", "namespace:news", keyValueRestfulRestfulList));
        return types;
    }

    @GET
    @Path("/compounds")
    public RestfulList<Compounds> getCompounds(@Context ServletContext servletContext) {
        final RestfulList<Compounds> types = new RestfulList<>();
        // TODO implement

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
        types.add(new Compounds("Events document", "namespace:events", "test/test/test"));
        return types;
    }

    @PUT
    @Path("/compounds/create/{name}")
    public KeyValueRestful createCompound(@PathParam("name") String name, @Context ServletContext servletContext) {
        KeyValueRestful keyValueRestful = new KeyValueRestful(name, "path" + name);

        final Session session = GlobalUtils.createSession();
        //GlobalUtils
        final PluginContext context = new DashboardPluginContext(GlobalUtils.createSession(), null);
        final String projectNamespacePrefix = context.getProjectNamespacePrefix();
        boolean success = false;
        //todo rg.apache.cxf.interceptor.Fault: com.sun.proxy.$Proxy32 cannot be cast to org.hippoecm.editor.repository.impl.NamespaceWorkflowImpl
        try {
            if (session.itemExists("/hippo:namespaces/mydemoessentials")) {
                final Node namespace = session.getNode("/hippo:namespaces/mydemoessentials");
                final WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();
                final Workflow editor = workflowManager.getWorkflow("editor", namespace);
                //if (editor instanceof NamespaceWorkflow) {
                System.out.println(editor.getClass().getMethods());
                System.out.println(editor);
                if (editor instanceof NamespaceWorkflow) {
                    final NamespaceWorkflowImpl namespaceWorkflowI = (NamespaceWorkflowImpl) editor;
                    namespaceWorkflowI.addCompoundType(name);
                    if (session.itemExists("/hippo:namespaces/mydemoessentials/" + name)) {
                        success = true;
                    }
                }


                //}
            }
        } catch (RepositoryException e) {
            log.error("", e);
        } catch (RemoteException e) {
            log.error("", e);
        } catch (WorkflowException e) {
            log.error("", e);
        }

        return keyValueRestful;
    }

}
