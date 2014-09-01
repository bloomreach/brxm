/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components.paging;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocumentIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IterablePagination: a Pageable with HippoBean items.
 *
 * @version $Id$
 */
public class IterablePagination<T extends HippoBean> extends Pageable<T> {

    public static final String UNCHECKED = "unchecked";
    private static Logger log = LoggerFactory.getLogger(IterablePagination.class);


    private List<T> items;

    /**
     * Constructor to be used when the paging has been done beforehand (for example in HST query).
     * The beans iterator size should be the same as pageSize (except maybe for the last page).
     * <p/>
     * E.g. when HstQuery is used to get the beans, both HstQuery#setLimit and HstQuery#setOffset has been used.
     */
    public IterablePagination(final HippoBeanIterator beans, final int totalSize, final int pageSize, final int currentPage) {
        super(totalSize, currentPage, pageSize);

        // add all from iterator; assumption that paging is done beforehand
        processItems(beans);
    }

    /**
     * Constructor to be used when the paging has been done beforehand (for example in HST query).
     */
    public IterablePagination(int totalSize, List<T> items) {
        super(totalSize);
        this.items = new ArrayList<>(items);
    }

    /**
     * Constructor to be used when the paging is not done beforehand (for example in HST query), but has to be done by
     * this class, for instance paging on facet navigation results.
     */
    @SuppressWarnings({UNCHECKED})
    public IterablePagination(final HippoBeanIterator beans, final int currentPage) {
        super(beans.getSize(), currentPage);
        processOffset(beans);
    }

    /**
     * Constructor to be used when the paging is not done beforehand (for example in HST query), but has to be done by
     * this class, for instance paging on facet navigation results.
     */
    public IterablePagination(final HippoBeanIterator beans, final int pageSize, final int currentPage) {
        super(beans.getSize(), currentPage, pageSize);
        items = new ArrayList<>();
        processOffset(beans);
    }

    /**
     * Constructor to be used when the paging is not done beforehand (for example in HST query), but has to be done by
     * this class, for instance paging on facet navigation results.
     */
    public IterablePagination(List<T> items, final int currentPage, final int pageSize) {
        super(items.size(), currentPage, pageSize);
        this.items = new ArrayList<>();
        int fromIndex = (currentPage - 1) * pageSize;
        if (fromIndex >= 0 && fromIndex <= items.size()) {
            int toIndex = currentPage * pageSize;
            if (toIndex > items.size()) {
                toIndex = items.size();
            }
            try {
                this.items = items.subList(fromIndex, toIndex);
            } catch (IndexOutOfBoundsException iobe) {
                log.error("Sublist out of bounds: fromIndex=" + fromIndex + ", toIndex=" + toIndex + ", list size=" +
                        items.size(), iobe);
            }
        }
    }

    /**
     * Constructor to be used when the paging is not done beforehand (for example in HST query), but has to be done by
     * this class, for instance paging on facet navigation results.
     */
    public IterablePagination(final HippoDocumentIterator<T> beans, final int totalSize, final int pageSize, final int pageNumber) {
        super(totalSize, pageNumber, pageSize);
        processDocumentsOffset(beans);
    }

    /**
     * Add an item
     *
     * @param item the item
     */
    public void addItem(T item) {
        items.add(item);
    }


    /**
     * Get all paged items
     *
     * @return all paged items
     */
    @XmlElement
    @Override
    public List<? extends HippoBean> getItems() {
        return items;
    }

    @SuppressWarnings({UNCHECKED})
    public void setItems(List<? extends HippoBean> items) {
        this.items = (List<T>) items;
    }


    protected void processDocumentsOffset(HippoDocumentIterator<T> documentsIterator) {
        items = new ArrayList<>();
        int startAt = getStartOffset();
        if (startAt < getTotal()) {
            documentsIterator.skip(startAt);
        }
        int count = 0;
        while (documentsIterator.hasNext()) {
            if (count == getPageSize()) {
                break;
            }
            T bean = documentsIterator.next();
            if (bean != null) {
                items.add(bean);
                count++;
            }
        }
    }

    @SuppressWarnings(UNCHECKED)
    protected final void processOffset(HippoBeanIterator beans) {
        items = new ArrayList<>();
        int startAt = getStartOffset();
        if (startAt < getTotal()) {
            beans.skip(startAt);
        }
        int count = 0;
        while (beans.hasNext()) {
            if (count == getPageSize()) {
                break;
            }
            Object bean = beans.next();
            if (bean != null) {
                items.add((T) bean);
                count++;
            }
        }
    }

    /**
     * Process items without offset
     */
    protected void processItems(HippoBeanIterator beans) {
        items = new ArrayList<>();
        while (beans.hasNext()) {
            @SuppressWarnings(UNCHECKED)
            T bean = (T) beans.nextHippoBean();
            if (bean != null) {
                items.add(bean);
            }
        }
    }

}