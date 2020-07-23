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

import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocumentIterator;

public class IterablePaginationUtils {

    public static Pagination<HippoBean> createPagination(final HippoDocumentIterator<HippoBean> beans, final int totalSize,
            final int pageSize, final int pageNumber) {
        return new IterablePagination(beans, totalSize, pageSize, pageNumber);
    }

    public static Pagination<HippoBean> createPagination(final HippoBeanIterator beans, final int totalSize, final int pageSize,
            final int currentPage, final int limit) {
        return new IterablePagination(beans, totalSize, pageSize, currentPage, limit);
    }

    public static Pagination<HippoBean> createPagination(final List<HippoBean> items, final int currentPage, final int pageSize) {
        return new IterablePagination(items, currentPage, pageSize);
    }

    public static Pagination<HippoBean> createPagination(final int totalSize, final List<HippoBean> items) {
        return new IterablePagination(totalSize, items);
    }

    public static Pagination<HippoBean> createPagination(final HippoBeanIterator beans, final int currentPage) {
        return new IterablePagination(beans, currentPage);
    }

}
