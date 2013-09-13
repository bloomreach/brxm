/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.site.components.service;

import java.util.Collections;
import java.util.List;

import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentIterator;
import org.onehippo.forge.utilities.hst.paging.IterablePagination;
import org.onehippo.cms7.essentials.site.beans.BaseDocument;

/**
 * @version "$Id: SearchCollection.java 157405 2013-03-08 09:15:49Z mmilicevic $"
 */
public class SearchCollection<T extends BaseDocument> extends IterablePagination<T> {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final SearchCollection<? extends HippoBean> EMPTY_IMMUTABLE = new SearchCollection<BaseDocument>(0, Collections.<BaseDocument>emptyList());

    @SuppressWarnings("unchecked")
    public static <E extends BaseDocument> SearchCollection<E> emptyCollection() {
        return (SearchCollection<E>) EMPTY_IMMUTABLE;
    }

    public SearchCollection(final HstQueryResult result, final int currentPage) {
        super(result.getHippoBeans(), currentPage);
    }

    public SearchCollection(final HstQueryResult result, final int currentPage, final int pageSize) {
        super(result.getHippoBeans(), pageSize, currentPage);
        setTotal(result.getTotalSize());
        setPageNumber(currentPage);
        setPageSize(pageSize);
    }

    public SearchCollection(final int total, final List<T> items) {
        super(total, items);
    }

    public SearchCollection(final List<T> items, final int pageSize, final int currentPage) {
        super(items, pageSize, currentPage);
    }

    public SearchCollection(final HippoDocumentIterator<T> beans, final int results, final int pageSize, final int pageNumber) {
        super(beans, results, pageSize, 1);
        setTotal(results);
        setPageNumber(pageNumber);
        setPageSize(pageSize);
    }


}
