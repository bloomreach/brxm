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

import javax.swing.tree.DefaultTreeModel;

import org.apache.wicket.model.IModel;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;

public class TaxonomyTreeModel extends DefaultTreeModel {
    private static final long serialVersionUID = 1L;

    private String language;

    public TaxonomyTreeModel(IModel<Taxonomy> root, String language) {
        this(root, language, null);
    }

    public TaxonomyTreeModel(IModel<Taxonomy> root, String language, Comparator<Category> categoryComparator) {
        super(new TaxonomyNode(root, language, categoryComparator));
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }
}
