/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hippoecm.hst.plugins.frontend.editor.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstContext implements IDetachable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(HstContext.class);

    private static final String HST_SITE = "hst:site";
    private static final String HST_CONFIGURATION = "hst:configuration";

    public static final String HST_CONTENT_PREFIX = "hst:content";

    private JcrNodeModel root;

    public HstSiteContext site;
    public HstSitemapContext sitemap;
    public HstSitemenuContext sitemenu;
    public HstTemplateContext template;
    public HstPageContext page;
    public HstComponentContext component;
    public HstContentContext content;
    public HstConfigurationContext config;

    private List<HstModelContext> models;
    String namespacesRoot;

    public HstContext(JcrNodeModel baseModel, String namespacesRoot) {
        root = baseModel;
        this.namespacesRoot = namespacesRoot;
        try {

            site = new HstSiteContext(root);
            config = new HstConfigurationContext(site.getModel());
            content = new HstContentContext(site.getModel());

            sitemenu = new HstSitemenuContext(config.getModel());
            sitemap = new HstSitemapContext(config.getModel());
            template = new HstTemplateContext(config.getModel());
            page = new HstPageContext(config.getModel());
            component = new HstComponentContext(config.getModel());

            models = Arrays.asList(new HstModelContext[] { site, config, content, sitemap, template, page, component });
        } catch (RepositoryException e) {
            log.error("Error instantiating HstContext", e);
            throw new IllegalStateException("Error instantiating HstContext");
        }
    }

    public JcrNodeModel getRoot() {
        return root;
    }

    /**
     * Currently only checks if node isn't the root of any of the context nodes
     * 
     * @param model The node model to be checked
     * @return Returns true when node can be safely removed
     */
    public boolean isDeleteAllowed(JcrNodeModel model) {
        for (HstModelContext m : models) {
            if (model.equals(m)) {
                return false;
            }
        }
        return true;
    }

    public void detach() {
        root.detach();
        for (IDetachable c : models) {
            c.detach();
        }
    }

    private abstract class HstModelContext implements IDetachable {
        private static final long serialVersionUID = 1L;

        JcrNodeModel model;

        public HstModelContext(JcrNodeModel model) throws RepositoryException {
            init(model);
            if (this.model == null) {
                throw new IllegalStateException("Model is null after init");
            }
        }

        public JcrNodeModel getModel() {
            return model;
        }

        public String getPath() {
            return model.getItemModel().getPath();
        }

        public String relativePath(String path) {
            String root = getPath();
            if (path == null) {
                return "";
            }

            if (path.length() < root.length() || !path.startsWith(root)) {
                log.warn("Node " + path + " is not a child of " + root);
                return "";
            }
            try {
                Session session = model.getNode().getSession();
                if (session.itemExists(path)) {
                    return path.substring(root.length() + 1);
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            return "";
        }

        /**
         * Return an absolute path of relative path
         * @param path
         * @return
         */
        public String absolutePath(String path) {
            String root = getPath();
            if (path == null) {
                return root;
            }
            if (path.startsWith("/")) {
                return checkExists(path, root);
            }
            return checkExists(root + "/" + path, root);
        }

        private String checkExists(String path, String fallback) {
            try {
                Session session = model.getNode().getSession();
                if (session.itemExists(path)) {
                    return path;
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
            return fallback;
        }

        public void detach() {
            if (model != null) {
                model.detach();
            }
        }

        public abstract void init(JcrNodeModel model) throws RepositoryException;
    }

    public abstract class HstNamespaceContext extends HstModelContext {
        private static final long serialVersionUID = 1L;

        private String namespace;

        public HstNamespaceContext(JcrNodeModel model, String namespace) throws RepositoryException {
            super(model);
            this.namespace = namespace;
        }

        public String getNamespace() {
            return namespacesRoot + "/" + namespace;
        }

    }

    public class HstSiteContext extends HstModelContext {
        private static final long serialVersionUID = 1L;

        public HstSiteContext(JcrNodeModel model) throws RepositoryException {
            super(model);
        }

        @Override
        public void init(JcrNodeModel model) throws RepositoryException {
            for (NodeIterator it = model.getNode().getNodes(); it.hasNext();) {
                Node child = it.nextNode();
                if (child.getPrimaryNodeType().isNodeType(HST_SITE)) {
                    this.model = new JcrNodeModel(child);
                    break;
                }
            }
        }
    }

    public class HstSitemapContext extends HstNamespaceContext {

        private static final long serialVersionUID = 1L;

        public static final String NODENAME = "hst:sitemap";

        public static final String ANY_MATCHER = "**";
        public static final String ANY_MATCHER_ENCODED = "_any_";
        public static final String WILDCARD_MATCHER = "*";
        public static final String WILDCARD_MATCHER_ENCODED = "_default_";
        public static final String MATCHER_SEPERATOR = ".";

        public static final String PAGE_PROPERTY_PREFIX = "hst:pages/";

        public HstSitemapContext(JcrNodeModel model) throws RepositoryException {
            super(model, "sitemapitem");
        }

        @Override
        public void init(JcrNodeModel model) throws RepositoryException {
            this.model = new JcrNodeModel(model.getNode().getNode(NODENAME));
        }

        public String encodeMatcher(String matcher) {
            int index = matcher.indexOf('.');
            if (index > -1) {
                return encode(matcher.substring(0, index)) + MATCHER_SEPERATOR + encode(matcher.substring(index + 1));
            }
            return encode(matcher);
        }

        private String encode(String s) {
            if (s.equals(ANY_MATCHER)) {
                return ANY_MATCHER_ENCODED;
            } else if (s.equals(WILDCARD_MATCHER)) {
                return WILDCARD_MATCHER_ENCODED;
            }
            return s;
        }

        public String decodeMatcher(String matcher) {
            int index = matcher.indexOf('.');
            if (index > -1) {
                return decode(matcher.substring(0, index)) + MATCHER_SEPERATOR + decode(matcher.substring(index + 1));
            }
            return decode(matcher);
        }

        private String decode(String s) {
            if (s.equals(ANY_MATCHER_ENCODED)) {
                return ANY_MATCHER;
            } else if (s.equals(WILDCARD_MATCHER_ENCODED)) {
                return WILDCARD_MATCHER;
            }
            return s;
        }

        public String decodeContentPath(String contentPath) {
            if (contentPath != null) {
                int index = contentPath.indexOf("${");
                if (index > 0) {
                    contentPath = contentPath.substring(0, index);
                }
            }
            return contentPath;
        }

        public String encodePage(String page) {
            return PAGE_PROPERTY_PREFIX + page;
        }

        public String decodePage(String selectedPage) {
            if (selectedPage.startsWith(PAGE_PROPERTY_PREFIX)) {
                return selectedPage.substring(PAGE_PROPERTY_PREFIX.length());
            }
            return selectedPage;
        }

    }

    public class HstSitemenuContext extends HstNamespaceContext {
        private static final long serialVersionUID = 1L;

        public static final String HST_SITEMENUS = "hst:sitemenus";

        public HstSitemenuContext(JcrNodeModel model) throws RepositoryException {
            super(model, "sitemenuitem");
        }

        @Override
        public void init(JcrNodeModel model) throws RepositoryException {
            this.model = new JcrNodeModel(model.getNode().getNode(HST_SITEMENUS));
        }

        public String decodeReferenceName(String name) {
            if (name.startsWith("hst:sitemenus/")) {
                return name.substring("hst:sitemenus/".length() + 1);
            }
            return "";
        }

        public String encodeReferenceName(String name) {
            return "hst:sitemenus/" + name;
        }

        public String getParentNamespace() {
            return namespacesRoot + "/sitemenu";
        }

    }

    public class HstComponentContext extends HstNamespaceContext {
        private static final long serialVersionUID = 1L;

        public static final String HST_COMPONENTS = "hst:components";

        public HstComponentContext(JcrNodeModel model) throws RepositoryException {
            super(model, "component");
        }

        @Override
        public void init(JcrNodeModel model) throws RepositoryException {
            this.model = new JcrNodeModel(model.getNode().getNode(HST_COMPONENTS));
        }

        //Strip of hst:components/
        public String decodeReferenceName(String name) {
            if (name.startsWith(HST_COMPONENTS + "/")) {
                return name.substring(HST_COMPONENTS.length() + 1);
            }
            return "";
        }

        public String encodeReferenceName(String name) {
            return HST_COMPONENTS + "/" + name;
        }

        public List<String> getReferenceables(boolean recursive) {
            return getReferenceables(getModel().getNode(), recursive);
        }

        private List<String> getReferenceables(Node parent, boolean recursive) {
            List<String> refs = new ArrayList<String>();
            try {
                for (NodeIterator it = parent.getNodes(); it.hasNext();) {
                    Node child = it.nextNode();
                    String relPath = relativePath(child.getPath());
                    refs.add(encodeReferenceName(relPath));

                    if (recursive) {
                        refs.addAll(getReferenceables(child, recursive));
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error loading list of hst:component", e);
            }
            return refs;
        }
    }

    public class HstTemplateContext extends HstNamespaceContext {
        private static final long serialVersionUID = 1L;

        public static final String HST_TEMPLATES = "hst:templates";

        public HstTemplateContext(JcrNodeModel model) throws RepositoryException {
            super(model, "template");
        }

        @Override
        public void init(JcrNodeModel model) throws RepositoryException {
            this.model = new JcrNodeModel(model.getNode().getNode(HST_TEMPLATES));
        }

        public List<String> getTemplatesAsList() {
            List<String> templates = new ArrayList<String>();
            try {
                for (NodeIterator it = getModel().getNode().getNodes(); it.hasNext();) {
                    Node child = it.nextNode();
                    templates.add(child.getName());
                }
            } catch (RepositoryException e) {
                log.error("Error loading hst:templates", e);
            }
            return templates;
        }
    }

    public class HstPageContext extends HstNamespaceContext {
        private static final long serialVersionUID = 1L;

        public static final String HST_PAGES = "hst:pages";

        public HstPageContext(JcrNodeModel model) throws RepositoryException {
            super(model, "page");
        }

        @Override
        public void init(JcrNodeModel model) throws RepositoryException {
            this.model = new JcrNodeModel(model.getNode().getNode(HST_PAGES));
        }

        public ArrayList<String> getPagesAsList() {
            ArrayList<String> pages = new ArrayList<String>();
            try {
                for (NodeIterator it = getModel().getNode().getNodes(); it.hasNext();) {
                    Node child = it.nextNode();
                    pages.add(child.getName());
                }
            } catch (RepositoryException e) {
                log.error("Error loading hst:pages into list", e);
            }
            return pages;
        }

        //Strip of hst:pages/
        public String decodeReferenceName(String name) {
            if (name.startsWith(HST_PAGES + "/")) {
                return name.substring(HST_PAGES.length() + 1);
            }
            return "";
        }

        public String encodeReferenceName(String name) {
            return HST_PAGES + "/" + name;
        }

        public List<String> getReferenceables() {
            List<String> refs = new ArrayList<String>();
            try {
                for (NodeIterator it = getModel().getNode().getNodes(); it.hasNext();) {
                    Node child = it.nextNode();
                    String relPath = relativePath(child.getPath());
                    refs.add(encodeReferenceName(relPath));
                }
            } catch (RepositoryException e) {
                log.error("Error loading list of hst:component", e);
            }
            return refs;
        }

    }

    public class HstContentContext extends HstModelContext {
        private static final long serialVersionUID = 1L;

        private static final String HST_CONTENT = "hst:content";

        public HstContentContext(JcrNodeModel model) throws RepositoryException {
            super(model);
        }

        @Override
        public void init(JcrNodeModel model) throws RepositoryException {
            if (!model.getNode().hasNode(HST_CONTENT)) {
                throw new IllegalArgumentException("Could not find node " + HST_CONTENT);
            }
            this.model = new JcrNodeModel(model.getNode().getNode(HST_CONTENT));
        }
    }

    public class HstConfigurationContext extends HstModelContext {
        private static final long serialVersionUID = 1L;

        public HstConfigurationContext(JcrNodeModel model) throws RepositoryException {
            super(model);
        }

        @Override
        public void init(JcrNodeModel model) throws RepositoryException {
            if (!model.getNode().hasNode(HST_CONFIGURATION)) {
                throw new IllegalArgumentException("Facet select " + HST_CONFIGURATION + " not found");
            }
            Node root = model.getNode().getNode(HST_CONFIGURATION);
            if (!root.hasNode(HST_CONFIGURATION)) {
                throw new IllegalArgumentException("Node " + HST_CONFIGURATION + " not found");
            }
            HippoNode hroot = (HippoNode) root.getNode(HST_CONFIGURATION);
            root = hroot.getCanonicalNode();
            this.model = new JcrNodeModel(root);
        }

    }
}
