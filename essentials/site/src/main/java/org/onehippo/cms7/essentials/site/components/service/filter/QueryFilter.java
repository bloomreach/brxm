/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.site.components.service.filter;

import org.hippoecm.hst.content.beans.query.HstQuery;
import org.onehippo.cms7.essentials.site.components.service.ctx.SearchContext;

/**
 * @version "$Id: QueryFilter.java 157405 2013-03-08 09:15:49Z mmilicevic $"
 */
public interface QueryFilter {
    void apply(HstQuery query, SearchContext context);
}
