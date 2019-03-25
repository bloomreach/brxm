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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.swing.tree.TreeNode;

import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.plugin.model.CategoryModel;

public class CategoryNode extends AbstractNode {

    private final AbstractNode parent;
    private final CategoryModel model;

    /**
     * In order to fix issue HIPPLUG-1583 we had to remove the two deprecated constructors that did not accept a
     * {@code parent} argument. Strictly speaking, these should have been removed in 13.0, but as we see more value in
     * fixing HIPPLUG-1583 than maintaining full backwards-compatibility we decided to remove them anyway.
     */
    public CategoryNode(final CategoryModel model, final AbstractNode parent, final Locale locale,
                        final Comparator<Category> categoryComparator) {
        super(model.getTaxonomyModel(), locale, categoryComparator);
        this.model = model;
        this.parent = parent;

        if (this.parent == null) {
            throw new IllegalStateException("Parent can not be null for category-node " + toString());
        }

        final Category category = getCategory();
        if (category == null) {
            throw new IllegalStateException("Could not get a category for '" + model.getKey() + "'.");
        }
    }

    public Category getCategory() {
        return model.getObject();
    }

    @Override
    List<? extends Category> getCategories() {
        return getCategory().getChildren();
    }

    public TreeNode getParent() {
        return parent;
    }

    public boolean isLeaf() {
        return getChildCount() == 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof CategoryNode) {
            return ((CategoryNode) obj).model.equals(model);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return model.hashCode() ^ 34603;
    }

    @Override
    public void detach() {
        model.detach();
        super.detach();
    }

}
