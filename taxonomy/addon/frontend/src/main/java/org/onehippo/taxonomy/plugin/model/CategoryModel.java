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
package org.onehippo.taxonomy.plugin.model;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;

public class CategoryModel extends LoadableDetachableModel<Category> {

    private IModel<Taxonomy> taxonomyModel;
    private final String key;

    public CategoryModel(IModel<Taxonomy> taxonomyModel, String key) {
        this.taxonomyModel = taxonomyModel;
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public IModel<Taxonomy> getTaxonomyModel() {
        return taxonomyModel;
    }

    @Override
    protected Category load() {
        return taxonomyModel.getObject().getCategoryByKey(key);
    }

    @Override
    public void detach() {
        taxonomyModel.detach();
        super.detach();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CategoryModel) {
            CategoryModel that = (CategoryModel) obj;
            return that.taxonomyModel.getObject().equals(taxonomyModel.getObject()) && that.key.equals(key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return taxonomyModel.getObject().hashCode() ^ key.hashCode() ^ 223;
    }

}
