/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.utils;

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.ContentBeanUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class containing utility methods for Beans
 * @deprecated  since 7.9.0 : use {@link ContentBeanUtils}
 */
@Deprecated
public class BeanUtils {

    private final static Logger log = LoggerFactory.getLogger(BeanUtils.class);

    /**
     * @deprecated since 7.9.0 : objectConverter and hstRequest not used any more.
     * use {@link ContentBeanUtils#getFacetNavigationBean(String, String)} instead
     */
    @Deprecated
    public static HippoFacetNavigationBean getFacetNavigationBean(HstRequest hstRequest, String relPath, String query, ObjectConverter objectConverter) throws HstComponentException {
        return ContentBeanUtils.getFacetNavigationBean(relPath, query);
    }

    /**
     * @deprecated since 7.9.0 : objectConverter and hstRequest not used any more.
     * use {@link ContentBeanUtils#getFacetNavigationBean(String)} instead
     */
    @Deprecated
    public static HippoFacetNavigationBean getFacetNavigationBean(HstRequest hstRequest, String query, ObjectConverter objectConverter) throws HstComponentException {
        return ContentBeanUtils.getFacetNavigationBean(query);
    }

    /**
     * Same as  {@link #getFacetNavigationBean(HstRequest, String, ObjectConverter)} only now instead of a {@link String} query we 
     * pass in a {@link HstQuery}
     * @deprecated since 7.9.0 : objectConverter and hstRequest not used any more.
     * use {@link ContentBeanUtils#getFacetNavigationBean(HstQuery)} instead
     */
    @Deprecated
    public static HippoFacetNavigationBean getFacetNavigationBean(HstRequest hstRequest, HstQuery query, ObjectConverter objectConverter) throws HstComponentException {
        String queryAsString = null;
        if(query == null) {
            return ContentBeanUtils.getFacetNavigationBean((String) null);
        }
        try {
            queryAsString = "xpath("+query.getQueryAsString(true)+")";
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
        return ContentBeanUtils.getFacetNavigationBean(queryAsString);
    }
    
    /**
     * @deprecated since 7.9.0 : objectConverter and hstRequest not used any more.
     * use {@link ContentBeanUtils#getFacetNavigationBean(HstQuery, String)} instead
     */
    @Deprecated
    public static HippoFacetNavigationBean getFacetNavigationBean(HstRequest hstRequest, HstQuery query, String relPath, ObjectConverter objectConverter) throws HstComponentException {
        String queryAsString = null;
        if(query == null) {
            return ContentBeanUtils.getFacetNavigationBean((String)null);
        }
        try {
            queryAsString = "xpath("+query.getQueryAsString(true)+")";
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
        return ContentBeanUtils.getFacetNavigationBean(relPath, queryAsString);
    }


    /**
     * @deprecated since 7.9.0 : objectConverter and hstRequest not used any more.
     * use {@link ContentBeanUtils#getFacetedNavigationResultDocument(String, Class<T>)} instead
     */
    @Deprecated
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(HstRequest hstRequest, String query,
            ObjectConverter objectConverter, Class<T> beanMappingClass)  {
        
        ResolvedSiteMapItem resolvedSiteMapItem = hstRequest.getRequestContext().getResolvedSiteMapItem();
        String relPath = PathUtils.normalizePath(resolvedSiteMapItem.getRelativeContentPath());
        
        return getFacetedNavigationResultDocument(hstRequest, query, relPath, objectConverter, beanMappingClass);
    }

    /**
     * @deprecated since 7.9.0 : objectConverter and hstRequest not used any more.
     * use {@link ContentBeanUtils#getFacetedNavigationResultDocument(String, String, Class<T>)} instead
     */
    @Deprecated
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(final HstRequest hstRequest, final String query, final String relPath,
            final ObjectConverter objectConverter, final Class<T> beanMappingClass)  {
        return ContentBeanUtils.getFacetedNavigationResultDocument(query, relPath, beanMappingClass);
    }


    /**
     * @deprecated since 7.9.0 : objectConverter and hstRequest not used any more.
     * use {@link ContentBeanUtils#getFacetedNavigationResultDocument(HstQuery, Class<T>)} instead
     */
    @Deprecated
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(HstRequest hstRequest, HstQuery query,
            ObjectConverter objectConverter, Class<T> beanMappingClass)  {
        return ContentBeanUtils.getFacetedNavigationResultDocument(query, beanMappingClass);
    }

    /**
     * @deprecated since 7.9.0 : objectConverter and hstRequest not used any more.
     * use {@link ContentBeanUtils#getFacetedNavigationResultDocument(HstQuery, String, Class<T>)} instead
     */
    @Deprecated
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(HstRequest hstRequest, HstQuery query, String relPath, 
            ObjectConverter objectConverter, Class<T> beanMappingClass)  {
        return ContentBeanUtils.getFacetedNavigationResultDocument(query, relPath, beanMappingClass);
    }


    /**
     * This method tries to get a {@link Session} from a disposable pool which is identified by
     * <code>disposablePoolIdentifier</code> or a {@link Session} from a security delegate
     * <p/>
     * If <code>sessionIdentifier</code> is empty or <code>null</code> an HstComponentException will be thrown.
     * If it is not possible to return a {@link Session} for the <code>sessionIdentifier</code>, for example
     * because there is configured a MultipleRepositoryImpl instead of LazyMultipleRepositoryImpl, also a {@link
     * HstComponentException} will be thrown.
     *
     * @param requestContext           the hstRequest for this HstComponent
     * @param sessionIdentifier the identifier for this disposable pool / session. It is not allowed to be empty or
     *                                 <code>null</code>
     * @return a jcr {@link Session} from a disposable pool
     * @throws HstComponentException
     *
     * @deprecated since 7.9.0 use {@link ContentBeanUtils#getDisposableSession(org.hippoecm.hst.core.request.HstRequestContext, String)}
     * instead
     */
    @Deprecated
    public static Session getDisposableSession(HstRequestContext requestContext, String sessionIdentifier) throws HstComponentException {
        return ContentBeanUtils.getDisposableSession(requestContext, sessionIdentifier);
    }

    /**
     *
     * @deprecated since 7.9.0 use {@link ContentBeanUtils#getPreviewCmsQuerySession(org.hippoecm.hst.core.request.HstRequestContext, String)}
     * instead
     */
    @Deprecated
    public static Session getPreviewCmsQuerySession(HstRequestContext requestContext, String sessionIdentifier) throws HstComponentException {
        return ContentBeanUtils.getPreviewCmsQuerySession(requestContext, sessionIdentifier);
    }

    /**
     *
     * @param hstRequest the current {@link HstRequest}
     * @param sessionIdentifier the identifier for this disposable pool session or for the session security delegate.
     *                          It is not allowed to be empty or <code>null</code>
     * @return a disposable jcr {@link Session}
     * @throws HstComponentException
     * @deprecated since 7.9.0 use {@link ContentBeanUtils#getDisposableSession(org.hippoecm.hst.core.request.HstRequestContext, String)}
     * instead
     */
    @Deprecated
    public static Session getDisposableSession(HstRequest hstRequest, String sessionIdentifier) throws HstComponentException {
        return ContentBeanUtils.getDisposableSession(hstRequest.getRequestContext(), sessionIdentifier);
    }

}
