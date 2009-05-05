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
    private static final String HST_SITE = "hst:site";

    private static final String HST_CONFIGURATION = "hst:configuration";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(HstContext.class);

    public static final String HST_CONTENT_PREFIX = "hst:content";

    private JcrNodeModel root;

    public HstSiteContext site;
    public HstSitemapContext sitemap;
    public HstTemplateContext template;
    public HstPageContext page;
    public HstComponentContext component;
    public HstContentContext content;
    public HstConfigurationContext config;

    private List<HstModelContext> models;

    public HstContext(JcrNodeModel baseModel) {
        //hier komt binnen /preview
        root = baseModel;
        try {

            site = new HstSiteContext(root);
            config = new HstConfigurationContext(site.getModel());
            content = new HstContentContext(site.getModel());

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

    public boolean canDeleteNode(JcrNodeModel model) {
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

    public class HstSitemapContext extends HstModelContext {
        private static final long serialVersionUID = 1L;

        public static final String NODENAME = "hst:sitemap";

        public static final String ANY_MATCHER = "**";
        public static final String ANY_MATCHER_ENCODED = "_any_";
        public static final String WILDCARD_MATCHER = "*";
        public static final String WILDCARD_MATCHER_ENCODED = "_default_";

        public static final String PAGE_PROPERTY_PREFIX = "hst:pages/";

        public HstSitemapContext(JcrNodeModel model) throws RepositoryException {
            super(model);
        }

        @Override
        public void init(JcrNodeModel model) throws RepositoryException {
            this.model = new JcrNodeModel(model.getNode().getNode(NODENAME));
        }

        public String encodeMatcher(String matcher) {
            if (matcher.equals(ANY_MATCHER)) {
                return ANY_MATCHER_ENCODED;
            } else if (matcher.equals(WILDCARD_MATCHER)) {
                return WILDCARD_MATCHER_ENCODED;
            }
            return matcher;
        }

        public String decodeMatcher(String matcher) {
            if (matcher.equals(ANY_MATCHER_ENCODED)) {
                return ANY_MATCHER;
            } else if (matcher.equals(WILDCARD_MATCHER_ENCODED)) {
                return WILDCARD_MATCHER;
            }
            return matcher;
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

    public class HstComponentContext extends HstModelContext {
        private static final long serialVersionUID = 1L;

        public HstComponentContext(JcrNodeModel model) throws RepositoryException {
            super(model);
        }

        public static final String HST_COMPONENTS = "hst:components";

        @Override
        public void init(JcrNodeModel model) throws RepositoryException {
            this.model = new JcrNodeModel(model.getNode().getNode(HST_COMPONENTS));
        }

        public String decodeReferenceName(String name) {
            if (name.startsWith("hst:components/")) {
                return name.substring("hst:components/".length() + 1);
            }
            return "";
        }

        public String encodeReferenceName(String name) {
            return "hst:components/" + name;
        }

    }

    public class HstTemplateContext extends HstModelContext {
        private static final long serialVersionUID = 1L;

        public static final String HST_TEMPLATES = "hst:templates";

        public HstTemplateContext(JcrNodeModel model) throws RepositoryException {
            super(model);
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

    public class HstPageContext extends HstModelContext {
        private static final long serialVersionUID = 1L;

        public static final String HST_PAGES = "hst:pages";

        public HstPageContext(JcrNodeModel model) throws RepositoryException {
            super(model);
        }

        @Override
        public void init(JcrNodeModel model) throws RepositoryException {
            this.model = new JcrNodeModel(model.getNode().getNode(HST_PAGES));
        }

        public List<String> getPagesAsList() {
            List<String> pages = new ArrayList<String>();
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
                throw new IllegalArgumentException("Facet select " + HST_CONFIGURATION + " not fonud");
            }
            Node root = model.getNode().getNode(HST_CONFIGURATION);
            if (!root.hasNode(HST_CONFIGURATION)) {
                throw new IllegalArgumentException("Node " + HST_CONFIGURATION + " not fonud");
            }
            HippoNode hroot = (HippoNode) root.getNode(HST_CONFIGURATION);
            root = hroot.getCanonicalNode();
            this.model = new JcrNodeModel(root);
        }

    }
}
