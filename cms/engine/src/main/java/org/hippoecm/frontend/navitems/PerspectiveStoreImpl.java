/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.frontend.navitems;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.frontend.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.query.Query.XPATH;

public class PerspectiveStoreImpl implements PerspectiveStore {

    private static Logger log = LoggerFactory.getLogger(PerspectiveStoreImpl.class);

    @Override
    public List<String> getPerspectiveClassNames(final Session session) throws RepositoryException {
        final NodeIterator perspectiveNodes = getAllPerspectiveNodes(session);
        return getClassNames(perspectiveNodes);
    }

    private List<String> getClassNames(NodeIterator perspectiveNodes) throws RepositoryException {
        final long size = perspectiveNodes.getSize();
        log.debug("Found {} perspective nodes", size);
        final List<String> perspectiveClassNames = new ArrayList<>((int) size);
        while (perspectiveNodes.hasNext()) {
            final Node perspectiveNode = perspectiveNodes.nextNode();
            if (perspectiveNode.hasProperty(Plugin.CLASSNAME)) {
                final String className = perspectiveNode.getProperty(Plugin.CLASSNAME).getString();
                perspectiveClassNames.add(className);
            } else {
                log.warn("node at path '{}' does not have property '{}', skipping it", perspectiveNode.getPath(), Plugin.CLASSNAME);
            }
        }
        log.debug("Perspective class names: {}", perspectiveClassNames);
        return perspectiveClassNames;
    }

    private NodeIterator getAllPerspectiveNodes(Session userSession) throws RepositoryException {
        final String xpathQuery = "//element(*,frontend:plugin)[@wicket.id = 'service.tab']";
        log.debug("Querying for perspectives: '{}'", xpathQuery);
        final QueryManager queryManager = userSession.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery(xpathQuery, XPATH);
        final QueryResult result = query.execute();
        return result.getNodes();
    }
}
