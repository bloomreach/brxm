/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.swing.tree.TreeNode;

import org.apache.wicket.model.IModel;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;

public class TaxonomyNode extends AbstractNode {
    private static final long serialVersionUID = 1L;

    private IModel<Taxonomy> model;

    public TaxonomyNode(IModel<Taxonomy> model, String language) {
        this(model, language, null);
    }

    public TaxonomyNode(IModel<Taxonomy> model, String language, Comparator<Category> categoryComparator) {
        super(model, language, categoryComparator);
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