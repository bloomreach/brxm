/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.onehippo.taxonomy.util.TaxonomyUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.util.DocumentUtils;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.plugin.ITaxonomyService;
import org.onehippo.taxonomy.plugin.api.EditableTaxonomy;
import org.onehippo.taxonomy.plugin.api.TaxonomyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrTaxonomy extends TaxonomyObject implements EditableTaxonomy {

    private static final Logger log = LoggerFactory.getLogger(JcrTaxonomy.class);

    private transient Map<String, JcrCategory> categories;

    public JcrTaxonomy(final IModel<Node> nodeModel,
                       final boolean editable,
                       final ITaxonomyService service) {
        super(nodeModel, editable, service);
    }

    /**
     * @deprecated use {@link #getLocaleObjects()} instead.
     */
    @Override
    @Deprecated
    public String[] getLocales() {
        return getLocaleObjects().stream().map(Locale::getLanguage).toArray(String[]::new);
    }

    @Override
    public List<Locale> getLocaleObjects() {
        List<Locale> locales = new ArrayList<>();

        try {
            Node node = getNode();

            if (node.hasProperty(TaxonomyNodeTypes.HIPPOTAXONOMY_LOCALES)) {
                for (Value value : node.getProperty(TaxonomyNodeTypes.HIPPOTAXONOMY_LOCALES).getValues()) {
                    final String localeString = StringUtils.trim(value.getString());
                    if (!StringUtils.isEmpty(localeString)) {
                        locales.add(TaxonomyUtil.toLocale(localeString));
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        }

        return locales;
    }

    @Override
    public List<JcrCategory> getCategories() {
        List<JcrCategory> result = new LinkedList<>();
        try {
            final Node node = getNode();
            final String nodePath = node.getPath();
            for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                Node child = iter.nextNode();
                if (child != null) {
                    try {
                        if (child.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY)) {

                            final JcrCategory category = toCategory(new JcrNodeModel(child), editable);

                            if (applyCategoryFilters(category)) {
                                result.add(category);
                            }
                        }
                    } catch (RepositoryException re) {
                        if (log.isDebugEnabled()) {
                            log.debug("Can't create category from child of node " +  nodePath, re);
                        }
                        else {
                            log.warn("Can't create category from child of node {}, message is {}", nodePath, re.getMessage());
                        }
                    } catch (TaxonomyException te) {
                        log.warn("TaxonomyException: can't create category from child of node {}, message is {}", nodePath, te.getMessage());
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error("Failure getting categories", ex);
        }
        return result;
    }

    @Override
    public List<JcrCategory> getDescendants() {
        return null;
    }

    @Override
    public String getName() {
        try {
            final IModel<String> nameModel = DocumentUtils.getDocumentNameModel(getNodeModel());
            if (nameModel != null) {
                return nameModel.getObject();
            }
        } catch (RepositoryException ignored) {
        }
        return null;
    }

    @Override
    public JcrCategory getCategory(String relPath) {
        return null;
    }

    @Override
    @Deprecated
    public JcrCategory addCategory(String key, String name, String locale) throws TaxonomyException {
        return addCategory(key, name, TaxonomyUtil.toLocale(locale));
    }

    @Override
    public JcrCategory addCategory(final String key, final String name, final Locale locale) throws TaxonomyException {
        try {
            final JcrCategory category = createCategory(getNode(), key, name, locale);
            detach();
            return category;
        } catch (RepositoryException ex) {
            throw new TaxonomyException("Could not create category with key " + key +
                    ", name " + name + " and locale " + locale, ex);
        }
    }

    public JcrCategory getCategoryByKey(String key) {
        if (key == null) {
            return null;
        }

        loadCategories();

        JcrCategory category = categories.get(key);
        if (category != null) {
            Node node;
            try {
                node = category.getNode();
                if (node != null && node.getSession().itemExists(node.getPath())) {
                    return category;
                }
            } catch (RepositoryException e) {
                log.error("failed to find category by key " + key, e);
            }
        }
        return null;
    }

    private void loadCategories() {
        if (categories == null) {
            categories = new TreeMap<>();
            try {
                Node taxonomyRootNode = getNode();
                if (!taxonomyRootNode.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TAXONOMY)) {
                    log.warn("Expected taxonomy  node of type {} for node {} but was of type {}",
                            new String[]{TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TAXONOMY, taxonomyRootNode.getPath(), taxonomyRootNode.getPrimaryNodeType().getName()});
                    return;
                }
                loadCategories(taxonomyRootNode);

            } catch (RepositoryException ex) {
                log.error("failed to load categories", ex);
            }
        }
    }

    private void loadCategories(final Node node) throws RepositoryException {

        if (node instanceof HippoNode) {
            if (((HippoNode)node).isVirtual())  {
                return;
            }
        }

        for (Node child : new NodeIterable(node.getNodes())) {
            try {
                if (child.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY)) {

                    final JcrCategory category = toCategory(new JcrNodeModel(child), editable);

                    if (!applyCategoryFilters(category)) {
                        continue;
                    }

                    final String key = category.getKey();
                    if (key == null) {
                        log.warn("Category {} does not have a key", category.getPath());
                        continue;
                    }

                    categories.put(key, category);
                    loadCategories(child);
                }
            } catch (TaxonomyException e) {
                if (log.isDebugEnabled()) {
                    log.warn("TaxonomyException: failed to load category from below {} : {}", node.getPath() , e);
                } else {
                    log.warn("TaxonomyException: failed to load category from below {}, message is {}", node.getPath() , e.toString());
                }
            }
        }
    }

    @Override
    public void detach() {
        super.detach();
        categories = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JcrTaxonomy) {
            return ((JcrTaxonomy) obj).getNodeModel().equals(getNodeModel());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getNodeModel().hashCode() ^ 991;
    }


}
