/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.bloomreach.connector.rest;

import java.io.InputStream;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.onehippo.cms7.essentials.plugin.sdk.utils.CndUtils;
import org.onehippo.cms7.essentials.plugin.sdk.utils.GlobalUtils;
import org.onehippo.cms7.essentials.sdk.api.model.rest.UserFeedback;
import org.onehippo.cms7.essentials.sdk.api.service.JcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("bloomreachConnector/")
public class BloomreachConnectorResource {

    private static final Logger log = LoggerFactory.getLogger(BloomreachConnectorResource.class);
    private static final String CRISP_NODE = "crisp:resourceresolvercontainer";

    private final JcrService jcrService;

    @Inject
    public BloomreachConnectorResource(final JcrService jcrService) {
        this.jcrService = jcrService;
    }

    @GET
    public ResourceData index(@Context ServletContext servletContext) throws Exception {
        // check if we have crisp namespace registered:
        final ResourceData resourceData = new ResourceData();
        resourceData.setCrispExists(CndUtils.nodeTypeExists(jcrService, CRISP_NODE));
        return resourceData;
    }


    @POST
    public UserFeedback run(final ResourceData data, @Context ServletContext servletContext) throws Exception {
        log.info("data = {}", data);
        // TODO validate data

        final UserFeedback feedback = new UserFeedback();
        final Session session = jcrService.createSession();
        if (session == null) {
            // set response status
            return feedback.addError("Failed to access JCR repository.");
        }

        try {
            final String basePath = data.getBasePath();
            final Node root = session.getNode(basePath);
            final String resourceName = data.getResourceName();
            if (root.hasNode(resourceName)) {
                return feedback.addSuccess("Resource with name '" + resourceName + "' already exists.");
            }
            final Node node = root.addNode(resourceName, "crisp:resourceresolver");
            final InputStream stream = getClass().getResourceAsStream("/bloomreachResourcetemplate.xml");
            final String xml = GlobalUtils.readStreamAsText(stream);
            node.setProperty("crisp:beandefinition", xml);
            // add names
            node.setProperty("crisp:propnames", new String[]{
                    "bloomreach.account_id",
                    "bloomreach.auth_key",
                    "bloomreach.domain_key",
                    "bloomreach.realm",
                    "bloomreach.fl",
                    "bloomreach.ref_url",
                    "bloomreach.url",
                    "bloomreach.request_id",
                    "bloomreach.baseUrl",
                    "bloomreach.maxEntriesLocalHeap",
                    "bloomreach.maxEntriesLocalDisk",
                    "bloomreach.timeToLiveSeconds",
                    "bloomreach.timeToIdleSeconds"

            });

            // add values
            node.setProperty("crisp:propvalues", new String[]{
                    data.getAccountId(),
                    data.getAuthKey(),
                    data.getDomainKey(),
                    data.getRealm(),
                    data.getFl(),
                    data.getRefUrl(),
                    data.getUrl(),
                    data.getRequestId(),
                    data.getBaseUrl(),
                    String.valueOf(data.getMaxEntriesLocalHeap()),
                    String.valueOf(data.getMaxEntriesLocalDisk()),
                    String.valueOf(data.getTimeToLiveSeconds()),
                    String.valueOf(data.getTimeToIdleSeconds())

            });
            session.save();
            feedback.addSuccess("Successfully created Bloomreach CRSIP resource: " + resourceName);
        } finally {
            jcrService.destroySession(session);
        }
        return feedback;
    }
}
