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

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocumentIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IterablePagination extends AbstractPagination<HippoBean> {

    private static Logger log = LoggerFactory.getLogger(IterablePagination.class);

    private List<HippoBean> items;

    public IterablePagination() {
        items = new ArrayList<>();
    }

    /**
     * Constructor to be used when the paging has been done beforehand (for example in HST query).
     * The beans iterator size should be the same as pageSize (except maybe for the last page).
     * <p/>
     * eg: when HstQuery is used to get the beans, both HstQuery#setLimit and HstQuery#setOffset has been used.
     */
    public IterablePagination(final HippoBeanIterator beans, final int totalSize, final int pageSize, final int currentPage) {
        super(totalSize, currentPage, pageSize);

        // add all from iterator; assuming that paging is done beforehand
        processItems(beans);
    }

    /**
     * Constructor to be used when the paging is not done beforehand (for example in HST query), but has to be done by
     * this class, for instance paging on facet navigation results.
     */
    public IterablePagination(final HippoDocumentIterator<HippoBean> beans, final int totalSize, final int pageSize,
            final int currentPage) {
        super(totalSize, currentPage, pageSize);
        processDocumentsOffset(beans);
    }

    /**
     * Constructor to be used when the paging has been done beforehand (for example in HST query).
     */
    public IterablePagination(int totalSize, List<HippoBean> items) {
        super(totalSize);
        this.items = new ArrayList<>(items);
    }

    /**
     * Constructor to be used when the paging is not done beforehand (for example in HST query), but has to be done by
     * this class, for instance paging on facet navigation results.
     */
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
    public IterablePagination(List<HippoBean> items, final int currentPage, final int pageSize) {
        super(items.size(), currentPage, pageSize);
        this.items = new ArrayList<>();
        int startOffset = (currentPage - 1) * pageSize;
        if (startOffset >= 0 && startOffset <= items.size()) {
            int endOffset = currentPage * pageSize;
            if (endOffset > items.size()) {
                endOffset = items.size();
            }
            try {
                this.items = items.subList(startOffset, endOffset);
            } catch (IndexOutOfBoundsException iobe) {
                log.error("Sublist out of bounds: fromIndex=" + startOffset + ", toIndex=" + endOffset + ", list size="
                        + items.size(), iobe);
            }
        }
    }

    /**
     * Add an item
     *
     * @param item the item
     */
    public void addItem(HippoBean item) {
        items.add(item);
    }

    /**
     * Get all paged items
     *
     * @return all paged items
     */
    @Override
    public List<HippoBean> getItems() {
        return items;
    }

    public void setItems(List<HippoBean> items) {
        this.items = items;
    }

    protected void processDocumentsOffset(HippoDocumentIterator<HippoBean> documentsIterator) {
        items = new ArrayList<>();
        final int offset = getOffset();
        if (offset < getTotal()) {
            documentsIterator.skip(offset);
        }
        int count = 0;
        while (documentsIterator.hasNext()) {
            if (count == getSize()) {
                break;
            }
            final HippoBean bean = documentsIterator.next();
            if (bean != null) {
                items.add(bean);
                count++;
            }
        }
    }

    protected final void processOffset(HippoBeanIterator beans) {
        items = new ArrayList<>();
        final int offset = getOffset();
        if (offset < getTotal()) {
            beans.skip(offset);
        }
        int count = 0;
        while (beans.hasNext()) {
            if (count == getSize()) {
                break;
            }
            final HippoBean bean = beans.next();
            if (bean != null) {
                items.add(bean);
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
            final HippoBean bean = beans.nextHippoBean();
            if (bean != null) {
                items.add(bean);
            }
        }
    }

}