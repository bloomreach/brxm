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
package org.onehippo.taxonomy.plugin.api;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.model.IModel;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyException;

public interface EditableCategory extends Category {

    List<? extends EditableCategory> getChildren();

    EditableCategoryInfo getInfo(Locale locale);

    List<? extends EditableCategory> getAncestors();

    EditableCategory addCategory(String key, String name, Locale locale, final IModel<Taxonomy> taxonomyModel) throws TaxonomyException;

    void remove() throws TaxonomyException;

    boolean canMoveUp();

    boolean moveUp() throws TaxonomyException;

    boolean canMoveDown();

    boolean moveDown() throws TaxonomyException;

    void move(EditableCategory destNode) throws TaxonomyException;

    void move(EditableTaxonomy taxonomy) throws TaxonomyException;
}
