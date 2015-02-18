/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components.ext;

import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocumentIterator;
import org.onehippo.cms7.essentials.components.paging.IterablePagination;
import org.onehippo.cms7.essentials.components.paging.Pageable;

public abstract class PageableFactory {

    public <T extends HippoBean> Pageable<T> createPageable(final HippoDocumentIterator<T> beans, final int totalSize, final int pageSize, final int pageNumber) {
        return new IterablePagination<>(beans, totalSize, pageSize, pageNumber);
    }

    public <T extends HippoBean> Pageable<T> createPageable(final HippoBeanIterator beans, final int totalSize, final int pageSize, final int currentPage) {
        return new IterablePagination<>(beans, totalSize, pageSize, currentPage);
    }

    public <T extends HippoBean> Pageable<T> createPageable(List<T> items, final int currentPage, final int pageSize) {
        return new IterablePagination<>(items, currentPage, pageSize);
    }

    public <T extends HippoBean> Pageable<T> createPageable(int totalSize, List<T> items) {
        return new IterablePagination<>(totalSize, items);
    }

    public <T extends HippoBean> Pageable<T> createPageable(final HippoBeanIterator beans, final int currentPage) {
        return new IterablePagination<>(beans, currentPage);
    }


}
