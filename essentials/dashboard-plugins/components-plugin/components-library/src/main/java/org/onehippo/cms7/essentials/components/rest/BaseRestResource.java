package org.onehippo.cms7.essentials.components.rest;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocumentIterator;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.content.beans.standard.HippoHtmlBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.jaxrs.services.AbstractResource;
import org.hippoecm.hst.util.PathUtils;
import org.onehippo.cms7.essentials.components.rest.common.RestList;
import org.onehippo.cms7.essentials.components.rest.common.Restful;
import org.onehippo.cms7.essentials.components.rest.ctx.RestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: BaseRestResource.java 174726 2013-08-22 14:24:50Z mmilicevic $"
 */
public abstract class BaseRestResource extends AbstractResource {

    public static final String INVALID_SCOPE = "Invalid scope";
    private static Logger log = LoggerFactory.getLogger(BaseRestResource.class);
    //

    /**
     * Return HippoQuery which scope is site root
     *
     * @param context context
     * @param clazz   zero or more scope classes
     * @return HstQuery instance
     */

    @SuppressWarnings("unchecked")
    public HstQuery createQuery(final RestContext context, final Class<? extends HippoBean> clazz) {
        HstQuery query = null;
        try {
            Node scopeNode = getScopeForContext(context);
            query = getHstQueryManager(context.getRequestContext()).createQuery(scopeNode, clazz);
            query.setLimit(context.getResultLimit());
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
        // HippoBean bena = getMountContentBaseBean(getRequestContext(request));
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

    protected <T extends HippoBean> Restful<T> populateResult(RestContext context, HstQuery query, Class<? extends Restful<T>> clazz) throws QueryException {
        Restful<T> restful = createInstance(clazz);
        T bean = getSingleBean(query);
        if (bean != null) {
            restful.fromHippoBean(bean, context);
            return restful;
        }
        return restful;
    }

    @SuppressWarnings("unchecked")
    protected <T extends HippoBean> T getSingleBean(HstQuery query) throws QueryException {
        final HstQueryResult results = query.execute();
        final HippoBeanIterator beans = results.getHippoBeans();
        if (beans.hasNext()) {
            return  (T) beans.nextHippoBean();
        }

        return null;
    }

    protected <T extends HippoBean> List<T> populateBeans(HstQuery query) throws QueryException {
        final HstQueryResult results = query.execute();
        final HippoBeanIterator beans = results.getHippoBeans();
        List<T> retval = new ArrayList<>();
        if (beans.hasNext()) {
            @SuppressWarnings({"unchecked"})
            final T bean = (T) beans.nextHippoBean();
            if (bean != null) {
                retval.add(bean);
            }
        }

        return retval;
    }

    protected <T extends HippoBean> RestList<Restful<T>> populateResults(RestContext context, HstQuery query, Class<? extends Restful<T>> clazz) throws QueryException {
        final HstQueryResult results = query.execute();
        final HippoBeanIterator beans = results.getHippoBeans();
        final RestList<Restful<T>> retVal = newRestList();
        while (beans.hasNext()) {
            @SuppressWarnings("unchecked")
            final T bean = (T) beans.nextHippoBean();
            if (bean != null) {
                Restful<T> restful = createInstance(clazz);
                restful.fromHippoBean(bean, context);
                retVal.add(restful);
            }
        }
        return retVal;
    }

    public abstract <T extends HippoBean> RestList<Restful<T>> newRestList();

    protected <T extends HippoBean> RestList<Restful<T>> populateResultsFromFolder(RestContext context, final Class<T> clazz, Class<? extends Restful<T>> restFull) throws QueryException {
        final RestList<Restful<T>> retVal = newRestList();
        try {
            Node node = getScopeForContext(context);
            final ObjectConverter objectConverter = getObjectConverter(context.getRequestContext());
            HippoBean scopeNode = (HippoBean) objectConverter.getObject(node);
            if (scopeNode.isHippoFolderBean()) {
                HippoFolderBean folder = (HippoFolderBean) scopeNode;
                final HippoDocumentIterator<T> beans = folder.getDocumentIterator(clazz);
                while (beans.hasNext()) {
                    T bean = beans.next();
                    if (bean != null) {
                        Restful<T> restful = createInstance(restFull);
                        restful.fromHippoBean(bean, context);
                        retVal.add(restful);
                    }
                }
                return retVal;
            }
        } catch (RepositoryException e) {
            log.error("Error fetching folder beans ", e);
        } catch (ObjectBeanManagerException e) {
            log.error("Error fetching folder", e);
        }
        return retVal;

    }

    protected <T extends HippoBean> Restful<T> createInstance(Class<? extends Restful<T>> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException e) {
            log.error("Error instantiating", e);
        } catch (IllegalAccessException e) {
            log.error("Access exception", e);
        }
        return null;
    }


    public String parseHtml(RestContext context, HippoHtmlBean body) {
        if (body == null) {
            return null;
        }
        final String content = body.getContent();
        return getContentRewriter().rewrite(content, body.getNode(), context.getRequestContext());
    }
}
