/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.selection.frontend.plugin.sorting;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.forge.selection.frontend.model.ListItem;
import org.onehippo.forge.selection.frontend.model.ValueList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class DefaultListItemComparatorTest {
    
    private ValueList valueList;
    
    @Before
    public void setup() {
        valueList = new ValueList();
        valueList.add(new ListItem("a", "Z"));
        valueList.add(new ListItem("c", "Y"));
        valueList.add(new ListItem("b", "X"));
    }
    
    @Test
    public void sortAscendingByLabel() {
        IListItemComparator comparator = new DefaultListItemComparator();
        comparator.setSortOptions(SortBy.label, SortOrder.ascending);
        valueList.sort(comparator);
        
        assertThat(valueList.get(0).getKey(), equalTo("b"));
        assertThat(valueList.get(0).getLabel(), equalTo("X"));
        assertThat(valueList.get(1).getKey(), equalTo("c"));
        assertThat(valueList.get(1).getLabel(), equalTo("Y"));
        assertThat(valueList.get(2).getKey(), equalTo("a"));
        assertThat(valueList.get(2).getLabel(), equalTo("Z"));
    }
    
    @Test
    public void sortAscendingByKey() {
        IListItemComparator comparator = new DefaultListItemComparator();
        comparator.setSortOptions(SortBy.key, SortOrder.ascending);
        valueList.sort(comparator);

        assertThat(valueList.get(0).getKey(), equalTo("a"));
        assertThat(valueList.get(0).getLabel(), equalTo("Z"));
        assertThat(valueList.get(1).getKey(), equalTo("b"));
        assertThat(valueList.get(1).getLabel(), equalTo("X"));
        assertThat(valueList.get(2).getKey(), equalTo("c"));
        assertThat(valueList.get(2).getLabel(), equalTo("Y"));
    }

    @Test
    public void sortDescendingByLabel() {
        IListItemComparator comparator = new DefaultListItemComparator();
        comparator.setSortOptions(SortBy.label, SortOrder.descending);
        valueList.sort(comparator);

        assertThat(valueList.get(0).getKey(), equalTo("a"));
        assertThat(valueList.get(0).getLabel(), equalTo("Z"));
        assertThat(valueList.get(1).getKey(), equalTo("c"));
        assertThat(valueList.get(1).getLabel(), equalTo("Y"));
        assertThat(valueList.get(2).getKey(), equalTo("b"));
        assertThat(valueList.get(2).getLabel(), equalTo("X"));
    }
    
    @Test
    public void sortDescendingByKey() {
        IListItemComparator comparator = new DefaultListItemComparator();
        comparator.setSortOptions(SortBy.key, SortOrder.descending);
        valueList.sort(comparator);

        assertThat(valueList.get(0).getKey(), equalTo("c"));
        assertThat(valueList.get(0).getLabel(), equalTo("Y"));
        assertThat(valueList.get(1).getKey(), equalTo("b"));
        assertThat(valueList.get(1).getLabel(), equalTo("X"));
        assertThat(valueList.get(2).getKey(), equalTo("a"));
        assertThat(valueList.get(2).getLabel(), equalTo("Z"));
    }
    
}
