/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.components.paging;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * Pageable
 *
 * @version $Id$
 */
public abstract class Pageable<T extends HippoBean> {

    private static final int DEFAULT_PAGE_RANGE = 10;

    public static final int DEFAULT_PAGE_FILL = 9;

    public static final int DEFAULT_PAGE_SIZE = 10;

    public static final int DEFAULT_PAGE_NUMBER = 1;

    public static final int DEFAULT_VISIBLE_PAGES = 10;

    private int pageSize = DEFAULT_PAGE_SIZE;

    private int pageNumber = DEFAULT_PAGE_NUMBER;

    private int visiblePages = DEFAULT_VISIBLE_PAGES;

    private long total;

    private boolean showPagination = true;

    /**
     * Constructor. NOTE: you can always override <code><strong>setTotal()</strong></code> method in your own class if
     * total number of items is not available immediately
     *
     * @param total total number of results query has returned
     * @see #setTotal(int)
     */
    public Pageable(long total) {
        this.total = total;
        processAll();
    }

    public Pageable(long total, int currentPage) {
        this.total = total;
        this.pageNumber = currentPage;
        processAll();
    }

    public Pageable(long total, int currentPage, int pageSize) {
        this.total = total;
        this.pageNumber = currentPage;
        this.pageSize = pageSize;
        processAll();
    }

    public Pageable(long total, int pageSize, int pageNumber, int visiblePages) {
        this.total = total;
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
        this.visiblePages = visiblePages;
        processAll();
    }

    public final void processAll() {
        processPageSize();
        if (visiblePages < 0) {
            visiblePages = DEFAULT_VISIBLE_PAGES;
        }
        processPageNumber();
    }

    /**
     * Returns max page size which wis used to limit nr. of results when executing query
     *
     * @return limit number
     */
    public long getMaxSize() {
        return getEndPage() * getPageSize() + 1;
    }

    /**
     * Returns current page number
     *
     * @return pagenumber we are displaying
     */
    public int getCurrentPage() {
        return pageNumber;
    }

    /**
     * Returns previous page number
     *
     * @return pagenumber we are displaying
     */
    public Integer getPreviousPage() {
        if (isPrevious()) {
            return pageNumber - 1;
        }
        return null;
    }

    /**
     * Returns next page number
     *
     * @return pagenumber we are displaying
     */
    public Integer getNextPage() {
        if (isNext()) {
            return pageNumber + 1;
        }
        return null;
    }

    /**
     * Has current page previous pages?
     *
     * @return true if page is bigger than 1 false otherwise
     */
    public boolean isPrevious() {
        return pageNumber > 1;
    }

    /**
     * Has current page next pages?
     *
     * @return true if page is followed  by other pages
     */
    public boolean isNext() {
        return getTotalPages() > pageNumber;
    }

    /**
     * Does  pagenumber exceeds number of visible pages?
     *
     * @return true if so, false otherwise
     */
    public boolean isPreviousBatch() {
        return pageNumber > visiblePages;
    }

    /**
     * Is  pagenumber followed by next pages e.g. next 10
     *
     * @return true if so, false otherwise
     */
    public boolean isNextBatch() {
        return getTotalPages() > getEndPage();
    }

    /**
     * Returns a list of numbers (between start and end offset)
     *
     * @return List containing page numbers..
     */
    public List<Long> getPageNumbersArray() {
        long startPage = getStartPage();
        long endPage = getEndPage();
        List<Long> pages = new ArrayList<Long>();
        for (long i = startPage; i <= endPage; i++) {
            pages.add(i);
        }
        return pages;
    }

    /**
     * Get where result offset should start NOTE: it's zero based
     *
     * @return int
     */
    public int getStartOffset() {
        int start = (pageNumber - 1) * pageSize;
        if (start >= total) {
            start = 0;
        }
        return start;
    }

    /**
     * get where result offset should end
     *
     * @return int
     */
    public long getEndOffset() {
        long end = pageNumber * pageSize;
        if (end > total) {
            end = total;
            if ((end - getStartOffset()) > pageSize) {
                end = pageSize;
            }
        }
        return end;
    }

    /**
     * get end page of the current page set (e.g. in pages 1...10 it will return 10)
     *
     * @return end page nr. of page batch
     */
    public long getEndPage() {
        final long startPage = getStartPage();
        long total_pages = getTotalPages();
        // boundary check
        if (pageNumber > total_pages) {
            return total_pages;
        }
        long end = startPage + visiblePages - 1;
        return end > total_pages ? total_pages : end;
    }

    /**
     * get start page of the offset, so, assuming visiblePages is set to 10: e.g. if pageNumber 3, it'll return 1,
     * pagenumber 19, it'll return  11)
     *
     * @return int page number of visible page batch
     */
    public long getStartPage() {

        if (pageNumber <= visiblePages || pageNumber == 1) {
            return 1;
        }
        int start = pageNumber / visiblePages;
        int remainder = pageNumber % visiblePages;
        if (remainder == 0) {
            return start * visiblePages - visiblePages + 1;
        }
        return start * visiblePages + 1;
    }

    /**
     * Return total number of pages (based on page size)
     *
     * @return nr. of pages
     */
    public long getTotalPages() {
        long pages = total / pageSize;
        long remainder = total % pageSize;
        pages += remainder == 0 ? 0 : 1;
        return pages;
    }

    //=================================
    //NOTE:
    // a lot of bound checking is done pretty
    // (much monkey-proof setters)
    //================================

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize <= 0 ? 10 : pageSize;
    }

    public final void processPageSize() {
        pageSize = pageSize <= 0 ? 10 : pageSize;
    }

    public final void processPageNumber() {
        if (pageNumber > getTotalPages()) {
            pageNumber = 1;
        }
        pageNumber = pageNumber <= 0 ? 1 : pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getVisiblePages() {
        return visiblePages;
    }

    /**
     * Total number of results.
     *
     * @return total nr. of results
     */
    public long getTotal() {
        return total;
    }

    /**
     * Sets total number of results.
     *
     * @param total number of results query returned/your collection holds
     */
    public void setTotal(int total) {
        if (total < 0) {
            total = 0;
        }
        this.total = total;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }


    public boolean isShowPagination() {
        return showPagination;
    }

    public void setShowPagination(final boolean showPagination) {
        this.showPagination = showPagination;
    }

    protected int getDefaultPageRange() {
        return DEFAULT_PAGE_RANGE;
    }

    /**
     * Default Page range for current selected page, it is "google alike" page range with x pages before selected item
     * and x+1 after selected item.
     *
     * @return range based on default fill {@literal 1, 2, 3, 4, 5 <selected 6>, 7, 8,9 etc. }
     * @see #DEFAULT_PAGE_FILL
     */
    public List<Long> getCurrentRange() {
        return getPageRangeWithFill(getCurrentPage(), DEFAULT_PAGE_FILL);
    }

    /**
     * Default page range for given page
     *
     * @param page current page
     * @return page surrounded by results on both side e.g. {@literal 1, 2, 3, 4 &lt;selected page&gt; 5, 6 ,7 ,8,9
     * etc.>}
     * @see #DEFAULT_PAGE_FILL
     */
    public List<Long> getPageRange(final int page) {
        return getPageRangeWithFill(page, DEFAULT_PAGE_FILL);
    }


    /**
     * Return previous X and next X pages for given page, based on total pages.
     *
     * @param page   selected page
     * @param fillIn selected page
     * @return page range for given page
     */
    public List<Long> getPageRangeWithFill(long page, final int fillIn) {
        final List<Long> pages = new ArrayList<>();
        // do bound checking
        if (page < 0) {
            page = 1;
        }
        if (page > getTotalPages()) {
            page = getTotalPages();
        }
        // fill in lower range: e.g. for 2 it will  be 1
        long start = page - fillIn;
        if (start <= 0) {
            start = 1;
        }
        // end part:
        long end = page + fillIn + 1;
        if (end > getTotalPages()) {
            end = getTotalPages();
        }
        for (long i = start; i <= end; i++) {
            pages.add(i);
        }
        return pages;
    }


    public abstract List<? extends HippoBean> getItems();
}
