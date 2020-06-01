/*
 *  Copyright 2009-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.swing.tree.TreeNode;

import org.onehippo.taxonomy.util.TaxonomyUtil;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.plugin.model.CategoryModel;

public abstract class AbstractNode implements TreeNode, IDetachable {

    private final Locale locale;
    private final IModel<Taxonomy> taxonomyModel;
    private List<CategoryNode> children = null;
    private final Comparator<Category> categoryComparator;

    /**
     * @deprecated Use {@link #AbstractNode(IModel, Locale, Comparator)} instead.
     */
    @Deprecated
    public AbstractNode(final IModel<Taxonomy> taxonomyModel, final String language) {
        this(taxonomyModel, TaxonomyUtil.toLocale(language), null);
    }

    /**
     * @deprecated Use {@link #AbstractNode(IModel, Locale, Comparator)} instead.
     */
    @Deprecated
    public AbstractNode(final IModel<Taxonomy> taxonomyModel, final String language, final Comparator<Category> categoryComparator) {
        this(taxonomyModel, TaxonomyUtil.toLocale(language), categoryComparator);
    }

    public AbstractNode(final IModel<Taxonomy> taxonomyModel, final Locale locale,
                 final Comparator<Category> categoryComparator) {
        this.taxonomyModel = taxonomyModel;
        this.locale = locale;
        this.categoryComparator = categoryComparator;
    }

    public List<CategoryNode> getChildren() {
        return getChildren(false);
    }

    public List<CategoryNode> getChildren(final boolean refresh) {
        if (children == null || refresh) {
            final List<? extends Category> categories = new LinkedList<Category>(getCategories());

            if (categoryComparator != null) {
                categories.sort(categoryComparator);
            }

            final List<CategoryNode> tempChildren = new LinkedList<>();

            for (final Category category : categories) {
                final CategoryModel categoryModel = new CategoryModel(taxonomyModel, category.getKey());
                tempChildren.add(new CategoryNode(categoryModel, this, locale, categoryComparator));
            }

            children = tempChildren;
        }

        return children;
    }

    abstract List<? extends Category> getCategories();

    public Enumeration<CategoryNode> children() {
        return Collections.enumeration(getChildren());
    }

    public TreeNode getChildAt(final int childIndex) {
        return getChildren().get(childIndex);
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public int getChildCount() {
        return getChildren().size();
    }

    public int getIndex(final TreeNode node) {
        final CategoryNode childNode = (CategoryNode) node;
        int index = 0;
        for (final CategoryNode item : getChildren()) {
            if (childNode.equals(item)) {
                break;
            }
            index++;
        }
        return index;
    }

    public void detach() {
        children = null;
        taxonomyModel.detach();
    }

    public Comparator<Category> getCategoryComparator() {
        return categoryComparator;
    }
}
