/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.component.pagination;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;

public class TestPagination {

    @Test
    public void test_current_page_is_correct_when_item_count_fewer_than_page_number() {
        final Pagination<?> pagination = new SimplePagination<>(Arrays.asList(new Object[50]), 16);
        assertEquals(1, pagination.getCurrent().getNumber());
    }

    @Test
    public void test_last_page_is_correct_when_page_count_fewer_than_default_page_limit() {
        final Pagination<?> pagination = new SimplePagination<>(Arrays.asList(new Object[50]), 16);
        assertEquals(5, pagination.getPages().size());
        assertEquals(5, pagination.getLast().getNumber());
    }

    @Test
    public void testCurrentAndNextAndPreviousPagesAreCorrect() {
        final Pagination<?> pagination = new SimplePagination<>(Arrays.asList(new Object[50]), 3);
        assertEquals(3, pagination.getCurrent().getNumber());
        assertEquals(4, pagination.getNext().getNumber());
        assertEquals(2, pagination.getPrevious().getNumber());
    }

    @Test
    public void test_next_Page_is_null_when_only_one_page_exists() {
        final Pagination<String> pagination = new SimplePagination<String>(Collections.emptyList(), 1);
        assertEquals(null, pagination.getNext());
    }

    @Test
    public void test_PageRange_is_correct_when_PageCount_fewer_than_DefaultPageLimit() {
        final Pagination<?> pagination = new SimplePagination<>(Arrays.asList(new Object[50]), 4);
        assertEquals(5, pagination.getPages().size());
        assertEquals(1, pagination.getPages().get(0).getNumber());
        assertEquals(2, pagination.getPages().get(1).getNumber());
        assertEquals(3, pagination.getPages().get(2).getNumber());
        assertEquals(4, pagination.getPages().get(3).getNumber());
        assertEquals(5, pagination.getPages().get(4).getNumber());
    }

    @Test
    public void test_pageRange_is_correct_when_pageCount_higher_than_defaultPageLimit() {
        final Pagination<?> pagination = new SimplePagination<>(Arrays.asList(new Object[250]), 10);
        assertEquals(10, pagination.getPages().size());
        assertEquals(11, pagination.getPages().get(0).getNumber());
        assertEquals(12, pagination.getPages().get(1).getNumber());
        assertEquals(13, pagination.getPages().get(2).getNumber());
        assertEquals(14, pagination.getPages().get(3).getNumber());
        assertEquals(15, pagination.getPages().get(4).getNumber());
        assertEquals(16, pagination.getPages().get(5).getNumber());
        assertEquals(17, pagination.getPages().get(6).getNumber());
        assertEquals(18, pagination.getPages().get(7).getNumber());
        assertEquals(19, pagination.getPages().get(8).getNumber());
        assertEquals(20, pagination.getPages().get(9).getNumber());
    }

    @Test
    public void test_size_is_correct_when_pageSize_is_equal_to_size() {
        final HippoBeanPagination<?> pagination = new HippoBeanPagination(Arrays.asList(new Object[20]), 1, 10);
        assertEquals(10, pagination.getSize());
    }

    @Test
    public void test_size_is_correct_when_pageSize_is_not_equal_to_size() {
        final HippoBeanPagination<?> pagination = new HippoBeanPagination(Arrays.asList(new Object[27]), 3, 10);
        assertEquals(7, pagination.getSize());
    }

    @Test
    public void test_correct_amount_of_pages_modulo_pageSize_bigger_0() {
        final Pagination<?> pagination = new SimplePagination<>(Arrays.asList(new Object[25]), 1);
        assertEquals(3, pagination.getPages().size());
    }

    @Test
    public void test_correct_amount_of_pages_modulo_pageSize_is_0() {
        final Pagination<?> pagination = new SimplePagination<>(Arrays.asList(new Object[20]), 1);
        assertEquals(2, pagination.getPages().size());
    }

    class SimplePagination<T> extends AbstractPagination<T> {

        private final List<T> items;

        public SimplePagination(final List<T> items, final int current) {
            super(items.size(), current);
            this.items = items;
        }

        @Override
        public List<T> getItems() {
            return items;
        }

        @Override
        public int getSize() {
            return CollectionUtils.isEmpty(items) ? 0 : items.size();
        }

    }

}
