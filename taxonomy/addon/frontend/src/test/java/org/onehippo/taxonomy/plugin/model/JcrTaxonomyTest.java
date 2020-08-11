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
package org.onehippo.taxonomy.plugin.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.api.TaxonomyException;
import org.onehippo.taxonomy.plugin.AbstractTaxonomyTest;
import org.onehippo.taxonomy.plugin.api.EditableCategory;
import org.onehippo.taxonomy.plugin.api.EditableCategoryInfo;
import org.onehippo.taxonomy.plugin.api.EditableTaxonomy;
import org.onehippo.taxonomy.plugin.api.KeyCodec;

public class JcrTaxonomyTest extends AbstractTaxonomyTest {

    @Test
    public void testTaxonomyName() throws Exception {
        assertEquals("taxonomy", taxonomy.getName());
    }

    @Test
    public void testTaxonomyCategories() throws Exception {
        List<? extends Category> children = taxonomy.getCategories();
        assertEquals(1, children.size());
        assertEquals(TOP_KEY, children.get(0).getKey());
    }

    @Test
    public void testTaxonomyGetByKey() throws Exception {
        Category item = taxonomy.getCategoryByKey(TOP_KEY);
        assertNotNull(item);
        assertEquals(TOP_NAME, item.getName());
    }

    @Test
    public void testCategoryChildren() throws Exception {
        Category item = taxonomy.getCategoryByKey(TOP_KEY);
        List<? extends Category> children = item.getChildren();
        assertEquals(3, children.size());
        assertEquals(BRANCH_NAME, children.get(0).getName());
        assertEquals(BRANCH_KEY, children.get(0).getKey());
    }

    @Test
    public void testCategoryParent() throws Exception {
        Category topItem = taxonomy.getCategoryByKey(TOP_KEY);
        Category parent = topItem.getParent();
        assertNull(parent);

        Category branchItem = taxonomy.getCategoryByKey(BRANCH_KEY);
        parent = branchItem.getParent();
        assertEquals(parent, topItem);
    }

    @Test
    public void testTaxonomyTranslation() throws Exception {
        Category branchItem = taxonomy.getCategoryByKey(BRANCH_KEY);
        CategoryInfo info = branchItem.getInfo(Locale.ENGLISH);
        assertEquals(BRANCH_NAME_EN, info.getName());
        assertArrayEquals(new String[] { BRANCH_SYNONYM }, info.getSynonyms());
    }

    @Test
    public void testCategoryAncestors() throws Exception {
        Category branchItem = taxonomy.getCategoryByKey(BRANCH_KEY);
        List<? extends Category> ancestors = branchItem.getAncestors();
        assertEquals(2, ancestors.size());
        assertEquals(taxonomy.getCategoryByKey(TOP_KEY), ancestors.get(0));
        assertEquals(branchItem, ancestors.get(1));
    }

    // TODO: (also) test with en-GB style Locale?
    @Test
    public void testNewCategory() throws Exception {
        EditableTaxonomy editable = (EditableTaxonomy) taxonomy;
        EditableCategory category = editable.addCategory("new_category", "New:Category", Locale.ENGLISH);
        assertNotNull(category);
        assertEquals("new_category", category.getKey());
        assertEquals("new:category", category.getName());
        assertTrue(session.itemExists("/test/taxonomy/" + KeyCodec.encode("new:category")));
        assertEquals(editable, category.getTaxonomy());

        String key = category.getKey();
        Category testCat = taxonomy.getCategoryByKey(key);
        assertEquals(testCat, category);

        // verify auto-generated info
        EditableCategoryInfo info = category.getInfo(Locale.ENGLISH);
        assertNotNull(info);
        assertEquals("New:Category", info.getName());
        assertEquals("", info.getDescription());
        assertEquals(Locale.ENGLISH, info.getLocale());
        assertArrayEquals(new String[0], info.getSynonyms());

        // verify new name is propagated to category name and key
        info.setName("new-name");
        assertEquals("new-name", info.getName());
        assertEquals("new-name", category.getName());
        assertEquals("new_category", category.getKey());
        assertTrue(session.itemExists("/test/taxonomy/new-name"));

        info.setDescription("new-description");
        assertEquals("new-description", info.getDescription());

        // verify remove removes category
        category.remove();

        testCat = taxonomy.getCategoryByKey(key);
        assertNull(testCat);
    }

    @Test
    public void testRenameExistingCategory() throws Exception {
        EditableTaxonomy editable = taxonomy;
        EditableCategory category = editable.addCategory("key", "Category", Locale.ENGLISH);
        session.save();

        String key = category.getKey();
        assertEquals("key", key);

        EditableCategoryInfo info = category.getInfo(Locale.ENGLISH);
        info.setName("new-name");
        assertEquals("category", category.getName());
        assertEquals("key", category.getKey());
        assertEquals("new-name", category.getInfo(Locale.ENGLISH).getName());
    }

    @Test
    public void testRenameNewCategoryToExistingNameDoesntChangeName() throws Exception {
        EditableTaxonomy editable = taxonomy;
        EditableCategory aap = editable.addCategory("aap", "aap", (Locale) null);
        EditableCategory noot = editable.addCategory("noot", "noot", (Locale) null);
        assertEquals("noot", noot.getKey());

        EditableCategoryInfo aapInfo = aap.getInfo(Locale.ENGLISH);
        aapInfo.setName("noot");
        assertEquals("aap", aap.getName());
        assertEquals("aap", aap.getKey());
    }

    @Test
    public void testSettingCategoryKeyToExistingKeyFails() throws Exception {
        EditableTaxonomy editable =  taxonomy;
        EditableCategory aap = editable.addCategory("aap", "aap", (Locale) null);
        try {
            EditableCategory noot = editable.addCategory("aap", "noot", (Locale) null);
            throw new Exception("Should not reach this point");
        } catch (TaxonomyException ex) {
            // this is OK
        }
    }

}
