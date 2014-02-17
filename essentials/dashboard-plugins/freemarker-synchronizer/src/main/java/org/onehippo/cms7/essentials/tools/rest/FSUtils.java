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
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.io.FileUtils;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.rest.NodeRestful;
import org.onehippo.cms7.essentials.dashboard.rest.PropertyRestful;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.dashboard.utils.HippoNodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public final class FSUtils {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("/hst:templates/");
    private static Logger log = LoggerFactory.getLogger(FSUtils.class);

    public static NodeRestful getScriptNodes(final PluginContext context) {
        final NodeRestful restful = new NodeRestful(true);
        Session session = null;
        try {
            session = context.getSession();
            final QueryManager queryManager = session.getWorkspace().getQueryManager();
            final String q = "//element(*, hst:template)[@hst:script]";
            final Query query = queryManager.createQuery(q, "xpath");
            final QueryResult result = query.execute();
            final NodeIterator nodes = result.getNodes();
            while (nodes.hasNext()) {
                final Node node = nodes.nextNode();
                final NodeRestful nodeRestful = new NodeRestful(node.getName(), node.getPath());
                HippoNodeUtils.populateProperties(node, nodeRestful);
                restful.addNode(nodeRestful);
            }
        } catch (RepositoryException e) {
            log.error("Error getting script nodes", e);
        } finally {
            GlobalUtils.cleanupSession(session);
        }


        return restful;


    }


    public static Map<String, String> writeFreemarkerFiles(final PluginContext context, final String freemarkerPath, final NodeRestful restful) {
        final Map<String, String> nodeFileMappings = new HashMap<>();
        final List<NodeRestful> nodes = restful.getNodes();
        for (NodeRestful node : nodes) {
            writeScriptNode(context, freemarkerPath, node, nodeFileMappings);
        }

        return nodeFileMappings;

    }

    public static void writeScriptNode(final PluginContext context, final String freemarkerPath, final NodeRestful node, final Map<String, String> nodeFileMappings) {
        final String path = node.getPath();
        if (path == null) {
            log.error("Path was null for node {}", node);
            return;
        }
        final String[] paths = TEMPLATE_PATTERN.split(path);
        if (paths.length != 2) {
            log.error("Invalid path, cannot extract Freemarker path {}", path);
            return;
        }
        final PropertyRestful property = node.getProperty("hst:script");
        if (property == null || property.getValue() == null) {
            log.error("Script node or it's value was null: {}", path);
            return;
        }
        final String value = property.getValue();


        try {
            final File dir = ensureDirs(freemarkerPath, context);
            final String fileName = MessageFormat.format("{0}{1}{2}{3}", dir.getAbsolutePath(), File.separator, node.getName(), ".ftl");
            final File file = new File((fileName));
            if (!file.exists()) {
                log.info("Creating file: {}", file);
                final boolean created = file.createNewFile();
                if (!created) {
                    log.error("Failed to create file {}", fileName);
                    return;
                }
            }
            GlobalUtils.writeToFile(value, file.toPath());
            nodeFileMappings.put(node.getPath(), file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Error writing script content", e);
        }
    }

    private static File ensureDirs(final String freemarkerPath, final PluginContext context) throws IOException {
        final File dir = new File(freemarkerPath + File.separator + context.getProjectNamespacePrefix());
        FileUtils.forceMkdir(dir);
        return dir;

    }


    private FSUtils() {
    }
}
