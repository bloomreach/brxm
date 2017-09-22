/*
 * Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.taxonomy.demo.plugin;

import java.util.Locale;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.plugin.TaxonomyBrowser;
import org.onehippo.taxonomy.plugin.model.CategoryModel;
import org.onehippo.taxonomy.plugin.model.Classification;
import org.onehippo.taxonomy.plugin.model.TaxonomyModel;

public class AdditionalFieldCustomTaxonomyBrowser extends TaxonomyBrowser {

    private static final long serialVersionUID = 1L;

    public AdditionalFieldCustomTaxonomyBrowser(String id, IModel<Classification> model, TaxonomyModel taxonomyModel, Locale preferredLocale) {
        super(id, model, taxonomyModel, preferredLocale);
    }

    @Override
    protected Details newDetails(String id, CategoryModel model) {
        return new Details(id, model);
    }

    @Override
    protected void addCategoryDetailFields(MarkupContainer detailFragment, Category category) {
        super.addCategoryDetailFields(detailFragment, category);
        CategoryInfo translation = category.getInfo(getPreferredLocaleObject());

        if (translation != null) {
            detailFragment.add(new MultiLineLabel("fulldescription",
                                                  translation.getString(CustomTaxonomyConstants.FULL_DESCRIPTION, "")));
        } else {
            detailFragment.add(new MultiLineLabel("fulldescription").setVisible(false));
        }
    }

    protected class Details extends TaxonomyBrowser.Details {
        public Details(String id, CategoryModel model) {
            super(id, model);
        }
    }

}
