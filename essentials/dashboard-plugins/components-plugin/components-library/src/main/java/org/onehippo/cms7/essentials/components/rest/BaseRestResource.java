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

package org.onehippo.cms7.essentials.components.rest;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoHtmlBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.services.AbstractResource;
import org.hippoecm.hst.util.PathUtils;
import org.onehippo.cms7.essentials.components.paging.DefaultPagination;
import org.onehippo.cms7.essentials.components.paging.IterablePagination;
import org.onehippo.cms7.essentials.components.paging.Pageable;
import org.onehippo.cms7.essentials.components.rest.ctx.RestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public abstract class BaseRestResource extends AbstractResource {

    public static final String INVALID_SCOPE = "Invalid scope";
    public static final String UNCHECKED = "unchecked";
    private static Logger log = LoggerFactory.getLogger(BaseRestResource.class);
    //


    protected <T extends HippoBean> Pageable<T> findBeans(final RestContext context, final Class<T> clazz)  {
        try {
            final HstQuery query = createQuery(context, clazz);
            final HstQueryResult execute = query.execute();
            return new IterablePagination<>(
                    execute.getHippoBeans(),
                    execute.getTotalSize(),
                    context.getPageSize(),
                    context.getPage());
        } catch (QueryException e) {
            log.error("Error finding beans", e);
        }
        return  DefaultPagination.emptyCollection();
    }


    protected Pageable<? extends HippoBean> executeQuery(final RestContext context, final HstQuery query) throws QueryException {
        final HstQueryResult execute = query.execute();
        return new IterablePagination<>(
                execute.getHippoBeans(),
                execute.getTotalSize(),
                context.getPageSize(),
                context.getPage());
    }

    /**
     * Return HippoQuery which scope is site root
     *
     * @param context context
     * @param clazz   zero or more scope classes
     * @return HstQuery instance
     */

    @SuppressWarnings(UNCHECKED)
    public HstQuery createQuery(final RestContext context, final Class<? extends HippoBean> clazz) {
        HstQuery query = null;
        try {
            Node scopeNode = getScopeForContext(context);
            query = getHstQueryManager(context.getRequestContext()).createQuery(scopeNode, clazz);
            final int pageSize = context.getPageSize();
            final int page = context.getPage();
            query.setLimit(pageSize);
            query.setOffset((page - 1) * pageSize);

        } catch (QueryException e) {
            log.error("Error creating HST query", e);
        } catch (RepositoryException e) {
            throw new WebServiceException(INVALID_SCOPE, e);
        }
        if (query == null) {
            throw new WebServiceException("Query was null (failed to create it)");
        }
        return query;
    }

    private Node getScopeForContext(final RestContext context) throws RepositoryException {
        Node scopeNode;
        final HttpServletRequest request = context.getRequest();
        if (context.getScope() == null) {
            scopeNode = getScope(request);
        } else {
            if (context.isAbsolutePath()) {
                final Node rootNode = context.getRequestContext().getSession().getRootNode();
                scopeNode = rootNode.getNode(StringUtils.removeStart(context.getScope(), "/"));
            } else {
                scopeNode = getScope(request, context.getScope());
            }
        }
        return scopeNode;
    }

    public Node getScope(final HttpServletRequest request) throws RepositoryException {
        HstRequestContext requestContext = getRequestContext(request);
        Mount siteMount = requestContext.getResolvedMount().getMount();
        if (siteMount == null) {
            log.error("Couldn't find site mount for rest service");
            return null;
        }
        String contentPath = siteMount.getContentPath();
        if (contentPath != null) {
            return requestContext.getSession().getRootNode().getNode(PathUtils.normalizePath(contentPath));
        }
        return null;
    }

    public Node getScope(final HttpServletRequest request, String relativePath) throws RepositoryException {
        final Node root = getScope(request);
        return root.getNode(relativePath);
    }


    @SuppressWarnings(UNCHECKED)
    protected <T extends HippoBean> T getSingleBean(HstQuery query) throws QueryException {
        final HstQueryResult results = query.execute();
        final HippoBeanIterator beans = results.getHippoBeans();
        if (beans.hasNext()) {
            return (T) beans.nextHippoBean();
        }

        return null;
    }

    protected <T extends HippoBean> List<T> populateBeans(HstQuery query) throws QueryException {
        final HstQueryResult results = query.execute();
        final HippoBeanIterator beans = results.getHippoBeans();
        List<T> retval = new ArrayList<>();
        if (beans.hasNext()) {
            @SuppressWarnings({UNCHECKED})
            final T bean = (T) beans.nextHippoBean();
            if (bean != null) {
                retval.add(bean);
            }
        }

        return retval;
    }


    public String parseHtml(RestContext context, HippoHtmlBean body) {
        if (body == null) {
            return null;
        }
        final String content = body.getContent();
        return getContentRewriter().rewrite(content, body.getNode(), context.getRequestContext());
    }
}
