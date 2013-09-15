package org.onehippo.cms7.essentials.components.utils.query;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * @version "$Id$"
 */
public interface QueryBuilder {

    @Nonnull
    HstQueryBuilder addFilter(Filter filter);

    @Nonnull
    HstQueryBuilder scope(HippoBean scope);

    @Nonnull
    HstQueryBuilder pageParam(String parameterName);

    @Nonnull
    HstQueryBuilder sizeParam(String parameterName);

    @Nonnull
    HstQueryBuilder page(int page);

    @Nonnull
    HstQueryBuilder size(int size);

    @Nonnull
    HstQueryBuilder siteScope();

    @SuppressWarnings("unchecked")
    @Nonnull
    HstQueryBuilder documents(Class<? extends HippoBean>... beans);

    @Nonnull
    HstQueryBuilder documents(String... primaryNodeTypes);

    @Nonnull
    HstQueryBuilder includeSubtypes();

    @Nonnull
    HstQueryBuilder excludeSubtypes();

    @Nullable
    HstQuery build();

    @Nullable
    HippoBean getScope();

    @Nullable
    HippoBean getSiteScope();
}
