/*
 *  Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.model.IModel;
import org.hippoecm.repository.api.HippoSession;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.plugin.AbstractTaxonomyTest;
import org.onehippo.taxonomy.plugin.api.EditableCategory;
import org.onehippo.taxonomy.plugin.api.JcrCategoryFilter;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JcrCategoryFilterTest extends AbstractTaxonomyTest {

    private IModel<Taxonomy> taxonomyModel;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        taxonomyModel = createNiceMock(IModel.class);
        taxonomyModel.detach();
        expectLastCall().anyTimes();
        replay(taxonomyModel);
    }

    class BarCategoryFilter implements JcrCategoryFilter {
        @Override
        public boolean apply(final JcrCategory category, final HippoSession session) {
            String name = category.getName();
            if ("bar".equals(name)) {
                return false;
            }
            return true;
        }
    }

    @Override
    public List<JcrCategoryFilter> getFilters() {
        JcrCategoryFilter[] filters = {new BarCategoryFilter()};
        return Collections.unmodifiableList(Arrays.asList(filters));
    }

    @Test
    public void test_filtering_does_not_filtering_when_editing_taxonomy() throws Exception {

        EditableCategory topItem = taxonomy.getCategoryByKey(TOP_KEY);
        List<? extends EditableCategory> children = topItem.getChildren();
        assertEquals(3, children.size());
        final EditableCategory firstLevelChild1 = children.get(0);
        assertEquals(BRANCH_NAME, firstLevelChild1.getName());
        assertEquals(BRANCH_KEY, firstLevelChild1.getKey());

        final EditableCategory firstLevelChild2 = topItem.addCategory("flFoo", "foo", Locale.ENGLISH, taxonomyModel);
        final EditableCategory firstLevelChild3 = topItem.addCategory("flBar", "bar", Locale.ENGLISH, taxonomyModel);

        final EditableCategory secondLevelChild1 = firstLevelChild1.addCategory("sclFoo", "foo", Locale.ENGLISH, taxonomyModel);
        final EditableCategory secondLevelChild2 = firstLevelChild1.addCategory("sclBar", "bar", Locale.ENGLISH, taxonomyModel);

        assertTrue(topItem.getChildren().contains(firstLevelChild2));
        assertTrue(topItem.getChildren().contains(firstLevelChild3));

        assertTrue(firstLevelChild1.getChildren().contains(secondLevelChild1));
        assertTrue(firstLevelChild1.getChildren().contains(secondLevelChild2));

    }

    @Test
    public void test_filtering_does_apply_when_non_editing_taxonomy() throws Exception {
        EditableCategory topItem = nonEditingTaxonomy.getCategoryByKey(TOP_KEY);
        List<? extends EditableCategory> children = topItem.getChildren();
        assertEquals(3, children.size());
        final EditableCategory firstLevelChild1 = children.get(0);
        assertEquals(BRANCH_NAME, firstLevelChild1.getName());
        assertEquals(BRANCH_KEY, firstLevelChild1.getKey());

        final EditableCategory firstLevelChild2 = topItem.addCategory("flFoo", "foo", Locale.ENGLISH, taxonomyModel);
        final EditableCategory firstLevelChild3 = topItem.addCategory("flBar", "bar", Locale.ENGLISH, taxonomyModel);

        final EditableCategory secondLevelChild1 = firstLevelChild1.addCategory("sclFoo", "foo", Locale.ENGLISH, taxonomyModel);
        final EditableCategory secondLevelChild2 = firstLevelChild1.addCategory("sclBar", "bar", Locale.ENGLISH, taxonomyModel);

        assertTrue(topItem.getChildren().contains(firstLevelChild2));
        assertFalse(topItem.getChildren().contains(firstLevelChild3));

        assertTrue(firstLevelChild1.getChildren().contains(secondLevelChild1));
        assertFalse(firstLevelChild1.getChildren().contains(secondLevelChild2));

    }
}
