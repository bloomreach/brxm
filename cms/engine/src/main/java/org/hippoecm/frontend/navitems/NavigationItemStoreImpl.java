/*
 * Copyright 2019-2023 Bloomreach
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
import java.util.function.Predicate;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.frontend.navigation.NavigationItem;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.query.Query.XPATH;

public class NavigationItemStoreImpl implements NavigationItemStore {

    static final String NAVIGATIONITEM_MIXIN = "frontend:navigationitem";
    static final String PLUGIN_PROPERTY_APP_PATH = "frontend:appPath";
    static final String PLUGIN_PROPERTY_PLUGIN_CLASS = "plugin.class";
    public static final String CMS_STATIC_PATH = "/hippo:configuration/hippo:frontend/cms/cms-static";

    private static Logger log = LoggerFactory.getLogger(NavigationItemStoreImpl.class);

    private final Predicate<Node> hasAccess;

    public NavigationItemStoreImpl(Predicate<Node> hasAccess) {
        this.hasAccess = hasAccess;
    }

    /**
     * Collects all navigation items in the following order:
     * <ol>
     * <li>the items in {@value #CMS_STATIC_PATH} in the order of the repository</li>
     * <li>the remaining navitems in arbitrary order</li>
     * </ol>
     *
     * See <a href="https://documentation.bloomreach.com/14/library/concepts/editor-interface/cms-perspectives.html">Create a Custom Perspective.</a>
     *
     * @param session a jcr session.
     * @return All navigation items
     * @throws RepositoryException
     */
    @Override
    public List<NavigationItem> getNavigationItems(Session session) throws RepositoryException {
        final List<NavigationItem> navigationItems = new ArrayList<>();
        final Node cmsStatic = session.getNode(CMS_STATIC_PATH);
        navigationItems.addAll(createList(cmsStatic.getNodes(), this::isNavigationItem));
        navigationItems.addAll(createList(queryAll(session), node -> isNoChildOfCmsStatic(cmsStatic, node)));
        log.debug("navigation items: {}", navigationItems);
        return navigationItems;
    }

    private boolean isNavigationItem(final Node node) {
        try {
            return node.isNodeType(NAVIGATIONITEM_MIXIN);
        } catch (RepositoryException exception) {
            log.warn("Could not determine nodetype of node: { path : {} }", JcrUtils.getNodePathQuietly(node));
        }
        return false;
    }

    private List<NavigationItem> createList(NodeIterator it, Predicate<Node> shouldAdd) throws RepositoryException {
        List<NavigationItem> list = new ArrayList<>();
        while (it.hasNext()) {
            final Node node = it.nextNode();
            if (shouldAdd.test(node)){
                final NavigationItem navigationItem = createNavigationItem(node);
                if (navigationItem != null) {
                    list.add(navigationItem);
                }
            }
        }
        return list;
    }

    private boolean isNoChildOfCmsStatic(final Node cmsStatic, final Node navItemNode){
        try {
            return !cmsStatic.getIdentifier().equals(navItemNode.getParent().getIdentifier());
        } catch (RepositoryException exception) {
            log.warn("Could not determine parent or identifier of node : { path: {} } and/or node: { path : {} }"
                    , JcrUtils.getNodePathQuietly(cmsStatic), JcrUtils.getNodePathQuietly(navItemNode));
        }
        return true;
    }

    @Override
    public NavigationItem getNavigationItem(String pluginClass, Session session) throws RepositoryException {
        final Node navigationItemNode = queryByPluginClass(pluginClass, session);
        return createNavigationItem(navigationItemNode);
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
