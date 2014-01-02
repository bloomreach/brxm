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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.ImportUUIDBehavior;
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
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.editor.repository.EditmodelWorkflow;
import org.hippoecm.editor.repository.NamespaceWorkflow;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.api.StringCodecFactory;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.cms7.essentials.dashboard.config.PluginConfigService;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.contentblocks.ContentBlocksPlugin;
import org.onehippo.cms7.essentials.dashboard.contentblocks.matcher.HasProviderMatcher;
import org.onehippo.cms7.essentials.dashboard.contentblocks.model.ContentBlockModel;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.setup.ProjectSetupPlugin;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.rest.model.KeyValueRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.onehippo.cms7.essentials.rest.model.contentblocks.AllDocumentMatcher;
import org.onehippo.cms7.essentials.rest.model.contentblocks.CBPayload;
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
        final PluginContext context = getContext(servletContext);
        final String projectNamespacePrefix = context.getProjectNamespacePrefix();
        String nameSpace = "hippo:namespaces/" + projectNamespacePrefix;
        String prefix = projectNamespacePrefix + ":";

        try {
            final List<String> primaryTypes = HippoNodeUtils.getPrimaryTypes(session, new AllDocumentMatcher(), "new-document");
            final Map<String, Compounds> compoundMap = getCompoundMap(servletContext);

            for (String primaryType : primaryTypes) {
                final RestfulList<KeyValueRestful> keyValueRestfulRestfulList = new RestfulList();
                final NodeIterator it = executeQuery(nameSpace + "//element(*, frontend:plugin)[@contentPickerType]");
                while (it.hasNext()){
                    final String name = it.nextNode().getName();
                    String namespaceName = prefix + name;
                    if(compoundMap.containsKey(namespaceName)){
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
        final Query query = queryManager.createQuery(queryString, Query.XPATH);
        final QueryResult execute = query.execute();
        final NodeIterator nodes = execute.getNodes();
        return nodes;

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
        //types.add(new Compounds("Events document", "namespace:events", "test/test/test"));
        return types;
    }

    @PUT
    @Path("/compounds/create/{name}")
    public KeyValueRestful createCompound(@PathParam("name") String name, @Context ServletContext servletContext) {
        final Session session = GlobalUtils.createSession();
        final PluginContext context = getContext(servletContext);
        final String projectNamespacePrefix = context.getProjectNamespacePrefix();

        String nameSpace = "/hippo:namespaces/" + projectNamespacePrefix;
        String item = "/hippo:namespaces/" + projectNamespacePrefix + "/" + name;

        try {
            if (session.itemExists(item)) {
                throw new RuntimeException("Item exists already");
            }

            if (StringUtils.isNotEmpty(projectNamespacePrefix) && session.itemExists(nameSpace)) {
                final Node namespace = session.getNode(nameSpace);
                final WorkflowManager workflowManager = ((HippoWorkspace) session.getWorkspace()).getWorkflowManager();

                final Workflow editor = workflowManager.getWorkflow("editor", namespace);

                if (editor instanceof NamespaceWorkflow) {
                    final NamespaceWorkflow namespaceWorkflowI = (NamespaceWorkflow) editor;
                    namespaceWorkflowI.addCompoundType(name);
                }
                if (session.nodeExists(item)) {
                    final Node node = session.getNode(item);
                    final Workflow workflow = workflowManager.getWorkflow("default", node);
                    if (workflow instanceof EditmodelWorkflow) {
                        EditmodelWorkflow editmodelWorkflow = (EditmodelWorkflow) workflow;
                        editmodelWorkflow.edit();
                        editmodelWorkflow.commit();
                        final Node protoType = node.getNode("hipposysedit:prototypes/hipposysedit:prototype");
                        protoType.setProperty("cbitem", true);
                        session.save();
                    }
                }
            } else {
                throw new RuntimeException("Namespace doesn't exist");
            }
        } catch (RepositoryException | RemoteException | WorkflowException e) {
            log.error("Exception happened while trying to access the namespace workflow {}", e);
        }

        KeyValueRestful keyValueRestful = new KeyValueRestful(name, item);
        return keyValueRestful;
    }


    public PluginContext getContext(ServletContext servletContext) {
        final String className = ProjectSetupPlugin.class.getName();
        final PluginContext context = new DashboardPluginContext(GlobalUtils.createSession(), getPluginByClassName(className, servletContext));
        final PluginConfigService service = context.getConfigService();

        final ProjectSettingsBean document = service.read(className);
        if (document != null) {
            context.setBeansPackageName(document.getSelectedBeansPackage());
            context.setComponentsPackageName(document.getSelectedComponentsPackage());
            context.setRestPackageName(document.getSelectedRestPackage());
            context.setProjectNamespacePrefix(document.getProjectNamespace());
        }
        return context;
    }

    //see org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerComponentResource.updateContainer()
    @POST
    @Path("/compounds/contentblocks/create")
//    @Consumes("application/json")
    public Response createContentBlocks(CBPayload body, @Context ServletContext servletContext) {
        final List<DocumentTypes> docTypes = body.getItems().getItems();
        for (DocumentTypes documentType : docTypes) {
            for (KeyValueRestful item : documentType.getProviders().getItems()) {
                ContentBlockModel model = new ContentBlockModel(item.getValue(), ContentBlocksPlugin.Prefer.LEFT, ContentBlocksPlugin.Type.LINKS, item.getKey(), documentType.getValue());
                addContentBlockToType(model);
            }
        }
        // final Object o = new Gson().fromJson(body, );
        return Response.status(201).build();
    }


    private boolean addContentBlockToType(final ContentBlockModel contentBlockModel) {
        final String documentType = contentBlockModel.getDocumentType();
        final Session session = GlobalUtils.createSession();
        InputStream in = null;

        try {
            Node docType;
            if (documentType.contains(":")) {
                docType = session.getNode("/hippo:namespaces/" + documentType.replace(':', '/'));
            } else {
                docType = session.getNode("/hippo:namespaces/system/" + documentType);
            }

            Node nodeType = null;
            if (docType.hasNode("hipposysedit:nodetype/hipposysedit:nodetype")) {
                nodeType = docType.getNode("hipposysedit:nodetype/hipposysedit:nodetype");
            }
            if (docType.hasNode("editor:templates/_default_/root")) {
                final Node ntemplate = docType.getNode("editor:templates/_default_");
                final Node root = docType.getNode("editor:templates/_default_/root");
                PluginType pluginType = null;
                if (root.hasProperty("plugin.class")) {
                    pluginType = PluginType.get(root.getProperty("plugin.class").getString());
                }
                if (pluginType != null) {
                    //Load template from source folder
                    /*Template template = cfg.getTemplate("nodetype.xml");
                    Template template2 = cfg.getTemplate("template.xml");*/
                    // Build the data-model
                    Map<String, Object> data = new HashMap<>();

                    data.put("name", contentBlockModel.getName());
                    data.put("path", new StringCodecFactory.UriEncoding().encode(contentBlockModel.getName()));
                    data.put("documenttype", documentType);
                    data.put("namespace", documentType.substring(0, documentType.indexOf(':')));
                    data.put("type", contentBlockModel.getType().getType());
                    data.put("provider", contentBlockModel.getProvider());

                    String fieldType = "${cluster.id}.field";

                    if (pluginType.equals(PluginType.TWOCOLUMN)) {
                        // switch (selected) {
                        //  case LEFT:
                        fieldType = "${cluster.id}.left.item";
                        //      break;
                        ///   case RIGHT:
                        //     fieldType = "${cluster.id}.right.item";
                        //     break;
                        //}
                    }
                    data.put("fieldType", fieldType);

                    String parsed = TemplateUtils.injectTemplate("nodetype.xml", data, getClass());

                    in = new ByteArrayInputStream(parsed.getBytes("UTF-8"));

                    ((HippoSession) session).importDereferencedXML(nodeType.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                            ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE);

                    parsed = TemplateUtils.injectTemplate("template.xml", data, getClass());
                    in = new ByteArrayInputStream(parsed.getBytes("UTF-8"));

                    ((HippoSession) session).importDereferencedXML(ntemplate.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                            ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE);
                    session.save();
                    return true;
                }
            }

        } catch (RepositoryException | IOException e) {
            GlobalUtils.refreshSession(session, false);
            log.error("Error in content bocks plugin", e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        return false;
    }

    public enum Prefer implements Serializable {
        LEFT("left"), RIGHT("right");
        String prefer;

        private Prefer(String prefer) {
            this.prefer = prefer;
        }

        public String getPrefer() {
            return prefer;
        }
    }

    public enum Type implements Serializable {
        LINKS("links"), DROPDOWN("dropdown");
        String type;

        private Type(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    public enum PluginType {

        LISTVIEWPLUGIN("org.hippoecm.frontend.service.render.ListViewPlugin"), TWOCOLUMN("org.hippoecm.frontend.editor.layout.TwoColumn"), UNKNOWN("unknown");
        String clazz;

        PluginType(String clazz) {
            this.clazz = clazz;
        }

        public static PluginType get(String clazz) {
            for (PluginType a : PluginType.values()) {
                if (a.clazz.equals(clazz)) {
                    return a;
                }
            }
            return UNKNOWN;
        }

        public String getClazz() {
            return clazz;
        }

    }


    @POST
    @Path("/compounds/contentblocks/save")
//    @Consumes("application/json")
    public Response saveContentBlocks(CBPayload body, @Context ServletContext servletContext) {
        System.out.println(body);
        //new Gson().fromJson(json, type);
        return Response.status(201).build();
    }

}
