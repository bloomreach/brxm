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
