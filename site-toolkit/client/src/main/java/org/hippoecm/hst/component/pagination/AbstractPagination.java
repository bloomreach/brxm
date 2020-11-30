/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.component.pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Abstract pagination implementation provides the core capabilites of a Pagination object.   
 *
 * @param <T>
 */
public abstract class AbstractPagination<T> implements Pagination<T> {

    private static final int DEFAULT_SIZE = 10;
    private static final int DEFAULT_NUMBER = 1;
    private static final int DEFAULT_LIMIT = 10;

    // maximum number of items in a page
    private int pageSize;
    private int current;
    private int limit;
    private long total;
    private boolean enabled;

    public AbstractPagination() {
        this(0);
    }

    public AbstractPagination(long total) {
        this(total, DEFAULT_NUMBER);
    }

    public AbstractPagination(long total, int current) {
        this(total, current, DEFAULT_SIZE);
    }

    public AbstractPagination(long total, int current, int pageSize) {
        this(total, current, pageSize, DEFAULT_LIMIT);
    }

    public AbstractPagination(long total, int current, int pageSize, int limit) {
        this.total = total;
        this.pageSize = (pageSize <= 0) ? DEFAULT_SIZE : pageSize;
        this.limit = (limit <= 0) ? DEFAULT_LIMIT : limit;
        this.current = (current > getCount() || current <= 0) ? 1 : current;
        this.enabled = true;
    }

    @Override
    public Page getFirst() {
        return new Page(1);
    }

    @Override
    public Page getPrevious() {
        return hasPrevious() ? new Page(current - 1) : null;
    }

    @Override
    public Page getCurrent() {
        return new Page(current);
    }

    @Override
    public Page getNext() {
        return hasNext() ? new Page(current + 1) : null;
    }

    @Override
    public Page getLast() {
        return new Page(getCount());
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public long getTotal() {
        return total;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public int getOffset() {
        int start = (current - 1) * pageSize;
        if (start >= total) {
            start = 0;
        }
        return start;
    }

    @Override
    public List<Page> getPages() {
        final List<Page> pages = new ArrayList<>();
        final int startPage = current - (current % limit) + 1;

        // end page gets calculated with respect to the limit value and it might be
        // smaller than the actual end page
        final int calculatedEndPage = startPage + limit - 1;
        final int actualEndPage = Math.min(calculatedEndPage, getCount());
        IntStream.rangeClosed(startPage, actualEndPage).forEach(pageNumber -> pages.add(new Page(pageNumber)));
        return pages;
    }

    /**
     * Whether current page has the next page
     *
     * @return true if current page is followed by other pages
     */
    private boolean hasNext() {
        return getCount() > current;
    }

    /**
     * Whether current page has the previous page
     *
     * @return true if current page is greater than 1
     */
    private boolean hasPrevious() {
        return current > 1;
    }

    /**
     * Calculates and returns total number of pages regarding of total number of items and the page size
     * 
     * @return total number of pages
     */
    private int getCount() {
        long modulo = total % pageSize;
        if (modulo == 0) {
            return  (int) total / pageSize;
        }

        return (int) ((total - modulo) / pageSize) + 1;
    }

    /**
     * Sets the page size. If the size is lower or equal to zero, default page size will be set.
     * 
     * @param size
     */
    public void setSize(int size) {
        this.pageSize = (size <= 0) ? DEFAULT_SIZE : size;
    }

    /**
     * Sets the pagination page limit. If the limit is lower or equal to zero, default page limit will be set.
     * 
     * @param limit
     */
    public void setLimit(int limit) {
        this.limit = (limit <= 0) ? DEFAULT_LIMIT : limit;
    }

    /**
     * Sets total number of results.
     *
     * @param total number of results query returned/data collection holds
     */
    public void setTotal(int total) {
        this.total = (total < 0) ? 0 : total;
    }

    /**
     * Sets the current page number.
     * 
     * @param current
     */
    public void setCurrent(int current) {
        this.current = current;
    }

    /**
     * Sets whether pagination is enabled
     * 
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
