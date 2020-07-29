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

import java.util.List;

import org.hippoecm.hst.content.PageModelEntity;

/**
 * Base class for Pagination implementations. 
 */
public interface Pagination<T> extends PageModelEntity {

    /**
     * Gets the items listed on the page
     * 
     */
    List<T> getItems();

    /**
     * Gets the first page
     *
     * @return
     */
    Page getFirst();

    /**
     * Gets the previous page
     *
     * @return the previous page if there is any. Otherwise returns null.
     */
    Page getPrevious();

    /**
     * Gets the current page
     *
     * @return
     */
    Page getCurrent();

    /**
     * Gets the next page
     *
     * @return the next page if there is any. Otherwise returns null.
     */
    Page getNext();

    /**
     * Gets the last page
     *
     * @return
     */
    Page getLast();

    /**
     * Gets the size of items which are listed on the page.
     * 
     * @return
     */
    int getSize();

    /**
     * Gets the number of pages which are shown on the pagination
     * 
     * @return
     */
    int getLimit();

    /**
     * Total number of results
     *
     * @return
     */
    long getTotal();

    /**
     * Whether pagination is enabled
     * 
     * @return true if the pagination is enabled
     */
    boolean isEnabled();

    /**
     * Gets the offset with respect to the current page number.
     *
     * @return calculates and returns the start offset. If the calculated value is
     *         greater or equal to total number of results, returns 0.
     */
    int getOffset();

    /**
     * Returns pages with respect to the limit
     *
     * @return List containing page numbers
     */
    List<Page> getPages();

}
