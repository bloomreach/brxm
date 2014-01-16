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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.TemplateUtils;
import org.onehippo.cms7.essentials.rest.exc.RestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class RestWorkflow {

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


        } catch (RepositoryException | IOException e) {
            log.error("Error in rest workflow: {}", e);
            throw new RestException(e.getCause().getMessage() + ", " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }

    }


}
