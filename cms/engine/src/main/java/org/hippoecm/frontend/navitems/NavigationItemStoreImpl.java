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
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.frontend.navigation.NavigationItem;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.query.Query.XPATH;

public class NavigationItemStoreImpl implements NavigationItemStore {

    static final String NAVIGATIONITEM_MIXIN = "frontend:navigationitem";
    static final String PLUGIN_PROPERTY_APP_PATH = "frontend:appPath";
    static final String PLUGIN_PROPERTY_PLUGIN_CLASS = "plugin.class";

    private static Logger log = LoggerFactory.getLogger(NavigationItemStoreImpl.class);

    private final Predicate<Node> hasAccess;

    public NavigationItemStoreImpl(Predicate<Node> hasAccess) {
        this.hasAccess = hasAccess;
    }

    @Override
    public List<NavigationItem> getNavigationItems(Session session) throws RepositoryException {
        final NodeIterator perspectiveNodes = queryAll(session);
        final int size = (int) perspectiveNodes.getSize();
        log.debug("Found {} nav item nodes", size);
        return getNavigationItems(perspectiveNodes);
    }

    @Override
    public NavigationItem getNavigationItem(String pluginClass, Session session) throws RepositoryException {
        final Node navigationItemNode = queryByPluginClass(pluginClass, session);
        return createNavigationItem(navigationItemNode);
    }

    private List<NavigationItem> getNavigationItems(NodeIterator perspectiveNodes) throws RepositoryException {
        final List<NavigationItem> navigationItems = new ArrayList<>();
        while (perspectiveNodes.hasNext()) {
            final Node navItemNode = perspectiveNodes.nextNode();
            final NavigationItem navigationItem = createNavigationItem(navItemNode);
            if (navigationItem != null) {
                navigationItems.add(navigationItem);
            }
        }
        log.debug("navigation items: {}", navigationItems);
        return navigationItems;
    }

    private NavigationItem createNavigationItem(Node navItemNode) throws RepositoryException {
        if (navItemNode == null || !hasAccess.test(navItemNode)) {
            return null;
        }
        final NavigationItem navigationItem = new NavigationItem();
        final String appPath = navItemNode.getProperty(PLUGIN_PROPERTY_APP_PATH).getString();
        navigationItem.setId(String.format("xm-%s", appPath));
        navigationItem.setAppPath(appPath);
        return navigationItem;
    }

    private NodeIterator queryAll(Session userSession) throws RepositoryException {
        final String xpathQuery = String.format("//element(*, %s)", NAVIGATIONITEM_MIXIN);
        final QueryResult result = getQueryResult(userSession, xpathQuery);
        return result.getNodes();
    }

    private Node queryByPluginClass(String pluginClass, Session session) throws RepositoryException {
        final String xpathQuery = String.format("//element(*, %s) [%s = '%s']", NAVIGATIONITEM_MIXIN, PLUGIN_PROPERTY_PLUGIN_CLASS, pluginClass);
        final NodeIterator nodeIterator = getQueryResult(session, xpathQuery).getNodes();
        if (nodeIterator.getSize() > 1) {
            throw new IllegalArgumentException("query '" + xpathQuery + "' should return at most one node");
        }
        if (nodeIterator.hasNext()) {
            return nodeIterator.nextNode();
        }
        return null;
    }

    private QueryResult getQueryResult(Session session, String xpathQuery) throws RepositoryException {
        log.debug("Executing query '{}'", xpathQuery);
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery(xpathQuery, XPATH);
        return query.execute();
    }
}
