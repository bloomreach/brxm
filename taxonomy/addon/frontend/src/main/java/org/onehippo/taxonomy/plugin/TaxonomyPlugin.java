/*
 *  Copyright 2009-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.plugin.api.JcrCategoryFilter;
import org.onehippo.taxonomy.plugin.model.JcrTaxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomyPlugin extends Plugin implements ITaxonomyService {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TaxonomyPlugin.class);

    public static final String CONFIG_CATEGORY_FILTERS = "taxonomy.category.filters";

    private String contentPath;
    private String state;
    private final List<JcrCategoryFilter> categoryFilters;

    public TaxonomyPlugin(final IPluginContext context,
                          final IPluginConfig config) {
        super(context, config);
        this.contentPath = config.getString("taxonomy.root");
        this.state = config.getString("taxonomy.state", "published");

        final String filters = config.getString(CONFIG_CATEGORY_FILTERS);
        if (filters != null) {
            final String[] categoryFilterClassNames = filters.split(",");
            final List<JcrCategoryFilter> filterList = new ArrayList<>();
            for (String filterClassName : categoryFilterClassNames) {
                try {
                    final String trimmed = filterClassName.trim();
                    Class clazz = Class.forName(trimmed);
                    if (JcrCategoryFilter.class.isAssignableFrom(clazz)) {
                        filterList.add((JcrCategoryFilter)
                                clazz.newInstance());
                    } else {
                        log.error("Configured category filter class {} is not a {}",
                                trimmed, JcrCategoryFilter.class.getName());
                    }
                } catch (ClassNotFoundException e) {
                    log.error("Failed to load configured category filter class {}", filterClassName, e);
                } catch (InstantiationException | IllegalAccessException e) {
                    log.error("Failed to instantiate configured category filter class {}", filterClassName, e);
                }
            }
            categoryFilters = Collections.unmodifiableList(filterList);
        } else {
            categoryFilters = Collections.emptyList();
        }
        context.registerService(this, config.getString(ITaxonomyService.SERVICE_ID, ITaxonomyService.DEFAULT_SERVICE_TAXONOMY_ID));
    }

    public Taxonomy getTaxonomy(String name) {
        try {
            Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
            Node node = ((Node) session.getItem(contentPath + "/" + name));
            Node child = selectChild(node);
            if (child != null) {
                return newTaxonomy(new JcrNodeModel(child), false);
            }
        } catch (RepositoryException ex) {
            if (log.isDebugEnabled()) {
                log.warn("Could not find taxonomy '" + name + "'", ex);
            } else {
                log.warn("Could not find taxonomy '{}'. {}", name, ex);
            }
        }

        return null;
    }

    @Override
    public List<JcrCategoryFilter> getCategoryFilters() {
        return categoryFilters;
    }

    protected JcrTaxonomy newTaxonomy(IModel<Node> model, boolean editing) {
        return new JcrTaxonomy(model, editing, this);
    }

    public List<String> getTaxonomies() {
        List<String> result = new LinkedList<>();
        try {
            Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
            NodeIterator nodes = ((Node) session.getItem(contentPath)).getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                if (node != null) {
                    try {
                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        Node child = selectChild(node);
                        if (child != null) {
                            result.add(node.getName());
                        }
                    }
                    } catch (ItemNotFoundException infe) {
                        log.error("Error accessing a child of {}", node.getPath(), infe);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Failed to list taxonomies", e);
        }
        return result;
    }

    private Node selectChild(Node parent) throws RepositoryException {
        NodeIterator children = parent.getNodes(parent.getName());
        while (children.hasNext()) {
            Node child = children.nextNode();
            if (child != null) {
                if (child.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TAXONOMY)) {
                    String childState = child.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                    if (childState.equals(state)) {
                        return child;
                    }
                }
            }
        }
        return null;
    }
}
