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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

public class TestPagination {

    @Test
    public void testCurrentPageIsCorrectWhenItemCountFewerThanPageNumber() {
        final List<String> items = IntStream.rangeClosed(1, 50).boxed().map(i -> new String()).collect(Collectors.toList());
        final Pagination<String> pagination = new SimplePagination<String>(items, 16);
        assertEquals(1, pagination.getCurrent().getNumber());
    }

    @Test
    public void testLastPageIsCorrectWhenPageCountFewerThanDefaultPageLimit() {
        final List<String> items = IntStream.rangeClosed(1, 50).boxed().map(i -> new String()).collect(Collectors.toList());
        final Pagination<String> pagination = new SimplePagination<String>(items, 16);
        assertEquals(6, pagination.getPages().size());
    }

    @Test
    public void testCurrentAndNextAndPreviousPagesAreCorrect() {
        final List<String> items = IntStream.rangeClosed(1, 50).boxed().map(i -> new String()).collect(Collectors.toList());
        final Pagination<String> pagination = new SimplePagination<String>(items, 3);
        assertEquals(3, pagination.getCurrent().getNumber());
        assertEquals(4, pagination.getNext().getNumber());
        assertEquals(2, pagination.getPrevious().getNumber());
    }

    @Test
    public void testNextPageIsNullWhenOnlyOnePageExist() {
        final Pagination<String> pagination = new SimplePagination<String>(Collections.emptyList(), 1);
        assertEquals(null, pagination.getNext());
    }

    @Test
    public void testPageRangeIsCorrectWhenPageCountFewerThanDefaultPageLimit() {
        final List<String> items = IntStream.rangeClosed(1, 50).boxed().map(i -> new String()).collect(Collectors.toList());
        final Pagination<String> pagination = new SimplePagination<String>(items, 6);
        assertEquals(6, pagination.getPages().size());
        assertEquals(1, pagination.getPages().get(0).getNumber());
        assertEquals(2, pagination.getPages().get(1).getNumber());
        assertEquals(3, pagination.getPages().get(2).getNumber());
        assertEquals(4, pagination.getPages().get(3).getNumber());
        assertEquals(5, pagination.getPages().get(4).getNumber());
        assertEquals(6, pagination.getPages().get(5).getNumber());
    }

    @Test
    public void testPageRangeIsCorrectWhenPageCountHigherThanDefaultPageLimit() {
        final List<String> items = IntStream.rangeClosed(1, 250).boxed().map(i -> new String()).collect(Collectors.toList());
        final Pagination<String> pagination = new SimplePagination<String>(items, 16);
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

    }

}
