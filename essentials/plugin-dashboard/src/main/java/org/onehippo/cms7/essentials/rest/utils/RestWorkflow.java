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

package org.onehippo.cms7.essentials.rest.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeExistsException;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.api.StringCodecFactory;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.rest.exc.RestException;
import org.onehippo.cms7.essentials.rest.model.contentblocks.ContentBlockModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class RestWorkflow {

    public static final String HIPPOSYSEDIT_NODETYPE = "hipposysedit:nodetype/hipposysedit:nodetype";
    public static final String COMPOUND_TEMPLATE_NAME = "/rest_workflow_content_block_template.xml";
    private static Logger log = LoggerFactory.getLogger(RestWorkflow.class);
    private final Session session;
    private final String namespace;
    private final PluginContext context;

    public RestWorkflow(final Session session, final String namespace, final PluginContext context) {
        this.session = session;
        this.namespace = namespace;
        this.context = context;
    }

    public RestWorkflow(final Session session, final PluginContext context) {
        this.session = session;
        this.context = context;
        this.namespace = context.getProjectNamespacePrefix();

    }

    public boolean addContentBlockCompound(final String name) throws RestException {

        try {
            // register namespace:
            CndUtils.registerDocumentType(context, namespace, name, true, false, "hippo:compound", "hippostd:relaxed");
            final NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
            final Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("namespace", namespace);
            data.put("uri", registry.getURI(namespace));
            final InputStream resourceAsStream = getClass().getResourceAsStream(COMPOUND_TEMPLATE_NAME);
            if (resourceAsStream == null) {
                throw new RestException("Template not found: " + COMPOUND_TEMPLATE_NAME, Response.Status.NO_CONTENT);
            }
            String template = GlobalUtils.readStreamAsText(resourceAsStream);
            log.debug("Processing template:  {}", template);
            template = TemplateUtils.replaceTemplateData(template, data);
            log.debug("After Processing template:  {}", template);
            session.importXML("/hippo:namespaces/" + namespace, IOUtils.toInputStream(template), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
            session.save();
            return true;
        } catch (AccessDeniedException | ConstraintViolationException | LockException | ReferentialIntegrityException | InvalidItemStateException | InvalidSerializedDataException e) {
            log.error("Error in rest workflow: {}", e);
            throw new RestException(e, Response.Status.INTERNAL_SERVER_ERROR);
        } catch (ItemExistsException e) {
            String msg = "Item with name " + namespace + ':' + name + "already exists: ";
            log.error(msg, e);
            throw new RestException(msg, Response.Status.INTERNAL_SERVER_ERROR, e);
        } catch (NamespaceException e) {
            log.error("namespace exception in rest workflow: {}", e);
            throw new RestException("Namespace exception in rest workflow: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        } catch (PathNotFoundException e) {
            log.error("Path not found", e);
            throw new RestException("Path not found", Response.Status.INTERNAL_SERVER_ERROR, e);
        } catch (NoSuchNodeTypeException e) {
            log.error("Node not found in rest workflow: {}", e);
            throw new RestException(e, Response.Status.INTERNAL_SERVER_ERROR);
        } catch (NodeTypeExistsException e) {
            log.error("Error in rest workflow: {}", e);
            throw new RestException("Node already exists: ", Response.Status.INTERNAL_SERVER_ERROR, e);
        } catch (RepositoryException e) {
            log.error("Error in rest workflow: {}", e);
            throw new RestException(e, Response.Status.INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            log.error("Template error in rest workflow: {}", e);
            throw new RestException("Template error in rest workflow: ", Response.Status.INTERNAL_SERVER_ERROR, e);
        }

    }


    /**
     * Removes document type from cnd and also removes document template
     *
     * @param name name of the document type
     * @return true on success, throws exception otherwise
     */
    public boolean removeDocumentType(final String name) {
        try {
            session.getNode("/hippo:namespaces/" + namespace + '/' + name).remove();
            session.save();
            return true;
            //return CndUtils.unRegisterDocumentType(context, namespace, name);
        } catch (Exception e) {
            log.error("Error removing document type", e);
            throw new RestException("Error removing document type: " + name, Response.Status.INTERNAL_SERVER_ERROR, e);
        }


    }


    public boolean addContentBlockToType(final ContentBlockModel contentBlockModel) {
        final String documentType = contentBlockModel.getDocumentType();
        InputStream in = null;

        try {
            Node docType;
            if (documentType.contains(":")) {
                docType = session.getNode("/hippo:namespaces/" + documentType.replace(':', '/'));
            } else {
                docType = session.getNode("/hippo:namespaces/system/" + documentType);
            }

            Node nodeType = null;
            if (docType.hasNode(HIPPOSYSEDIT_NODETYPE)) {
                nodeType = docType.getNode(HIPPOSYSEDIT_NODETYPE);
            }
            if (nodeType == null) {
                throw new RestException("Node " + HIPPOSYSEDIT_NODETYPE + " not found", Response.Status.NOT_FOUND);
            }
            if (docType.hasNode("editor:templates/_default_/root")) {
                final Node ntemplate = docType.getNode("editor:templates/_default_");
                final Node root = docType.getNode("editor:templates/_default_/root");
                ContentBlockModel.PluginType pluginType = null;
                if (root.hasProperty("plugin.class")) {
                    pluginType = ContentBlockModel.PluginType.get(root.getProperty("plugin.class").getString());
                }
                if (pluginType != null) {

                    Map<String, Object> data = new HashMap<>();

                    data.put("name", contentBlockModel.getName());
                    data.put("path", new StringCodecFactory.UriEncoding().encode(contentBlockModel.getName()));
                    data.put("documenttype", documentType);
                    data.put("namespace", documentType.substring(0, documentType.indexOf(':')));
                    data.put("type", contentBlockModel.getType().getType());
                    data.put("provider", contentBlockModel.getProvider());

                    String fieldType = "${cluster.id}.field";

                    if (pluginType.getClazz().equals(ContentBlockModel.PluginType.TWOCOLUMN.getClazz())) {
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

                    String parsed = TemplateUtils.injectTemplate("content_blocks_nodetype.xml", data, getClass());
                    if (parsed == null) {
                        throw new RestException("Error updating content blocks, template with name: content_block_nodetype.xml couldn't be found", Response.Status.NOT_FOUND);

                    }

                    in = new ByteArrayInputStream(parsed.getBytes("UTF-8"));

                    ((HippoSession) session).importDereferencedXML(nodeType.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW,
                            ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_OVERWRITE);

                    parsed = TemplateUtils.injectTemplate(EssentialConst.CONTENT_BLOCKS_TEMPLATE_XML, data, getClass());
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
}
