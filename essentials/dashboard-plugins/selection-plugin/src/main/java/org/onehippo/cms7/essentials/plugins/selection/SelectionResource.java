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

package org.onehippo.cms7.essentials.plugins.selection;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.ErrorMessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ideas:
 * - sort document types in UI (documents first, compounds last?)
 * - show up-to-date list of selection fields, maybe per document type?
 * - provide UI to create value lists
 * - add more "advanced" options, depending on selection field type
 * - support default value => prototype
 */

@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("selectionplugin")
public class SelectionResource extends BaseResource {

    private static Logger log = LoggerFactory.getLogger(SelectionResource.class);

    @POST
    @Path("/")
    public MessageRestful tickle(final PostPayloadRestful payloadRestful, @Context ServletContext servletContext) {
        // just to make sure the front-end - back-end connection is working...
        log.error("tickling...");
        return null;
    }

    @POST
    @Path("/addfield")
    public MessageRestful addField(final PostPayloadRestful payloadRestful, @Context ServletContext servletContext) {
        final Session session = getContext(servletContext).createSession();

        try {
            return addField(session, payloadRestful.getValues());
        } catch (RepositoryException | IOException e) {
            log.warn("Exception trying to add a selection field to a document type", e);
            return new ErrorMessageRestful("Failed to add new selection field to document type. Check logs.");
        } finally {
            GlobalUtils.cleanupSession(session);
        }
    }

    @GET
    @Path("/fieldsfor/{docType}/")
    public List<SelectionFieldRestful> getSelectionFields(@Context ServletContext servletContext,
                                                          @PathParam("docType") String docType) {
        final List<SelectionFieldRestful> fields = new ArrayList<>();
        final Session session = getContext(servletContext).createSession();

        try {
            addSelectionFields(fields, docType, session);
        } catch (RepositoryException e) {
            log.warn("Exception trying to retrieve selection fields for document type '{}'", docType, e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        return fields;
    }

    /**
     * Add all selection fields of a document type to a list.
     *
     * @param fields  list of selection fields
     * @param docType namespaced name of document type
     * @param session JCR session to read configuration
     * @throws RepositoryException
     */
    private void addSelectionFields(final List<SelectionFieldRestful> fields, final String docType, final Session session)
        throws RepositoryException
    {
        final String[] parts = docType.split(":");
        if (parts.length != 2) {
            log.warn("Unexpected document type '{}'.", docType);
            return ;
        }
        final String nameSpace = parts[0];
        final String docName = parts[1];
        final String docTypeBase = MessageFormat.format("/hippo:namespaces/{0}/{1}/", nameSpace, docName);
        final Node editorTemplate = session.getNode(docTypeBase + "editor:templates/_default_");
        final Node nodeType = session.getNode(docTypeBase + "hipposysedit:nodetype/hipposysedit:nodetype");

        addSingleSelectFields(fields, nameSpace, docName, nodeType, editorTemplate);
        addMultiSelectFields(fields, nameSpace, docName, editorTemplate);
    }

    /**
     * Find all single selection fields of the document type and add them to the list of selection fields.
     *
     * @param fields         list of selection fields
     * @param nameSpace      document type namespace
     * @param documentName   document type name
     * @param nodeType       node type root node
     * @param editorTemplate editor template root node
     * @throws RepositoryException
     */
    private void addSingleSelectFields(final List<SelectionFieldRestful> fields, final String nameSpace,
                                       final String documentName, final Node nodeType, final Node editorTemplate)
        throws RepositoryException
    {
        final NodeIterator children = nodeType.getNodes();
        while (children.hasNext()) {
            final Node child = children.nextNode();

            if (child.hasProperty("hipposysedit:type") && "DynamicDropdown".equals(child.getProperty("hipposysedit:type").getString())) {
                final String fieldName = child.getName();
                final NodeIterator editorFields = editorTemplate.getNodes();
                while (editorFields.hasNext()) {
                    final Node editorField = editorFields.nextNode();
                    if (editorField.hasProperty("field") && fieldName.equals(editorField.getProperty("field").getString())) {

                        final SelectionFieldRestful field = new SelectionFieldRestful();
                        field.setNameSpace(nameSpace);
                        field.setDocumentName(documentName);
                        field.setName(editorField.getProperty("caption").getString());
                        if (editorTemplate.getNode("root").hasProperty("wicket.extensions")) {
                            field.setPosition(editorField.getProperty("wicket.id").getString());
                        }
                        field.setType("single");
                        field.setValueList(editorField.getNode("cluster.options").getProperty("source").getString());
                        fields.add(field);
                        break; // out of the inner loop
                    }
                }
            }
        }
    }

    /**
     * Find all multiple selection fields of the document type and add them to the list of selection fields.
     *
     * @param fields         list of selection fields
     * @param nameSpace      document type namespace
     * @param documentName   document type name
     * @param editorTemplate editor template root node
     * @throws RepositoryException
     */
    private void addMultiSelectFields(final List<SelectionFieldRestful> fields, final String nameSpace,
                                      final String documentName, final Node editorTemplate)
            throws RepositoryException
    {
        final NodeIterator editorFields = editorTemplate.getNodes();
        while (editorFields.hasNext()) {
            final Node editorField = editorFields.nextNode();
            if (editorField.hasNode("valuelist.options")) {
                final SelectionFieldRestful field = new SelectionFieldRestful();
                field.setNameSpace(nameSpace);
                field.setDocumentName(documentName);
                field.setName(editorField.getProperty("caption").getString());
                field.setPosition(editorField.getProperty("wicket.id").getString());
                field.setType("multiple");
                field.setValueList(editorField.getNode("valuelist.options").getProperty("source").getString());
                fields.add(field);
            }
        }
    }

    /**
     * Add a new selection field to a document type.
     *
     * @param session JCR session for persisting the changes
     * @param values  parameters of new selection field (See selectionPlugin.js for keys).
     * @return        message to be sent back to front-end.
     */
    private MessageRestful addField(final Session session, final Map<String, String> values)
            throws RepositoryException, IOException {
        final String docTypeBase = MessageFormat.format("/hippo:namespaces/{0}/{1}/",
                values.get("namespace"), values.get("documentType"));
        final String documentType = values.get("namespace") + ":" + values.get("documentType");

        final Node editorTemplate = session.getNode(docTypeBase + "editor:templates/_default_");
        final Node nodeTypeHandle = session.getNode(docTypeBase + "hipposysedit:nodetype");
        if (nodeTypeHandle.getNodes().getSize() > 1) {
            return new ErrorMessageRestful("Document type '" + documentType + "' is currently being edited in the CMS, "
                                         + "please commit any pending changes before adding a selection field.");
        }
        final Node nodeType = nodeTypeHandle.getNode("hipposysedit:nodetype");

        // Check if the field name is valid. If so, normalize it.
        final String normalized = NodeNameCodec.encode(values.get("fieldName").toLowerCase().replaceAll("\\s", ""));
        values.put("normalizedFieldName", normalized);
        String fieldPosition = values.get("fieldPosition");
        if (!"${cluster.id}.field".equals(fieldPosition)) {
            values.put("fieldPosition", values.get("fieldPosition") + ".item"); // stripped by content type service?
        }

        // Check if the fieldName is already in use
        if (nodeType.hasNode(normalized)
            || editorTemplate.hasNode(normalized)
            || isPropertyNameInUse(nodeType, values.get("namespace"), normalized)) {
            return new ErrorMessageRestful("Field name is already in use for this document type.");
        }

        if ("single".equals(values.get("selectionType"))) {
            importXml("/xml/single-field-editor-template.xml", values, editorTemplate);
            importXml("/xml/single-field-node-type.xml", values, nodeType);
        } else if ("multiple".equals(values.get("selectionType"))) {
            importXml("/xml/multi-field-editor-template.xml", values, editorTemplate);
            importXml("/xml/multi-field-node-type.xml", values, nodeType);
        }
        session.save();

        final String successMessage = MessageFormat.format("Successfully added new selection field {0} to document type {1}.",
                values.get("fieldName"), documentType);
        return new MessageRestful(successMessage);
    }

    /**
     * Check if a certain namespace/fieldname combination is already in use for a certain document type
     *
     * @param nodeType  JCR node representing the hipposysedit:nodetype node.
     * @param namespace JCR namespace for the selected document type.
     * @param fieldName Candidate normalized field name.
     * @return          true if already in use, false otherwise.
     * @throws RepositoryException
     */
    private boolean isPropertyNameInUse(final Node nodeType, final String namespace, final String fieldName)
            throws RepositoryException {
        final NodeIterator fields = nodeType.getNodes();
        final String path = namespace + ":" + fieldName;
        while (fields.hasNext()) {
            final Node field = fields.nextNode();
            if (field.hasProperty("hipposysedit:path") && path.equals(field.getProperty("hipposysedit:path").getString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Import an XML resource into the repository, after mustache-processing.
     *
     * @param resourcePath   path to obtain resource.
     * @param placeholderMap map with placeholder values
     * @param destination    target parent JCR node for the import
     * @throws IOException
     * @throws RepositoryException
     */
    private void importXml(final String resourcePath, final Map<String, String> placeholderMap, final Node destination)
        throws IOException, RepositoryException
    {
        final InputStream stream = getClass().getResourceAsStream(resourcePath);
        final String processedXml = TemplateUtils.replaceStringPlaceholders(GlobalUtils.readStreamAsText(stream), placeholderMap);
        destination.getSession().importXML(destination.getPath(), IOUtils.toInputStream(processedXml),
                ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
    }
}