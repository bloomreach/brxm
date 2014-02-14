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

package org.onehippo.cms7.essentials.tools.rest;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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
import org.onehippo.cms7.essentials.dashboard.rest.KeyValueRestful;
import org.onehippo.cms7.essentials.dashboard.rest.MessageRestful;
import org.onehippo.cms7.essentials.dashboard.rest.NodeRestful;
import org.onehippo.cms7.essentials.dashboard.rest.RestfulList;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
@Produces({MediaType.APPLICATION_JSON})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_FORM_URLENCODED})
@Path("/freemarkersync/")
public class FreemarkerSyncResource extends BaseResource {

    private static final Logger log = LoggerFactory.getLogger(FreemarkerSyncResource.class);

    /**
     * Returns list of all freemarker templates stored on file system
     */
    @GET
    @Path("/")
    public RestfulList<MessageRestful> getTemplateList(@Context ServletContext servletContext) {

        final PluginContext context = getContext(servletContext);
        final RestfulList<MessageRestful> list = new RestfulList<>();

        final File freemarkerDirectory = new File((String) context.getPlaceholderData().get(EssentialConst.PLACEHOLDER_SITE_FREEMARKER_ROOT));
        if (!freemarkerDirectory.exists()) {
            return list;
        }
        final Collection<File> files = FileUtils.listFiles(freemarkerDirectory, EssentialConst.FTL_FILTER, true);
        for (File file : files) {
            list.add(new MessageRestful(file.getPath()));
        }
        return list;


    }

    @POST
    @Path("/repository")
    public MessageRestful writeToRepository(final RestfulList<KeyValueRestful> paths, @Context ServletContext servletContext) {
        final List<KeyValueRestful> items = paths.getItems();
        Session session = null;
        final StringBuilder messageBuilder = new StringBuilder();

        final MessageRestful message = new MessageRestful();
        try {
            final PluginContext context = getContext(servletContext);
            session = context.getSession();
            for (KeyValueRestful item : items) {
                final String nodePath = item.getKey();
                final String filePath = item.getValue();
                final File file = new File(filePath);
                if (!file.exists()) {
                    log.error("File not found for path: {}", filePath);
                    continue;
                }
                final StringBuilder builder = GlobalUtils.readTextFile(file.toPath());
                final String content = builder.toString();
                if (Strings.isNullOrEmpty(content)) {
                    log.error("Content was empty for  {}", item);
                    continue;
                }
                if (session.nodeExists(nodePath)) {
                    final Node node = session.getNode(nodePath);
                    node.setProperty("hst:script", content);
                    session.save();
                    messageBuilder.append(node.getName()).append(";   ");

                }
            }
        } catch (RepositoryException e) {
            log.error("Error fetching node", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }

        final String msg = messageBuilder.toString();
        if (Strings.isNullOrEmpty(msg)) {
            message.setValue("No nodes were updated");
        } else {
            message.setValue("Successfully updated following nodes: " + msg);
        }

        return message;
    }

    /**
     * Writes nodes to files
     *
     * @param paths          list of XPATH entries
     * @param servletContext
     */

    @POST
    @Path("/file")
    public RestfulList<KeyValueRestful> writeToFileSystem(final RestfulList<KeyValueRestful> paths, @Context ServletContext servletContext) {

        final PluginContext context = getContext(servletContext);
        final List<KeyValueRestful> items = paths.getItems();
        final NodeRestful scriptNodes = FSUtils.getScriptNodes(context);
        final Map<String, String> results = new HashMap<>();
        for (KeyValueRestful item : items) {
            final String scriptPath = item.getValue();
            final NodeRestful nodeForPath = scriptNodes.getNodeForPath(scriptPath);
            if (nodeForPath != null) {
                FSUtils.writeScriptNode(context, nodeForPath, results);
            }
        }

        final RestfulList<KeyValueRestful> list = new RestfulList<>();
        for (Map.Entry<String, String> entry : results.entrySet()) {
            list.add(new KeyValueRestful(entry.getKey(), entry.getValue()));
        }
        return list;
    }


}
