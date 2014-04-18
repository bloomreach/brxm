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

package org.onehippo.cms7.essentials.components;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.ContentBeanUtils;
import org.onehippo.cms7.essentials.components.info.EssentialsBlogAuthorPostsComponentInfo;
import org.onehippo.cms7.essentials.components.model.AuthorEntry;
import org.onehippo.cms7.essentials.components.model.Authors;
import org.onehippo.cms7.essentials.components.paging.DefaultPagination;
import org.onehippo.cms7.essentials.components.paging.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsBlogAuthorPostsComponentInfo.class)
public class EssentialsBlogAuthorPostsComponent extends EssentialsListComponent {

    public static final int DEFAULT_SEARCH_DEPTH = 3;
    private static Logger log = LoggerFactory.getLogger(EssentialsBlogAuthorPostsComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        log.info("**** BLOG AUTHOR COMPONENT ****");
        final HstRequestContext context = request.getRequestContext();
        final HippoBean document = context.getContentBean();
        if (document instanceof Authors) {
            final Authors entry = (Authors) document;
            final List<? extends AuthorEntry> authors = entry.getAuthors();
            if (authors.size() > 0) {
                // NOTE: most use-cases will only have one author,
                // so for convenience purposes  also add first author on request
                request.setAttribute("author", authors.get(0));
                request.setAttribute("authors", authors);
                final Class<? extends HippoBean> clazz = getPrimaryType(context, document);
                final EssentialsBlogAuthorPostsComponentInfo paramInfo = getComponentParametersInfo(request);
                final int limit = paramInfo.getPageSize();
                final String sortField = paramInfo.getSortField();
                final List<HippoBean> beans = new ArrayList<>(limit);
                final HippoBean scopeBean = getScopeBean(paramInfo.getScope());
                try {
                    for (AuthorEntry author : authors) {
                        final HstQuery hstQuery = ContentBeanUtils.createIncomingBeansQuery(author, scopeBean, getSearchDepth(), clazz, true);
                        hstQuery.setLimit(limit);
                        hstQuery.addOrderByDescending(sortField);
                        final HippoBeanIterator it = hstQuery.execute().getHippoBeans();
                        while (it.hasNext()) {
                            final HippoBean bean = it.nextHippoBean();
                            if (!document.isSelf(bean)) {
                                beans.add(bean);
                            }
                        }
                        if (beans.size() >= limit) {
                            break;
                        }

                    }
                    final Pageable<HippoBean> pageable = new DefaultPagination<>(beans);
                    request.setAttribute(REQUEST_ATTR_PAGEABLE, pageable);
                } catch (QueryException e) {
                    log.error("Error fetching posts by authors", e);
                }
                request.setAttribute(REQUEST_ATTR_PARAM_INFO, paramInfo);
            }
        }
    }

    private Class<? extends HippoBean> getPrimaryType(final HstRequestContext context, final HippoBean document) {
        try {
            final ObjectConverter converter = context.getContentBeansTool().getObjectConverter();
            final String primaryObjectType = converter.getPrimaryObjectType(document.getNode());
            return converter.getAnnotatedClassFor(primaryObjectType);

        } catch (ObjectBeanManagerException e) {
            log.error("Error fetching primary node type", e);
        }
        return null;
    }


    private int getSearchDepth() {
        return DEFAULT_SEARCH_DEPTH;
    }

}
