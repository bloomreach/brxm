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

package org.onehippo.cms7.essentials.plugin.clonecomponent;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.FileUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.rest.BaseResource;
import org.onehippo.cms7.essentials.dashboard.rest.ErrorMessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;


@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/cloneComponent/")
public class CloneComponentResource extends BaseResource {

    public static final String COMPONENTS_ROOT = "/hst:hst/hst:configurations/hst:default/hst:catalog/essentials-catalog";

    private static final Logger log = LoggerFactory.getLogger(CloneComponentResource.class);
    public static final String HST_TEMPLATE = "hst:template";
    public static final String HST_LABEL = "hst:label";

    @GET
    public List<CloneRestful> runCloneComponent(@Context ServletContext servletContext) {

        final PluginContext context = getContext(servletContext);
        final Session session = context.createSession();
        List<CloneRestful> componentList = new ArrayList<>();
        try {

            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query query = queryManager.createQuery("//hst:hst/hst:configurations//element(*, hst:containeritemcomponent)", "xpath");
            final QueryResult result = query.execute();
            final NodeIterator nodes = result.getNodes();
            while (nodes.hasNext()) {
                final Node componentNode = nodes.nextNode();
                if (componentNode.hasProperty(HST_TEMPLATE)) {
                    componentList.add(new CloneRestful(componentNode.getIdentifier(), componentNode.getName(),
                            componentNode.getProperty(HST_TEMPLATE).getString(), componentNode.getProperty(HST_LABEL).getString()));
                } else {
                    log.warn("Component node does not have a template: {}", componentNode.getPath());
                }

            }
        } catch (RepositoryException e) {
            log.error("Error fetching components", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }
        return componentList;
    }

    @POST
    public MessageRestful cloneComponent(final List<CloneRestful> payload, @Context ServletContext servletContext) {

        final PluginContext context = getContext(servletContext);
        final Session session = context.createSession();
        final StringBuilder builder = new StringBuilder();
        try {

            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final Query query = queryManager.createQuery("//hst:hst/hst:configurations//element(*, hst:template)", "xpath");
            final QueryResult result = query.execute();
            final NodeIterator templateNodes = result.getNodes();
            Map<String, TemplateWrapper> templateMap = new HashMap<>();
            final String webInf = context.getPlaceholderData().get(EssentialConst.PLACEHOLDER_SITE_WEB_INF_ROOT) + File.separator;
            while (templateNodes.hasNext()) {
                final Node node = templateNodes.nextNode();
                if (node.hasProperty("hst:renderpath")) {
                    final String name = node.getName();
                    final String path = node.getProperty("hst:renderpath").getString();
                    final String fullPath = MessageFormat.format("{0}{1}", webInf, path);
                    templateMap.put(name, new TemplateWrapper(fullPath, node));

                }
            }
            for (CloneRestful restful : payload) {
                final Node cloneNode = session.getNodeByIdentifier(restful.getUuid());
                final String templateName = cloneNode.getProperty("hst:template").getString();
                if (Strings.isNullOrEmpty(templateName) || !templateMap.containsKey(templateName)) {
                    log.warn("Template name not found:{}", templateName);
                    continue;
                }
                final Node parent = cloneNode.getParent();
                final Node newNode = parent.addNode(restful.getKey(), "hst:containeritemcomponent");
                newNode.setProperty("hst:label", restful.getLabel());
                newNode.setProperty("hst:template", restful.getValue());


                // copy :
                setProperty(newNode, cloneNode, "hst:componentclassname");
                setProperty(newNode, cloneNode, "hst:iconpath");
                setProperty(newNode, cloneNode, "hst:xtype");
                // duplicate file:
                final TemplateWrapper templateWrapper = templateMap.get(templateName);
                File file = new File(templateWrapper.getPath());
                if (file.exists()) {
                    String destination = MessageFormat.format("{0}{1}{2}", file.getParentFile().getAbsolutePath(), File.separator, restful.getValue());
                    if (file.getAbsolutePath().endsWith("jsp")) {
                        destination = MessageFormat.format("{0}.jsp", destination);
                    } else {
                        destination = MessageFormat.format("{0}.ftl", destination);
                    }
                    FileUtils.copyFile(file, new File(destination));
                    // add template
                    final Node templateParent = session.getNode(templateWrapper.getTemplateNode().getParent().getPath());
                    final Node newTemplate = templateParent.addNode(restful.getValue(), HST_TEMPLATE);
                    newTemplate.setProperty("hst:renderpath", destination.substring(webInf.length(), destination.length()));
                }


                session.save();
                builder.append(restful.getKey());
                builder.append(',');

            }

        } catch (RepositoryException e) {
            log.error("Error cloning component", e);
            return new ErrorMessageRestful("Error cloning components: " + e.getMessage());
        } catch (IOException e) {
            log.error("Error during file copy file", e);

        } finally {
            GlobalUtils.cleanupSession(session);
        }


        return new MessageRestful("Following components created: : " + builder);
    }

    private void setProperty(final Node newNode, final Node cloneNode, final String propertyName) throws RepositoryException {
        if (cloneNode.hasProperty(propertyName)) {
            final Property property = cloneNode.getProperty(propertyName);
            newNode.setProperty(propertyName, property.getValue());
        }
    }


}
