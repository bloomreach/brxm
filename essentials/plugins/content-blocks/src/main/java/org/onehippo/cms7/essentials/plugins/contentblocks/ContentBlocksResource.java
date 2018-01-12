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

package org.onehippo.cms7.essentials.plugins.contentblocks;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms7.essentials.plugin.sdk.model.ContentType;
import org.onehippo.cms7.essentials.plugin.sdk.model.UserFeedback;
import org.onehippo.cms7.essentials.plugin.sdk.service.ContentTypeService;
import org.onehippo.cms7.essentials.plugin.sdk.service.JcrService;
import org.onehippo.cms7.essentials.plugin.sdk.service.SettingsService;
import org.onehippo.cms7.essentials.plugins.contentblocks.model.CompoundRestful;
import org.onehippo.cms7.essentials.plugins.contentblocks.model.ContentBlocksFieldRestful;
import org.onehippo.cms7.essentials.plugins.contentblocks.model.DocumentTypeRestful;
import org.onehippo.cms7.essentials.plugins.contentblocks.updater.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("contentblocks")
public class ContentBlocksResource {
    private static final Logger log = LoggerFactory.getLogger(ContentBlocksResource.class);
    private static final String HIPPOSYSEDIT_NODETYPE = "hipposysedit:nodetype/hipposysedit:nodetype";
    private static final String EDITOR_TEMPLATES_NODE = "editor:templates/_default_";
    private static final String ERROR_MSG = "The Content Blocks plugin encountered an error, check the log messages for more info.";
    private static final String NODE_OPTIONS = "cluster.options";
    private static final String PROP_CAPTION = "caption";
    private static final String PROP_COMPOUNDLIST = "compoundList";
    private static final String PROP_MAXITEMS = "maxitems";
    private static final String PROP_PATH = "hipposysedit:path";
    private static final String PROP_PICKERTYPE = "contentPickerType";
    private static final Predicate<ContentType> NO_IMAGE_FILTER
        = type -> !type.getFullName().equals("hippogallery:imageset") && !type.getSuperTypes().contains("hippogallery:imageset");

    // These "compounds" don't have the compoundType flag set internally.
    private static final Set<String> BUILTIN_COMPOUNDS
            = new HashSet<>(Arrays.asList("hippo:mirror", "hippo:resource", "hippostd:html", "hippogallerypicker:imagelink"));

    @Inject private JcrService jcrService;
    @Inject private ContentTypeService contentTypeService;
    @Inject private SettingsService settingsService;

    @GET
    @Path("/")
    public List<DocumentTypeRestful> getContentBlocks() {
        List<ContentType> documents = contentTypeService.fetchContentTypesFromOwnNamespace()
                .stream()
                .filter(NO_IMAGE_FILTER)
                .collect(Collectors.toList());
        List<DocumentTypeRestful> cbDocuments = new ArrayList<>();

        final Session session = jcrService.createSession();
        if (session != null) {
            try {
                for (ContentType documentType : documents) {
                    if ("basedocument".equals(documentType.getName())) {
                        continue; // don't expose the base document as you can't instantiate it.
                    }
                    final String primaryType = documentType.getFullName();
                    final DocumentTypeRestful cbDocument = new DocumentTypeRestful();
                    cbDocument.setId(primaryType);
                    cbDocument.setName(documentType.getDisplayName());
                    populateContentBlocksFields(cbDocument, session);
                    cbDocuments.add(cbDocument);
                }
            } finally {
                jcrService.destroySession(session);
            }
        }
        return cbDocuments;
    }

    /**
     * Process a request to update the content blocks configuration
     *
     * @param docTypes      The per-document type update requests.
     * @param response      for signalling an error.
     * @return              feedback message.
     */
    @POST
    @Path("/")
    public UserFeedback update(List<DocumentTypeRestful> docTypes, @Context HttpServletResponse response) {
        final List<UpdateRequest> updaters = new ArrayList<>();
        int updatersRun = 0;

        final Session session = jcrService.createSession();
        if (session != null) {
            try {
                for (DocumentTypeRestful docType : docTypes) {
                    updateDocumentType(docType, session, updaters);
                }
                session.save();
                updatersRun = executeUpdaters(session, updaters);
            } catch (RepositoryException e) {
                log.warn("Problem saving the JCR changes after updating the content blocks fields.", e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return new UserFeedback().addError(ERROR_MSG);
            } catch (ContentBlocksException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return new UserFeedback().addError(e.getMessage());
            } finally {
                jcrService.destroySession(session);
            }
        }

        String message = "Successfully updated content blocks settings.";
        if (updatersRun > 0) {
            message += " " + updatersRun + " content updater" + (updatersRun > 1 ? "s were" : " was") + " executed"
                    + " to adjust your content. You may want to delete content updaters from the history and use them"
                    + " on other environments, too.";
        }

        return new UserFeedback().addSuccess(message);
    }

    @GET
    @Path("/compounds")
    public List<CompoundRestful> getCompounds() {
        final List<ContentType> compoundTypes = contentTypeService.fetchContentTypes(false)
                .stream()
                .filter(ct -> ct.isCompoundType() || BUILTIN_COMPOUNDS.contains(ct.getFullName()))
                .collect(Collectors.toList());
        final List<CompoundRestful> cbCompounds = new ArrayList<>();

        for (ContentType compoundType : compoundTypes) {
            if ("hippo:compound".equals(compoundType.getFullName())) {
                continue; // don't expose the base compound as you don't want to instantiate it.
            }
            final CompoundRestful cbCompound = new CompoundRestful();
            cbCompound.setId(compoundType.getFullName());
            cbCompound.setName(compoundType.getDisplayName());
            cbCompounds.add(cbCompound);
        }
        return cbCompounds;
    }

    /**
     * For a given document type, check what content blocks fields there are, and build a structure describing those.
     *
     * @param docType representation of the document type.
     * @param session JCR session.
     */
    private void populateContentBlocksFields(final DocumentTypeRestful docType, final Session session) {
        final String primaryType = docType.getId();
        final List<ContentBlocksFieldRestful> contentBlocksFields = new ArrayList<>();
        docType.setContentBlocksFields(new ArrayList<>());
        try {
            final NodeIterator it = findContentBlockFields(primaryType, session);

            while (it.hasNext()) {
                final Node fieldNode = it.nextNode();
                final ContentBlocksFieldRestful field = new ContentBlocksFieldRestful();
                field.setName(fieldNode.getProperty(PROP_CAPTION).getString());
                field.setPickerType(fieldNode.getProperty(PROP_PICKERTYPE).getString());
                if (fieldNode.getNode(NODE_OPTIONS).hasProperty(PROP_MAXITEMS)) {
                    field.setMaxItems(Long.parseLong(fieldNode.getNode(NODE_OPTIONS).getProperty(PROP_MAXITEMS).getString()));
                }
                final String[] compoundNames = fieldNode.getProperty(PROP_COMPOUNDLIST).getString().split(",");
                field.setCompoundRefs(Arrays.asList(compoundNames));
                contentBlocksFields.add(field);
            }
        } catch (RepositoryException e) {
            log.warn("Problem populating content blocks fields for primary type '" + primaryType + "'.", e);
        }
        docType.setContentBlocksFields(contentBlocksFields);
    }

    /**
     * Update the content blocks fields for a specific document type
     *
     * @param docType    document type to adjust
     * @param session    JCR session
     * @throws ContentBlocksException for error message propagation
     */
    private void updateDocumentType(final DocumentTypeRestful docType,
                                    final Session session, final List<UpdateRequest> updaters)
            throws ContentBlocksException {
        final String primaryType = docType.getId();

        // Compare existing content blocks fields with requested ones
        try {
            final NodeIterator it = findContentBlockFields(primaryType, session);
            while (it.hasNext()) {
                final Node fieldNode = it.nextNode();
                final String fieldName = fieldNode.getProperty(PROP_CAPTION).getString();
                boolean updated = false;
                for (ContentBlocksFieldRestful field : docType.getContentBlocksFields()) {
                    if (fieldName.equals(field.getOriginalName())) {
                        updated = true;
                        docType.getContentBlocksFields().remove(field);
                        updateField(fieldNode, docType, field, updaters);
                        // the fieldNode may have been renamed (copied), don't use it anymore!
                        break;
                    }
                }
                if (!updated) {
                    deleteField(fieldNode, docType, fieldName, updaters);
                }
            }
        } catch (RepositoryException e) {
            log.warn("Problem retrieving existing content blocks fields for document type '" + primaryType + "'.", e);
            throw new ContentBlocksException(ERROR_MSG);
        }

        // add new content blocks fields
        for (ContentBlocksFieldRestful field : docType.getContentBlocksFields()) {
            createField(field, docType, session);
        }
    }

    /**
     * Create a new content blocks field.
     *
     * @param field   desired field parameters
     * @param docType document type to extend
     * @param session JCR session
     * @throws ContentBlocksException for error message propagation
     */
    private void createField(final ContentBlocksFieldRestful field,
                             final DocumentTypeRestful docType, final Session session)
            throws ContentBlocksException {
        final String newNodeName = makeNodeName(field.getName());
        final String primaryType = docType.getId();
        final String errorMsg = "Failed to create content blocks field '" + field.getName() + "' for document type '"
                              + docType.getName() + "'.";

        try {
            final Node docTypeNode = getDocTypeNode(session, primaryType);
            final Node nodeTypeNode = getNodeTypeNode(docTypeNode, primaryType);
            final Node editorTemplateNode = getEditorTemplateNode(docTypeNode, primaryType);

            if (editorTemplateNode.hasNode(newNodeName)) {
                throw new ContentBlocksException(
                        "Document type '" + docType.getName() + "' already has field '" + field.getName() + "'.");
            }

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
                    log.error("Can't determine layout of document type " + docType.getName() + ".");
                    throw new ContentBlocksException(errorMsg);
            }

            // Build interpolation map
            Map<String, Object> data = new HashMap<>();

            data.put("name", newNodeName);
            data.put("namespace", settingsService.getSettings().getProjectNamespace());
            data.put("caption", field.getName());
            data.put("pickerType", field.getPickerType());
            data.put("compoundList", makeCompoundList(field));
            data.put("fieldType", fieldType);

            if (!jcrService.importResource(nodeTypeNode, "/content_blocks_nodetype.xml", data)
                    || !jcrService.importResource(editorTemplateNode, "/content_blocks_template.xml", data)) {
                jcrService.refreshSession(session, false);
                throw new ContentBlocksException(errorMsg);
            }

            // Set maxitems
            if (field.getMaxItems() > 0) {
                final Node options = editorTemplateNode.getNode(field.getName() + "/" + NODE_OPTIONS);
                options.setProperty(PROP_MAXITEMS, field.getMaxItems());
            }
        } catch (RepositoryException e) {
            jcrService.refreshSession(session, false);
            log.error("Error in content bocks plugin", e);
            throw new ContentBlocksException(errorMsg);
        }
    }

    /**
     * Update an existing content blocks field
     *
     * We update a field rather than deleting and re-creating it, such that its (adjusted) location, hints and
     * other parameters get preserved.
     *
     * @param oldFieldNode JCR node representing the content blocks field in the document (type) editor
     * @param docType      Document type to update
     * @param field        desired field parameters
     * @throws ContentBlocksException for error message propagation
     */
    private void updateField(final Node oldFieldNode, final DocumentTypeRestful docType,
                             final ContentBlocksFieldRestful field, final List<UpdateRequest> updaters)
            throws ContentBlocksException {
        Node fieldNode = oldFieldNode;
        try {
            final String newNodeName = makeNodeName(field.getName());
            final Node oldNodeTypeNode = getNodeTypeNode(fieldNode);
            Node nodeTypeNode = oldNodeTypeNode;

            if (!newNodeName.equals(fieldNode.getName())) {
                if (fieldNode.getParent().hasNode(newNodeName)) {
                    throw new ContentBlocksException(
                            "Document type '" + docType.getName() + "' already has field '" + field.getName() + "'.");
                }

                final String namespace = settingsService.getSettings().getProjectNamespace();
                final String oldNodePath = nodeTypeNode.getProperty(PROP_PATH).getString();
                final String newNodePath = namespace + ":" + newNodeName;
                nodeTypeNode = JcrUtils.copy(nodeTypeNode, newNodeName, nodeTypeNode.getParent());
                nodeTypeNode.setProperty(PROP_PATH, newNodePath);
                oldNodeTypeNode.remove();

                final String oldNodeCaption = fieldNode.getProperty(PROP_CAPTION).getString();
                fieldNode = JcrUtils.copy(fieldNode, newNodeName, fieldNode.getParent());
                fieldNode.setProperty("field", newNodeName);
                oldFieldNode.remove();

                // schedule updater to fix existing content
                final Map<String, Object> vars = new HashMap<>();
                vars.put("docType", docType.getId());
                vars.put("docName", docType.getName());
                vars.put("oldNodePath", oldNodePath);
                vars.put("oldNodeName", oldNodeCaption);
                vars.put("newNodePath", newNodePath);
                vars.put("newNodeName", field.getName());
                updaters.add(new UpdateRequest("/content-updater.xml", vars));
            }

            fieldNode.setProperty(PROP_CAPTION, field.getName());
            fieldNode.setProperty(PROP_PICKERTYPE, field.getPickerType());
            fieldNode.setProperty(PROP_COMPOUNDLIST, makeCompoundList(field));

            // max items
            final Node clusterOptions = fieldNode.getNode(NODE_OPTIONS);
            if (field.getMaxItems() > 0) {
                clusterOptions.setProperty(PROP_MAXITEMS, field.getMaxItems());
            } else if (clusterOptions.hasProperty(PROP_MAXITEMS)) {
                clusterOptions.getProperty(PROP_MAXITEMS).remove();
            }
        } catch (RepositoryException e) {
            final String msg = "Failed to update content blocks field '" + field.getName() + "' from document type '"
                    + docType.getName() + "'.";
            log.warn(msg, e);
            throw new ContentBlocksException(msg);
        }
    }

    /**
     * Delete an existing content blocks field
     *
     * @param fieldNode   JCR node representing the content blocks field in the document (type) editor
     * @param docType     Document type to update
     * @param fieldName   Name of the to-be deleted field.
     * @throws ContentBlocksException for error message propagation
     */
    private void deleteField(final Node fieldNode, final DocumentTypeRestful docType, final String fieldName,
                             final List<UpdateRequest> updaters) throws ContentBlocksException {
        try {
            final Node nodeTypeNode = getNodeTypeNode(fieldNode);
            final String nodePath = nodeTypeNode.getProperty(PROP_PATH).getString();
            final String nodeName = fieldNode.getProperty(PROP_CAPTION).getString();
            fieldNode.remove();
            nodeTypeNode.remove();

            final Map<String, Object> vars = new HashMap<>();
            vars.put("docType", docType.getId());
            vars.put("docName", docType.getName());
            vars.put("nodePath", nodePath);
            vars.put("nodeName", nodeName);
            updaters.add(new UpdateRequest("/content-deleter.xml", vars));
        } catch (RepositoryException e) {
            final String msg = "Failed to remove content blocks field '" + fieldName + "' from document type '"
                    + docType.getName() + "'.";
            log.warn(msg, e);
            throw new ContentBlocksException(msg);
        }
    }

    private int executeUpdaters(final Session session, final List<UpdateRequest> updaters) {
        int updatersRun = 0;
        try {
            final Node targetNode = session.getNode("/hippo:configuration/hippo:update/hippo:queue");
            for (UpdateRequest updater : updaters) {
                if (jcrService.importResource(targetNode, updater.getResource(), updater.getVars())) {
                    session.save();
                    updatersRun++;
                }
            }
        } catch (RepositoryException e) {
            log.error("Failed retrieving updater queue node.", e);
        }
        return updatersRun;
    }

    private String makeNodeName(final String caption) {
        return NodeNameCodec.encode(caption);
    }

    private String makeCompoundList(final ContentBlocksFieldRestful field) {
        return StringUtils.join(field.getCompoundRefs(), ",");
    }

    private Node getNodeTypeNode(final Node fieldNode) throws RepositoryException {
        final String nodeTypeName = fieldNode.getProperty("field").getString();
        return fieldNode.getParent().getParent().getParent().getNode(HIPPOSYSEDIT_NODETYPE + "/" + nodeTypeName);
    }

    private NodeIterator findContentBlockFields(final String primaryType, final Session session) throws RepositoryException {
        final String queryString = MessageFormat.format("{0}//element(*, frontend:plugin)[@compoundList]",
                                                        contentTypeService.jcrBasePathForContentType(primaryType).substring(1));
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery(queryString, "xpath");
        final QueryResult execute = query.execute();
        return execute.getNodes();
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
}
