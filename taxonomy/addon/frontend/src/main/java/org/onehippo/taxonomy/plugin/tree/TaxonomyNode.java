/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.swing.tree.TreeNode;

import org.onehippo.taxonomy.util.TaxonomyUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;

public class TaxonomyNode extends AbstractNode {

    private IModel<Taxonomy> model;

    /**
     * @deprecated Use {@link #TaxonomyNode(IModel, Locale, Comparator)} instead.
     */
    @Deprecated
    public TaxonomyNode(IModel<Taxonomy> model, String language) {
        this(model, TaxonomyUtil.toLocale(language), null);
    }

    /**
     * @deprecated use {@link #TaxonomyNode(IModel, Locale, Comparator)} instead
     */
    @Deprecated
    public TaxonomyNode(IModel<Taxonomy> model, String language, Comparator<Category> categoryComparator) {
        this(model, TaxonomyUtil.toLocale(language), categoryComparator);
    }

    public TaxonomyNode(final IModel<Taxonomy> model, final Locale locale, final Comparator<Category> categoryComparator) {
        super(model, locale, categoryComparator);
        this.model = model;
    }

    public Taxonomy getTaxonomy() {
        return model.getObject();
    }

    @Override
    List<? extends Category> getCategories() {
        return getTaxonomy().getCategories();
    }

    public TreeNode getParent() {
        return null;
    }

    public boolean isLeaf() {
        return false;
    }

    public CategoryNode findCategoryNodeByKey(final String key) {
        List<CategoryNode> categoryNodes = new ArrayList<CategoryNode>();
        findCategoryNodesByKey(this, key, categoryNodes);
        return categoryNodes.isEmpty() ? null : categoryNodes.get(0);
    }

    private static void findCategoryNodesByKey(final AbstractNode node, final String key, List<CategoryNode> categoryNodes) {
        for (CategoryNode categoryNode : node.getChildren()) {
            if (!categoryNodes.isEmpty()) {
                break;
            }

            if (StringUtils.equals(key, categoryNode.getCategory().getKey())) {
                categoryNodes.add(categoryNode);
            } else {
                findCategoryNodesByKey(categoryNode, key, categoryNodes);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TaxonomyNode) {
            return ((TaxonomyNode) obj).model.equals(model);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return model.hashCode() ^ 3461;
    }

    @Override
    public void detach() {
        model.detach();
        super.detach();
    }

}