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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.*;

import javax.jcr.*;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.rs.security.cors.CrossOriginResourceSharing;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.api.StringCodecFactory;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.*;
import org.onehippo.cms7.essentials.plugins.contentblocks.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

@CrossOriginResourceSharing(allowAllOrigins = true)
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("contentblocks")
public class ContentBlocksResource extends BaseResource {
    private static final String HIPPOSYSEDIT_NODETYPE = "hipposysedit:nodetype/hipposysedit:nodetype";
    private static final String EDITOR_TEMPLATES_NODE = "editor:templates/_default_";
    private static final String ERROR_MSG = "The Content Blocks plugin encountered an error, check the log messages for more info.";
    private static Logger log = LoggerFactory.getLogger(ContentBlocksResource.class);

    /**
     * Retrieve all document types and their content block providers
     *
     * @return data structure representing the current state.
     */
    @GET
    @Path("/")
    public ContentBlocksRestful getContentBlocks() {
        final List<DocumentTypeRestful> docTypes = new ArrayList<>();
        final PluginContext context = PluginContextFactory.getContext();
        final String projectNamespacePrefix = context.getProjectNamespacePrefix();

        final Session session = GlobalUtils.createSession();
        try {
            final Map<String, ProviderRestful> providerMap = getProviderMap();
            final List<String> primaryTypes = HippoNodeUtils.getPrimaryTypes(session, new JcrMatcher() {
                @Override
                public boolean matches(Node node) throws RepositoryException {
                    return true;
                }
            }, "new-document");

            for (String primaryType : primaryTypes) {
                final DocumentTypeRestful docType = new DocumentTypeRestful();
                final List<ProviderActionRestful> providerActions = new ArrayList<>();
                docType.setName(primaryType);
                docType.setTranslatedName(HippoNodeUtils.getDisplayValue(session, primaryType));
                docType.setProviderActions(providerActions);

                // detect the doc type's provider fields
                final NodeIterator it = findContentBlockFields(
                        MessageFormat.format("{0}//element(*, frontend:plugin)[@cpItemsPath]",
                                             HippoNodeUtils.resolvePath(primaryType).substring(1)), session);
                while (it.hasNext()) {
                    final Node fieldNode = it.nextNode();
                    final String name = MessageFormat.format("{0}:{1}", projectNamespacePrefix, fieldNode.getName());
                    if (providerMap.containsKey(name)) {
                        final ProviderActionRestful providerAction = new ProviderActionRestful();
                        providerAction.setName(name);
                        providerActions.add(providerAction);
                    }
                }

                docTypes.add(docType);
            }
        } catch (RepositoryException e) {
            log.error("Exception while trying to retrieve document types from repository {}", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        final ContentBlocksRestful contentBlocks = new ContentBlocksRestful();
        contentBlocks.setDocumentTypes(docTypes);
        return contentBlocks;
    }

    /**
     * Process a request to update the content blocks configuration
     *
     * @param contentBlocks The per-document type update requests.
     * @param response      for signalling an error.
     * @return              feedback message.
     */
    @POST
    @Path("/update")
    public MessageRestful save(ContentBlocksRestful contentBlocks, @Context HttpServletResponse response) {
        final Session session = GlobalUtils.createSession();

        try {
            for (DocumentTypeRestful docType : contentBlocks.getDocumentTypes()) {
                for (ProviderActionRestful providerAction : docType.getProviderActions()) {
                    if (providerAction.isAdd()) {
                        addProvider(session, docType.getName(), providerAction.getName());
                    } else {
                        removeProvider(session, docType.getName(), providerAction.getName());
                    }
                }
            }
        } catch (ContentBlocksException e) {
            return createErrorMessage(e.getMessage(), response);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        return new MessageRestful("Successfully updated content blocks settings");
    }

    /**
     * Retrieve a list of all content blocks providers.
     *
     * @return data structure representing the current state.
     */
    @GET
    @Path("/providers")
    public RestfulList<ProviderRestful> getProviders() {
        final RestfulList<ProviderRestful> providers = new RestList<>();
        final Session session = GlobalUtils.createSession();
        try {
            final Set<String> primaryTypes = HippoNodeUtils.getCompounds(session, new JcrMatcher() {
                @Override
                public boolean matches(final Node prototypeNode) throws RepositoryException {
                    return (prototypeNode.hasProperty("cbitem") && prototypeNode.getProperty("cbitem").getBoolean());
                }
            });
            for (String primaryType : primaryTypes) {
                final ProviderRestful provider = new ProviderRestful();
                provider.setName(primaryType);
                provider.setTranslatedName(HippoNodeUtils.getDisplayValue(session, primaryType));
                provider.setRepositoryPath(HippoNodeUtils.resolvePath(primaryType));
                providers.add(provider);
            }
        } catch (RepositoryException e) {
            log.error("Failed to retrieve providers", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return providers;
    }

    /**
     * Create a new empty provider compound.
     *
     * This is currently irreversible; we can't unregister a "document" type from the CND.
     *
     * @param name     name of the new provider
     * @param response for signalling an error.
     * @return         feedback message.
     */
    @PUT
    @Path("/providers/{name}")
    public MessageRestful createProvider(@PathParam("name") String name, @Context HttpServletResponse response) {
        final Session session = GlobalUtils.createSession();
        try {
            createProvider(session, name);
        } catch (ContentBlocksException e) {
            return createErrorMessage(e.getMessage(), response);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return new MessageRestful("Successfully created provider with name: " + name);
    }

    private NodeIterator findContentBlockFields(String queryString, final Session session) throws RepositoryException {
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery(queryString, EssentialConst.XPATH);
        final QueryResult execute = query.execute();
        return execute.getNodes();
    }

    private Map<String, ProviderRestful> getProviderMap() {
        final RestfulList<ProviderRestful> providers = getProviders();
        Map<String, ProviderRestful> providerMap = new HashMap<>();
        for (ProviderRestful provider : providers.getItems()) {
            providerMap.put(provider.getName(), provider);
        }
        return providerMap;
    }

    private void addProvider(final Session session, final String docTypeName, final String providerName)
            throws ContentBlocksException {
        final String providerFieldName = providerName.substring(providerName.indexOf(':') + 1);
        final String errorMsg = "Failed to add provider " + providerName + " to document type " + docTypeName + ".";
        InputStream in = null;

        try {
            final Node docTypeNode = getDocTypeNode(session, docTypeName);
            final Node nodeTypeNode = getNodeTypeNode(docTypeNode, docTypeName);
            final Node editorTemplateNode = getEditorTemplateNode(docTypeNode, docTypeName);

            // Determine document type layout
            String fieldType;
            final String pluginClass = editorTemplateNode.getNode("root").getProperty("plugin.class").getString();
            switch (pluginClass) {
                case "org.hippoecm.frontend.service.render.ListViewPlugin":
                    fieldType = "${cluster.id}.field";
                    break;
                case "org.hippoecm.frontend.editor.layout.TwoColumn":
                    fieldType = "${cluster.id}.left.item";
                    break;
                default:
                    log.error("Can't determine layout of document type " + docTypeName + ".");
                    throw new ContentBlocksException(errorMsg);
            }

            // Build interpolation map
            Map<String, Object> data = new HashMap<>();

            data.put("name", providerFieldName);
            data.put("path", createFieldName(providerFieldName));
            data.put("documenttype", docTypeName);
            data.put("namespace", docTypeName.substring(0, docTypeName.indexOf(':')));
            data.put("type", "links");
            data.put("provider", providerName);
            data.put("fieldType", fieldType);

            // Import nodetype
            String parsed = TemplateUtils.injectTemplate("content_blocks_nodetype.xml", data, getClass());
            if (parsed == null) {
                log.error("Can't read resource 'content_blocks_nodetype.xml'.");
                throw new ContentBlocksException(errorMsg);
            }
            in = new ByteArrayInputStream(parsed.getBytes("UTF-8"));
            ((HippoSession)session).importDereferencedXML(nodeTypeNode.getPath(), in,
                    ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                    ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE,
                    ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE);

            // Import editor template
            parsed = TemplateUtils.injectTemplate("content_blocks_template.xml", data, getClass());
            if (parsed == null) {
                log.error("Can't read resource 'content_blocks_template.xml'.");
                throw new ContentBlocksException(errorMsg);
            }
            in = new ByteArrayInputStream(parsed.getBytes("UTF-8"));
            ((HippoSession)session).importDereferencedXML(editorTemplateNode.getPath(), in,
                    ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                    ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE,
                    ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE);

            session.save();
        } catch (RepositoryException | IOException e) {
            GlobalUtils.refreshSession(session, false);
            log.error("Error in content bocks plugin", e);
            throw new ContentBlocksException(errorMsg);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    private void removeProvider(final Session session, final String docTypeName, final String providerName)
            throws ContentBlocksException {
        final String fieldName = createFieldName(providerName.substring(providerName.indexOf(':') + 1));
        final String errorMsg = "Failed to remove provider " + providerName + " to document type " + docTypeName + ".";

        try {
            final Node docTypeNode = getDocTypeNode(session, docTypeName);
            final Node nodeTypeNode = getNodeTypeNode(docTypeNode, docTypeName);
            if (nodeTypeNode.hasNode(fieldName)) {
                nodeTypeNode.getNode(fieldName).remove();
            }

            final Node editorTemplateNode = getEditorTemplateNode(docTypeNode, docTypeName);
            if (editorTemplateNode.hasNode(fieldName)) {
                editorTemplateNode.getNode(fieldName).remove();
            }

            session.save();
        } catch (RepositoryException e) {
            GlobalUtils.refreshSession(session, false);
            log.error(errorMsg, e);
            throw new ContentBlocksException(errorMsg);
        }
    }

    private String createFieldName(final String providerName) {
        return new StringCodecFactory.UriEncoding().encode(providerName);
    }

    private Node getDocTypeNode(final Session session, final String docTypeName) throws ContentBlocksException {
        try {
            if (docTypeName.contains(":")) {
                return session.getNode("/hippo:namespaces/" + docTypeName.replace(':', '/'));
            } else {
                return session.getNode("/hippo:namespaces/system/" + docTypeName);
            }
        } catch (RepositoryException e) {
            log.error("Problem retrieving the document type node for '" + docTypeName + "'.", e);
            throw new ContentBlocksException(ERROR_MSG);
        }
    }

    private Node getNodeTypeNode(final Node docTypeNode, final String docTypeName) throws ContentBlocksException {
        try {
            return docTypeNode.getNode(HIPPOSYSEDIT_NODETYPE);
        } catch (RepositoryException e) {
            log.error("Document type " + docTypeName + " is missing nodetype node'.");
            throw new ContentBlocksException(ERROR_MSG);
        }
    }

    private Node getEditorTemplateNode(final Node docTypeNode, final String docTypeName) throws ContentBlocksException {
        try {
            return docTypeNode.getNode(EDITOR_TEMPLATES_NODE);
        } catch (RepositoryException e) {
            log.error("Document type " + docTypeName + " is missing editor template node.");
            throw new ContentBlocksException(ERROR_MSG);
        }
    }

    private void createProvider(final Session session, final String name) throws ContentBlocksException {
        if (Strings.isNullOrEmpty(name)) {
            throw new ContentBlocksException("Invalid (empty) provider name.");
        }
        final PluginContext context = PluginContextFactory.getContext();
        final String namespace = context.getProjectNamespacePrefix();
        final String errorMsg = "Error creating provider '" + name + "'. Check log messages for more information.";
        try {
            CndUtils.registerDocumentType(context, namespace, name, true, false, "hippo:compound", "hippostd:relaxed");

            final NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
            final Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("namespace", namespace);
            data.put("uri", registry.getURI(namespace));

            final String parsed = TemplateUtils.injectTemplate("provider_template.xml", data, getClass());
            if (parsed == null) {
                log.error("Template 'provider_template.xml' not found.");
                throw new ContentBlocksException(errorMsg);
            }
            session.importXML("/hippo:namespaces/" + namespace, IOUtils.toInputStream(parsed),
                    ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
            session.save();
        } catch (NodeTypeExistsException e) {
            final String msg = "Node type " + namespace + ':' + name + " already exists.";
            log.debug(msg, e);
            throw new ContentBlocksException(msg);
        } catch (ItemExistsException e) {
            final String msg = "Item with name " + namespace + ':' + name + " already exists.";
            log.debug(msg, e);
            throw new ContentBlocksException(msg);
        } catch (RepositoryException | IOException e) {
            log.error("Error adding compound type: {}", e);
            throw new ContentBlocksException(errorMsg);
        }
    }
}
