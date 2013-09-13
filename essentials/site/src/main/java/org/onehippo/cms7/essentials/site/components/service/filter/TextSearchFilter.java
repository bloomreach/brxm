/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.site.components.service.filter;

import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.filter.BaseFilter;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.onehippo.cms7.essentials.site.components.service.ctx.SearchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id: TextSearchFilter.java 157405 2013-03-08 09:15:49Z mmilicevic $"
 */
public class TextSearchFilter implements QueryFilter {

    private static Logger log = LoggerFactory.getLogger(TextSearchFilter.class);


    private String query;

    public TextSearchFilter(String query) {
        this.query = query;
    }

    @Override
    public void apply(final HstQuery hstQuery, SearchContext context) {
        if (Strings.isNullOrEmpty(query) || query.trim().startsWith("*")) {
            log.warn("query was null or starts with (*), no filter will be applied: ", query);
            return;
        }


        try {
            BaseFilter base = hstQuery.getFilter();
            if (base == null) {
                base = hstQuery.createFilter();
            }
            String parsedQuery = SearchInputParsingUtils.parse(query, false);

            // we search as well on the TITLE and the CONTENT and OR this to boost hits in the title
            // a filter specifically for the title property that contains query
            Filter titleFilter = hstQuery.createFilter();
            titleFilter.addContains("hippoplugins:title", parsedQuery);
            // a filter for the entire content that contains query
            Filter contentFilter = hstQuery.createFilter();
            contentFilter.addContains(".", parsedQuery);

            Filter searchFilter = hstQuery.createFilter();
            // OR the title with content to boost title hits
            searchFilter.addOrFilter(titleFilter).addOrFilter(contentFilter);
            hstQuery.setFilter(((Filter) base).addAndFilter(searchFilter));
        } catch (FilterException e) {
            log.error("Filter disabled, error in syntax {}", query);
        }

    }


}
