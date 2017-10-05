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

import static org.junit.Assert.assertEquals;

import java.util.Comparator;

import org.apache.wicket.model.IModel;
import org.junit.Test;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.plugin.AbstractTaxonomyTest;

public class TaxonomyTreeTest extends AbstractTaxonomyTest {

    class TaxModel implements IModel {

        public Object getObject() {
            return taxonomy;
        }

        public void setObject(Object object) {

        }

        public void detach() {

        }
    }

    protected TaxonomyTreeModel treeModel;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        IModel taxonomyModel = new TaxModel();
        final String locale = "en";
        Comparator<Category> categoryComparator = new CategoryNameComparator(locale);
        treeModel = new TaxonomyTreeModel(taxonomyModel, locale, categoryComparator);
    }

    @Test
    public void testTree() {
        TaxonomyNode root = (TaxonomyNode) treeModel.getRoot();
        assertEquals(1, root.getChildCount());
        assertEquals("taxonomy", root.getTaxonomy().getName());

        CategoryNode top = root.children().nextElement();
        assertEquals(3, top.getChildCount());
        Category topItem = top.getCategory();
        assertEquals(TOP_KEY, topItem.getKey());
        assertEquals(TOP_NAME, topItem.getName());

        CategoryNode branch = top.children().nextElement();
        assertEquals(0, branch.getChildCount());
        Category branchItem = branch.getCategory();
        assertEquals(BRANCH_KEY, branchItem.getKey());
        assertEquals(BRANCH_NAME, branchItem.getName());
    }

}
