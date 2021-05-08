/*
 * Copyright 2014-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cms7.essentials.sdk.api.model.Module;
import org.onehippo.cms7.essentials.sdk.api.model.rest.UserFeedback;
import org.onehippo.cms7.essentials.sdk.api.service.ContentTypeService;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.onehippo.cms7.essentials.sdk.api.service.ProjectService;
import org.onehippo.cms7.essentials.sdk.api.service.RebuildService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("selectionplugin")
public class SelectionResource {

    private static final Logger log = LoggerFactory.getLogger(SelectionResource.class);
    private static final String MULTISELECT_PLUGIN_CLASS = "org.onehippo.forge.selection.frontend.plugin.DynamicMultiSelectPlugin";
    private static final String VALUELIST_MANAGER_ID = "org.onehippo.forge.selection.hst.manager.ValueListManager";
    private static final String VALUELIST_XPATH = "/beans/beans:bean[@id=\""
            + VALUELIST_MANAGER_ID + "\"]/beans:constructor-arg/beans:map";
    private static final String PLUGIN_CLASS = "plugin.class";
    private static final String CLUSTER_OPTIONS = "cluster.options";
    private static final String SOURCE = "source";

    private final RebuildService rebuildService;
    private final ProjectService projectService;
    private final JcrService jcrService;
    private final ContentTypeService contentTypeService;

    @Inject
    public SelectionResource(final RebuildService rebuildService, final ProjectService projectService,
                             final JcrService jcrService, final ContentTypeService contentTypeService) {
        this.rebuildService = rebuildService;
        this.projectService = projectService;
        this.jcrService = jcrService;
        this.contentTypeService = contentTypeService;
    }

    @POST
    @Path("/addfield")
    public UserFeedback addField(final Map<String, Object> parameters, @Context HttpServletResponse response) {
        final UserFeedback feedback = new UserFeedback();
        final Session session = jcrService.createSession();

        try {
            final int status = addField(session, parameters, feedback);
            if (Response.Status.Family.familyOf(status) != SUCCESSFUL) {
                response.setStatus(status);
            }
        } catch (RepositoryException | IOException e) {
            log.warn("Exception trying to add a selection field to a document type", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            feedback.addError("Failed to add new selection field to document type. Check logs.");
        } finally {
            jcrService.destroySession(session);
        }
        return feedback;
    }

    @GET
    @Path("/fieldsfor/{docType}/")
    public List<SelectionField> getSelectionFields(@PathParam("docType") String docType) {
        final List<SelectionField> fields = new ArrayList<>();
        final Session session = jcrService.createSession();

        try {
            addSelectionFields(fields, docType, session);
        } catch (RepositoryException e) {
            log.warn("Exception trying to retrieve selection fields for document type '{}'", docType, e);
        } finally {
            jcrService.destroySession(session);
        }

        return fields;
    }

    @GET
    @Path("spring")
    public List<ProvisionedValueList> loadProvisionedValueLists() {
        List<ProvisionedValueList> pvlList = new ArrayList<>();
        final Document document = readSpringConfiguration();

        if (document != null) {
            String xPath = VALUELIST_XPATH + "/beans:entry";
            List valueLists = document.selectNodes(xPath);
            Iterator iter = valueLists.iterator();

            while (iter.hasNext()) {
                Element valueList = (Element) iter.next();
                ProvisionedValueList pvl = new ProvisionedValueList();
                pvl.setId(valueList.attributeValue("key"));
                pvl.setPath(valueList.attributeValue("value"));
                pvlList.add(pvl);
            }
        }
        return pvlList;
    }

    @POST
    @Path("spring")
    public UserFeedback storeProvisionedValueLists(final List<ProvisionedValueList> provisionedValueLists,
                                                     @Context HttpServletResponse response) {
        final Document document = readSpringConfiguration();
        if (document == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new UserFeedback().addError("Failure parsing the Spring configuration.");
        }

        Element map = (Element)document.selectSingleNode(VALUELIST_XPATH);
        if (map == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new UserFeedback().addError("Failure locating the relevant piece of Spring configuration.");
        }

        // remove the old value lists
        List<Element> oldValueLists = (List<Element>)map.elements();
        for (Element e : oldValueLists) {
            e.detach();
        }

        // add the new value lists
        for (ProvisionedValueList pvl : provisionedValueLists) {
            Element entry = map.addElement("entry");
            entry.addAttribute("key", pvl.getId());
            entry.addAttribute("value", pvl.getPath());
        }

        try {
            final File springFile = getSpringFile();
            springFile.getParentFile().mkdirs();
            springFile.createNewFile();

            FileOutputStream fos = new FileOutputStream(springFile);
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter(fos, format);
            writer.write(document);
            writer.flush();
        } catch (IOException ex) {
            log.error("Problem writing the Spring configuration", ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new UserFeedback().addError("Failure storing the Spring configuration.");
        }

        rebuildService.requestRebuild("selectionPlugin");
        return new UserFeedback()
                .addSuccess("Successfully updated the Spring configuration")
                .addSuccess("Spring configuration updated, project rebuild needed");
    }

    /**
     * Add all selection fields of a document type to a list.
     *
     * @param fields  list of selection fields
     * @param jcrContentType namespaced name of document type
     * @param session JCR session to read configuration
     * @throws RepositoryException
     */
    private void addSelectionFields(final List<SelectionField> fields, final String jcrContentType, final Session session)
        throws RepositoryException
    {
        final String contentTypeBasePath = contentTypeService.jcrBasePathForContentType(jcrContentType);
        final Node editorTemplate = session.getNode(contentTypeBasePath + "/editor:templates/_default_");
        final Node nodeType = session.getNode(contentTypeBasePath + "/hipposysedit:nodetype/hipposysedit:nodetype");

        addSingleSelectFields(fields, jcrContentType, nodeType, editorTemplate);
        addMultiSelectFields(fields, jcrContentType, editorTemplate);
    }

    /**
     * Find all single selection fields of the document type and add them to the list of selection fields.
     *
     * @param fields         list of selection fields
     * @param jcrContentType JCR content type name
     * @param nodeType       node type root node
     * @param editorTemplate editor template root node
     * @throws RepositoryException
     */
    private void addSingleSelectFields(final List<SelectionField> fields, final String jcrContentType,
                                       final Node nodeType, final Node editorTemplate)
        throws RepositoryException
    {
        final String pluginClass = "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin";
        final String namespace = contentTypeService.extractPrefix(jcrContentType);
        final String shortName = contentTypeService.extractShortName(jcrContentType);
        final NodeIterator children = nodeType.getNodes();
        while (children.hasNext()) {
            final Node child = children.nextNode();

            if (child.hasProperty("hipposysedit:type")) {
                String type = child.getProperty("hipposysedit:type").getString();
                if ("DynamicDropdown".equals(type) || "selection:RadioGroup".equals(type)) {
                    final Node editorField = findCorrespondingEditorTemplateField(child, editorTemplate);
                    if (editorField != null && editorField.hasProperty(PLUGIN_CLASS)
                            && pluginClass.equals(editorField.getProperty(PLUGIN_CLASS).getString())) {

                        final SelectionField field = new SelectionField();
                        field.setType("single");
                        field.setNameSpace(namespace);
                        field.setDocumentName(shortName);
                        field.setName(editorField.getProperty("caption").getString());
                        field.setValueList(editorField.getNode(CLUSTER_OPTIONS).getProperty(SOURCE).getString());
                        fields.add(field);
                    }
                }
            }
        }
    }

    /**
     * @param nodeTypeField  Node representing the field's node type definition
     * @param editorTemplate Node representing the document's editor template
     * @return               Node representing the field's editor template definition, or null.
     * @throws RepositoryException
     */
    private Node findCorrespondingEditorTemplateField(final Node nodeTypeField, final Node editorTemplate)
        throws RepositoryException
    {
        final String fieldName = nodeTypeField.getName();
        final NodeIterator editorFields = editorTemplate.getNodes();
        while (editorFields.hasNext()) {
            final Node editorField = editorFields.nextNode();
            if (editorField.hasProperty("field") && fieldName.equals(editorField.getProperty("field").getString())) {
                return editorField;
            }
        }
        return null;
    }

    /**
     * Find all multiple selection fields of the document type and add them to the list of selection fields.
     *
     * @param fields         list of selection fields
     * @param jcrContentType JCR content type name
     * @param editorTemplate editor template root node
     * @throws RepositoryException
     */
    private void addMultiSelectFields(final List<SelectionField> fields, final String jcrContentType, final Node editorTemplate)
            throws RepositoryException
    {
        final String namespace = contentTypeService.extractPrefix(jcrContentType);
        final String shortName = contentTypeService.extractShortName(jcrContentType);
        final NodeIterator editorFields = editorTemplate.getNodes();
        while (editorFields.hasNext()) {
            final Node editorField = editorFields.nextNode();
            if (editorField.hasNode(CLUSTER_OPTIONS) && editorField.getNode(CLUSTER_OPTIONS).hasProperty(SOURCE) &&
                    editorField.hasProperty(PLUGIN_CLASS) && MULTISELECT_PLUGIN_CLASS.equals(editorField.getProperty(PLUGIN_CLASS).getString())) {
                final SelectionField field = new SelectionField();
                field.setType("multiple");
                field.setNameSpace(namespace);
                field.setDocumentName(shortName);
                field.setName(editorField.getProperty("caption").getString());
                field.setValueList(editorField.getNode(CLUSTER_OPTIONS).getProperty(SOURCE).getString());
                fields.add(field);
            }
        }
    }

    /**
     * Add a new selection field to a document type.
     *
     * @param session JCR session for persisting the changes
     * @param parameters  parameters of new selection field (See selectionPlugin.js for keys).
     * @return        message to be sent back to front-end.
     */
    private int addField(final Session session, final Map<String, Object> parameters, final UserFeedback feedback)
            throws RepositoryException, IOException {
        final String jcrContentType = (String) parameters.get("jcrContentType");
        final String namespace = contentTypeService.extractPrefix(jcrContentType);
        parameters.put("namespace", namespace);

        final String contentTypeBackPath = contentTypeService.jcrBasePathForContentType(jcrContentType);
        final Node editorTemplate = session.getNode(contentTypeBackPath + "/editor:templates/_default_");
        final Node nodeTypeHandle = session.getNode(contentTypeBackPath + "/hipposysedit:nodetype");
        if (nodeTypeHandle.getNodes().getSize() > 1) {
            feedback.addError("Document type '" + jcrContentType + "' is currently being edited in the CMS, "
                    + "please commit any pending changes before adding a selection field.");
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        final Node nodeType = nodeTypeHandle.getNode("hipposysedit:nodetype");

        // Check if the field name is valid. If so, normalize it.
        final String fieldName = (String) parameters.get("fieldName");
        final String normalized = NodeNameCodec.encode(fieldName.toLowerCase().replaceAll("\\s", ""));
        parameters.put("normalizedFieldName", normalized);

        // Check if the fieldName is already in use
        if (nodeType.hasNode(normalized)
            || editorTemplate.hasNode(normalized)
            || isPropertyNameInUse(nodeType, namespace, normalized)) {
            feedback.addError("Field name '" + normalized + "' is already in use for this document type.");
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        // Put the new field to the default location
        parameters.put("fieldPosition", contentTypeService.determineDefaultFieldPosition(jcrContentType));

        String presentationType = "DynamicDropdown";
        if ("single".equals(parameters.get("selectionType"))) {
            if ("radioboxes".equals(parameters.get("presentation"))) {
                presentationType = "selection:RadioGroup";
            }
            parameters.put("presentationType", presentationType);

            jcrService.importResource(editorTemplate, "/xml/single-field-editor-template.xml", parameters);
            jcrService.importResource(nodeType, "/xml/single-field-node-type.xml", parameters);
        } else if ("multiple".equals(parameters.get("selectionType"))) {
            jcrService.importResource(editorTemplate, "/xml/multi-field-editor-template.xml", parameters);
            jcrService.importResource(nodeType, "/xml/multi-field-node-type.xml", parameters);
        }
        session.save();

        final String successMessage = String.format("Successfully added new selection field '%s' to document type '%s'.",
                fieldName, jcrContentType);
        feedback.addSuccess(successMessage);
        return HttpServletResponse.SC_CREATED;
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

    private Document readSpringConfiguration() {
        final File springFile = getSpringFile();

        try (InputStream is = openSpringConfiguration(springFile)) {
            if (is != null) {
                Map<String, String> namespaceUris = new HashMap<>();
                namespaceUris.put("beans", "http://www.springframework.org/schema/beans");

                DocumentFactory factory = new DocumentFactory();
                factory.setXPathNamespaceURIs(namespaceUris);

                SAXReader reader = new SAXReader();
                reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                reader.setDocumentFactory(factory);
                return reader.read(is);
            }
        } catch (DocumentException | IOException | SAXException e) {
            log.error("Problem parsing Spring configuration file.", e);
        }
        return null;
    }

    private InputStream openSpringConfiguration(final File springFile) {
        if (springFile != null && springFile.exists() && springFile.isFile()) {
            try {
                return new FileInputStream(springFile);
            } catch (FileNotFoundException ex) {
                log.error("Problem reading Spring configuration file.", ex);
                return null;
            }
        } else {
            // no Spring configuration present yet, use template.
            final String path = "/xml/valuelistmanager.xml";
            return getClass().getResourceAsStream(path);
        }
    }

    private File getSpringFile() {
        return projectService.getResourcesRootPathForModule(Module.SITE_COMPONENTS)
                .resolve("META-INF").resolve("hst-assembly").resolve("overrides").resolve("valueListManager.xml").toFile();
    }
}