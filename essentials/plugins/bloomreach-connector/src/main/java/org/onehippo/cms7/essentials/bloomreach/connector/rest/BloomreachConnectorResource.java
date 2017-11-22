/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContextFactory;
import org.onehippo.cms7.essentials.dashboard.model.DependencyRestful;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.ErrorMessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.CndUtils;
import org.onehippo.cms7.essentials.dashboard.utils.DependencyUtils;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;


@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("bloomreachConnector/")
public class BloomreachConnectorResource extends BaseResource {

    private static final Logger log = LoggerFactory.getLogger(BloomreachConnectorResource.class);
    private static final String CRISP_GROUP_ID = "org.onehippo.cms7";
    private static final String ARTIFACT_ID_REPOSITORY = "hippo-addon-crisp-repository";
    private static final String ARTIFACT_ID_API = "hippo-addon-crisp-api";
    private static final String CRISP_NODE = "crisp:resourceresolvercontainer";
    private static final String CRISP_VERSION = "${hippo.addon-crisp.version}";
    @Inject private EventBus eventBus;
    @Inject private PluginContextFactory contextFactory;


    @GET
    public ResourceData index(@Context ServletContext servletContext) throws Exception {
        // check if we have crisp namespace registered and if not if we at least have dependencies in place::
        final PluginContext context = contextFactory.getContext();
        final boolean exists = CndUtils.nodeTypeExists(context, CRISP_NODE);
        final ResourceData resourceData = new ResourceData();
        final DependencyRestful dependency = new DependencyRestful();
        dependency.setTargetPom(TargetPom.CMS.getName());
        dependency.setArtifactId(ARTIFACT_ID_REPOSITORY);
        dependency.setGroupId(CRISP_GROUP_ID);
        final DependencyRestful dependencyApi = new DependencyRestful();
        dependencyApi.setTargetPom(TargetPom.CMS.getName());
        dependencyApi.setArtifactId(ARTIFACT_ID_API);
        dependencyApi.setGroupId(CRISP_GROUP_ID);
        final boolean hasDependency = DependencyUtils.hasDependency(context, dependency)
                && DependencyUtils.hasDependency(context, dependencyApi);
        resourceData.setCrispDependencyExists(hasDependency);
        resourceData.setCrispExists(exists);
        return resourceData;
    }


    @POST
    @Path("/install")
    public RestfulList<MessageRestful> install(final ResourceData data, @Context ServletContext servletContext) throws Exception {
        // check if we have crisp namespace registered:
        final PluginContext context = contextFactory.getContext();
        final RestfulList<MessageRestful> retVal = new RestfulList<>();
        //TODO read dependency from plugin descriptor
        final DependencyRestful dependency = new DependencyRestful();
        dependency.setTargetPom(TargetPom.CMS.getName());
        dependency.setArtifactId(ARTIFACT_ID_REPOSITORY);
        dependency.setGroupId(CRISP_GROUP_ID);
        dependency.setVersion(CRISP_VERSION);
        final DependencyRestful dependencyApi = new DependencyRestful();
        dependencyApi.setTargetPom(TargetPom.CMS.getName());
        dependencyApi.setArtifactId(ARTIFACT_ID_API);
        dependencyApi.setGroupId(CRISP_GROUP_ID);
        dependencyApi.setVersion(CRISP_VERSION);
        boolean added = DependencyUtils.addDependency(context, dependency) &&
                DependencyUtils.addDependency(context, dependencyApi);
        if (added) {
            retVal.add(new MessageRestful("Successfully added dependencies"));
        } else {
            retVal.add(new ErrorMessageRestful("Failed to add dependencies"));
        }
        return retVal;
    }

    @POST
    public RestfulList<MessageRestful> run(final ResourceData data, @Context ServletContext servletContext) throws Exception {
        log.info("data = {}", data);
        // TODO validate data

        final PluginContext context = contextFactory.getContext();
        final Session session = context.createSession();
        final RestfulList<MessageRestful> retVal = new RestfulList<>();
        try {

            final String basePath = data.getBasePath();
            final Node root = session.getNode(basePath);
            final String resourceName = data.getResourceName();
            if (root.hasNode(resourceName)) {
                retVal.add(new MessageRestful("Resource with name" + resourceName + " already exists!"));
                return retVal;
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
            retVal.add(new MessageRestful("Successfully created Bloomreach CRSIP resource: " + resourceName));
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return retVal;
    }


}
