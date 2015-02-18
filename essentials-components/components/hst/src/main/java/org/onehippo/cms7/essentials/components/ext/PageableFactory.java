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
