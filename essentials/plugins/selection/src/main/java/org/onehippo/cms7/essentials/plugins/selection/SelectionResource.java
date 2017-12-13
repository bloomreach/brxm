/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.jcr.ImportUUIDBehavior;
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

import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.event.RebuildEvent;
import org.onehippo.cms7.essentials.dashboard.model.UserFeedback;
import org.onehippo.cms7.essentials.dashboard.rest.PostPayloadRestful;
import org.onehippo.cms7.essentials.dashboard.utils.DocumentTemplateUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.ProjectUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("selectionplugin")
public class SelectionResource {

    private static final Logger log = LoggerFactory.getLogger(SelectionResource.class);
    private static final String MULTISELECT_PLUGIN_CLASS = "org.onehippo.forge.selection.frontend.plugin.DynamicMultiSelectPlugin";
    private static final String VALUELIST_MANAGER_ID = "org.onehippo.forge.selection.hst.manager.ValueListManager";
    private static final String VALUELIST_XPATH = "/beans/beans:bean[@id=\""
            + VALUELIST_MANAGER_ID + "\"]/beans:constructor-arg/beans:map";

    @Inject private EventBus eventBus;
    @Inject private PluginContextFactory contextFactory;

    @POST
    @Path("/addfield")
    public UserFeedback addField(final PostPayloadRestful payloadRestful, @Context HttpServletResponse response) {
        final Session session = contextFactory.getContext().createSession();
        final UserFeedback feedback = new UserFeedback();

        try {
            final int status = addField(session, payloadRestful.getValues(), feedback);
            if (status >= HttpServletResponse.SC_MULTIPLE_CHOICES) {
                response.setStatus(status);
            }
        } catch (RepositoryException | IOException e) {
            log.warn("Exception trying to add a selection field to a document type", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            feedback.addError("Failed to add new selection field to document type. Check logs.");
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return feedback;
    }

    @GET
    @Path("/fieldsfor/{docType}/")
    public List<SelectionFieldRestful> getSelectionFields(@PathParam("docType") String docType) {
        final List<SelectionFieldRestful> fields = new ArrayList<>();
        final Session session = contextFactory.getContext().createSession();

        try {
            addSelectionFields(fields, docType, session);
        } catch (RepositoryException e) {
            log.warn("Exception trying to retrieve selection fields for document type '{}'", docType, e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        return fields;
    }

    @GET
    @Path("spring")
    public List<ProvisionedValueList> loadProvisionedValueLists() {
        List<ProvisionedValueList> pvlList = new ArrayList<>();
        final PluginContext context = contextFactory.getContext();
        final Document document = readSpringConfiguration(context);

        String xPath = VALUELIST_XPATH + "/beans:entry";
        List valueLists = document.selectNodes(xPath);
        Iterator iter = valueLists.iterator();

        while (iter.hasNext()) {
            Element valueList = (Element)iter.next();
            ProvisionedValueList pvl = new ProvisionedValueList();
            pvl.setId(valueList.attributeValue("key"));
            pvl.setPath(valueList.attributeValue("value"));
            pvlList.add(pvl);
        }
        return pvlList;
    }

    @POST
    @Path("spring")
    public UserFeedback storeProvisionedValueLists(final List<ProvisionedValueList> provisionedValueLists,
                                                     @Context HttpServletResponse response) {
        final PluginContext context = contextFactory.getContext();
        final Document document = readSpringConfiguration(context);
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
            final File springFile = getSpringFile(context);
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

        final String message = "Spring configuration updated, project rebuild needed";
        eventBus.post(new RebuildEvent("selectionPlugin", message));

        return new UserFeedback().addSuccess("Successfully updated the Spring configuration");
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
        final String pluginClass = "org.hippoecm.frontend.editor.plugins.field.PropertyFieldPlugin";
        final NodeIterator children = nodeType.getNodes();
        while (children.hasNext()) {
            final Node child = children.nextNode();

            if (child.hasProperty("hipposysedit:type")) {
                String type = child.getProperty("hipposysedit:type").getString();
                if ("DynamicDropdown".equals(type) || "selection:RadioGroup".equals(type)) {
                    final Node editorField = findCorrespondingEditorTemplateField(child, editorTemplate);
                    if (editorField != null && editorField.hasProperty("plugin.class")
                            && pluginClass.equals(editorField.getProperty("plugin.class").getString())) {

                        final SelectionFieldRestful field = new SelectionFieldRestful();
                        field.setType("single");
                        field.setNameSpace(nameSpace);
                        field.setDocumentName(documentName);
                        field.setName(editorField.getProperty("caption").getString());
                        field.setValueList(editorField.getNode("cluster.options").getProperty("source").getString());
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
            if (editorField.hasNode("cluster.options") && editorField.getNode("cluster.options").hasProperty("source") &&
                    editorField.hasProperty("plugin.class") && MULTISELECT_PLUGIN_CLASS.equals(editorField.getProperty("plugin.class").getString())) {
                final SelectionFieldRestful field = new SelectionFieldRestful();
                field.setType("multiple");
                field.setNameSpace(nameSpace);
                field.setDocumentName(documentName);
                field.setName(editorField.getProperty("caption").getString());
                field.setValueList(editorField.getNode("cluster.options").getProperty("source").getString());
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
    private int addField(final Session session, final Map<String, String> values, final UserFeedback feedback)
            throws RepositoryException, IOException {
        final String docTypeBase = MessageFormat.format("/hippo:namespaces/{0}/{1}/",
                values.get("namespace"), values.get("documentType"));
        final String documentType = values.get("namespace") + ':' + values.get("documentType");

        final Node editorTemplate = session.getNode(docTypeBase + "editor:templates/_default_");
        final Node nodeTypeHandle = session.getNode(docTypeBase + "hipposysedit:nodetype");
        if (nodeTypeHandle.getNodes().getSize() > 1) {
            feedback.addError("Document type '" + documentType + "' is currently being edited in the CMS, "
                    + "please commit any pending changes before adding a selection field.");
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        final Node nodeType = nodeTypeHandle.getNode("hipposysedit:nodetype");

        // Check if the field name is valid. If so, normalize it.
        final String normalized = NodeNameCodec.encode(values.get("fieldName").toLowerCase().replaceAll("\\s", ""));
        values.put("normalizedFieldName", normalized);

        // Check if the fieldName is already in use
        if (nodeType.hasNode(normalized)
            || editorTemplate.hasNode(normalized)
            || isPropertyNameInUse(nodeType, values.get("namespace"), normalized)) {
            feedback.addError("Field name '" + normalized + "' is already in use for this document type.");
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }

        // Put the new field to the default location
        values.put("fieldPosition", DocumentTemplateUtils.getDefaultPosition(editorTemplate));

        String presentationType = "DynamicDropdown";
        if ("single".equals(values.get("selectionType"))) {
            if ("radioboxes".equals(values.get("presentation"))) {
                presentationType = "selection:RadioGroup";
            }
            values.put("presentationType", presentationType);

            importXml("/xml/single-field-editor-template.xml", values, editorTemplate);
            importXml("/xml/single-field-node-type.xml", values, nodeType);
        } else if ("multiple".equals(values.get("selectionType"))) {
            importXml("/xml/multi-field-editor-template.xml", values, editorTemplate);
            importXml("/xml/multi-field-node-type.xml", values, nodeType);
        }
        session.save();

        final String successMessage = MessageFormat.format("Successfully added new selection field {0} to document type {1}.",
                values.get("fieldName"), documentType);
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

        destination.getSession().importXML(destination.getPath(),
                IOUtils.toInputStream(processedXml, StandardCharsets.UTF_8),
                ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
    }

    private Document readSpringConfiguration(final PluginContext context) {
        final File springFile = getSpringFile(context);
        InputStream is = null;
        if (springFile !=null && springFile.exists() && springFile.isFile()) {
            try {
                is = new FileInputStream(springFile);
            } catch (FileNotFoundException ex) {
                log.error("Problem reading Spring configuration file.", ex);
            }
        } else {
            // no Spring configuration present yet, use template.
            final String path = "/xml/valuelistmanager.xml";
            is = getClass().getResourceAsStream(path);
        }

        if (is != null) {
            try {
                Map<String, String> namespaceUris = new HashMap<>();
                namespaceUris.put("beans", "http://www.springframework.org/schema/beans");

                DocumentFactory factory = new DocumentFactory();
                factory.setXPathNamespaceURIs(namespaceUris);

                SAXReader reader = new SAXReader();
                reader.setDocumentFactory(factory);
                return reader.read(is);
            } catch (DocumentException ex) {
                log.error("Problem parsing Spring configuration file.", ex);
            }
        }
        return null;
    }

    private File getSpringFile(final PluginContext context) {
        final String baseDir = GlobalUtils.decodeUrl(ProjectUtils.getBaseProjectDirectory());
        if (Strings.isNullOrEmpty(baseDir)) {
            return null;
        }
        String springFilePath = new StringBuilder()
                .append(baseDir)
                .append(File.separator)
                .append(context.getProjectSettings().getSiteModule())
                .append(File.separator)
                .append("src")
                .append(File.separator)
                .append("main")
                .append(File.separator)
                .append("resources")
                .append(File.separator)
                .append("META-INF")
                .append(File.separator)
                .append("hst-assembly")
                .append(File.separator)
                .append("overrides")
                .append(File.separator)
                .append("valueListManager.xml").toString();

        return new File(springFilePath);
    }
}