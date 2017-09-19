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
package org.onehippo.taxonomy.plugin.tree;

import java.util.Comparator;
import java.util.Locale;

import javax.swing.tree.DefaultTreeModel;

import org.apache.commons.lang.LocaleUtils;
import org.apache.wicket.model.IModel;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;

public class TaxonomyTreeModel extends DefaultTreeModel {

    private Locale locale;

    /**
     * @deprecated use {@link #TaxonomyTreeModel(IModel, Locale)} instead
     */
    @Deprecated
    public TaxonomyTreeModel(IModel<Taxonomy> root, String language) {
        this(root, language, null);
    }

    /**
     * @deprecated use {@link #TaxonomyTreeModel(IModel, Locale, Comparator)} instead
     */
    @Deprecated
    public TaxonomyTreeModel(IModel<Taxonomy> root, String language, Comparator<Category> categoryComparator) {
        this(root, LocaleUtils.toLocale(language), categoryComparator);
    }

    public TaxonomyTreeModel(IModel<Taxonomy> root, Locale locale) {
        this(root, locale, null);
    }

    public TaxonomyTreeModel(IModel<Taxonomy> root, Locale locale, Comparator<Category> categoryComparator) {
        super(new TaxonomyNode(root, locale, categoryComparator));
        this.locale = locale;
    }

    /**
     * @deprecated use {@link #getLocale()} to retrieve the language code from
     */
    @Deprecated
    public String getLanguage() {
        return locale.getLanguage();
    }

    public Locale getLocale() {
        return locale;
    }
}
