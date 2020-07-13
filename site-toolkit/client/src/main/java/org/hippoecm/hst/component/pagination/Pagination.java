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
import java.util.stream.LongStream;

import org.hippoecm.hst.content.PageModelEntity;

/**
 *
 */
public abstract class Pagination<T> implements PageModelEntity {

    private static final int DEFAULT_SIZE = 10;
    private static final int DEFAULT_NUMBER = 1;

    private int size;
    private int current;
    private long total;
    private boolean enabled;

    public Pagination() {
        this(0);
    }

    public Pagination(long total) {
        this(total, DEFAULT_NUMBER);
    }

    public Pagination(long total, int current) {
        this(total, current, DEFAULT_SIZE);
    }

    public Pagination(long total, int current, int size) {
        this.total = total;
        this.size = (size <= 0) ? DEFAULT_SIZE : size;
        this.current = (current > getLast() || current <= 0) ? 1 : current;
        this.enabled = true;
    }

    public abstract List<T> getItems();

    /**
     * Gets the first page
     *
     * @return
     */
    public int getFirst() {
        return 1;
    }

    /**
     * Gets the previous page
     *
     * @return the previous page if there is any. Otherwise returns null.
     */
    public int getPrevious() {
        return hasPrevious() ? current - 1 : null;
    }

    /**
     * Gets the current page
     *
     * @return
     */
    public int getCurrent() {
        return current;
    }

    /**
     * Gets the next page
     *
     * @return the next page if there is any. Otherwise returns null.
     */
    public int getNext() {
        return hasNext() ? current + 1 : null;
    }

    /**
     * Gets the last page
     *
     * @return
     */
    public int getLast() {
        return (int) ((total - (total % size)) / size) + 1;
    }

    /**
     * Gets the size of items which are listed on the page
     * 
     * @return
     */
    public int getSize() {
        return size;
    }

    /**
     * Total number of results
     *
     * @return
     */
    public long getTotal() {
        return total;
    }

    /**
     * Whether pagination is enabled
     * 
     * @return true if the pagination is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the offset with respect to the current page number.
     *
     * @return calculates and returns the start offset. If the calculated value is
     *         greater or equal to total number of results, returns 0. 
     */
    public int getOffset() {
        int start = (current - 1) * size;
        if (start >= total) {
            start = 0;
        }
        return start;
    }

    /**
     * Returns all pages numbers
     *
     * @return List containing page numbers
     */
    public List<Long> getPages() {
        final List<Long> pages = new ArrayList<>();
        LongStream.range(getFirst(), getLast() + 1).forEach(pageNumber -> pages.add(pageNumber));
        return pages;
    }

    /**
     * Whether current page has the next page
     *
     * @return true if current page is followed by other pages
     */
    private boolean hasNext() {
        return getLast() > current;
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
     * Sets the page size. If the size is lower or equal to zero, default page size will be set.
     * 
     * @param size
     */
    public void setSize(int size) {
        this.size = (size <= 0) ? DEFAULT_SIZE : size;
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
