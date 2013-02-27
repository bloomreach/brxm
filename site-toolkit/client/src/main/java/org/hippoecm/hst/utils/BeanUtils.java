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

import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManagerImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoResultSetBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.hst.security.HstSubject;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ContentBeanUtils;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class containing utility methods for Beans
 * 
 */

public class BeanUtils {

    private final static Logger log = LoggerFactory.getLogger(BeanUtils.class);

    private static final String DISPOSABLE_SESSION_KEY_PREFIX = BeanUtils.class.getName() + ";disposableSession";
    

    /**
     * <p>
     * Returns the {@link HippoFacetNavigationBean} for this {@link HstRequest} and <code>relPath</code> where it is accounted for the free text <code>query</code>. When <code>query</code> is <code>null</code> or
     * empty, we return the HippoFacetNavigationBean without free text search. Else, we try to return the HippoFacetNavigationBean with free text search. If the 
     * HippoFacetNavigationBean does not exist in the faceted navigation tree in combination with the free text search, we return <code>null</code>. 
     * </p>
     * <p>
     * <b>Note</b> we can only return the HippoFacetNavigationBean if the current {@link ResolvedSiteMapItem} has a {@link ResolvedSiteMapItem#getRelativeContentPath()} that
     * points to points to a {@link HippoFacetNavigationBean} or to some lower descendant {@link HippoBean}, for example to a {@link HippoResultSetBean}. In this latter case,
     * we traverse up until we find a  {@link HippoFacetNavigationBean}, and return that one.
     * </p>
     * <p>
     * The <code>relPath</code> is relative to the site content base path and <b>must not</b> start with a /
     * </p>
     * 
     * If some exception happens, like we cannot get a disposable pooled session, we throw a {@link HstComponentException}
     * 
     * @param hstRequest the hstRequest
     * @param relPath the relative path to the faceted navigation node, which must not start with a / and is relative to the site content base path
     * @param query the free text query that should be accounted for for this <code>facNavBean</code> 
     * @param objectConverter the objectConverter to be used
     * @return the <code>HippoFacetNavigationBean</code> accounted for this <code>query</code> and <code>null</code> if we could not find the HippoFacetNavigationBean when the <code>query</code> is applied
     * @throws HstComponentException
     */
    public static HippoFacetNavigationBean getFacetNavigationBean(HstRequest hstRequest, String relPath, String query, ObjectConverter objectConverter) throws HstComponentException {        
        String base = PathUtils.normalizePath(hstRequest.getRequestContext().getResolvedMount().getMount().getContentPath());
        
        if(relPath == null) {
            log.warn("Cannot return a content bean for relative path null for resolvedSitemapItem belonging to '{}'. Return null", hstRequest.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getId());
            return null;
        }
        
        String absPath = "/"+base;
        if(!"".equals(relPath)) {
            absPath += "/" + relPath;
        }
        
        Task facnavTask = null;
        try {
            if (HDC.isStarted()) {
                facnavTask = HDC.getCurrentTask().startSubtask("RootFacetedNavigation Node");
            }
            if(query == null || "".equals(query)) {
                if (facnavTask != null) {
                    facnavTask.setAttribute("no free query", "");
                }
                try {
                    ObjectBeanManager objectBeanMngr = new ObjectBeanManagerImpl(hstRequest.getRequestContext().getSession(), objectConverter);
                    HippoBean bean  = (HippoBean)objectBeanMngr.getObject(absPath);
                    if(bean == null) {
                        log.info("Cannot return HippoFacetNavigationBean for path '{}'. Return null", absPath);
                        return null;
                    }

                    while(bean != null && !(bean instanceof HippoFacetNavigationBean)) {
                        log.debug("Bean for '{}' is not instance of 'HippoFacetNavigationBean'. Let's check it's parent. ", bean.getPath());
                        if(bean.getPath().equals("/"+base)) {
                            // we are at the sitebase and did not find a HippoFacetNavigationBean. return null
                            log.info("We did not find a 'HippoFacetNavigationBean' somewhere in the path below '{}'. Return null", absPath);
                            return null;
                        }
                        bean = bean.getParentBean();
                    }
                    return (HippoFacetNavigationBean)bean;

                } catch (ObjectBeanManagerException e) {
                    throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"'", e);
                } catch (RepositoryException e) {
                    throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"'", e);
                }
            }

            if (facnavTask != null) {
                facnavTask.setAttribute("free query", query);
            }

            // we have free text search. Now, we have to fetch from the root every descendant one-by-one until we hit a FacetedNavigationNode.

            // first, let's get a disposable session:
            Session disposablePoolSession = getDisposablePoolSession(hstRequest, query);
            ObjectBeanManager objectBeanMngr = new ObjectBeanManagerImpl(disposablePoolSession, objectConverter);

            HippoFacetNavigationBean facetNavBean = null;

            // first, with the original session which is not tied to THIS free text faceted navigation, we need to get the
            // faceted navigation node. We CANNOT do this with the disposablePoolSession because then we TIE the faceted navigation
            // without free text search already to the disposablePoolSession
            try {
                Node siteBaseNode = (Node)hstRequest.getRequestContext().getSession().getItem("/"+base);
                Node stepInto = siteBaseNode;
                String[] pathElements = relPath.split("/");
                for(int i = 0; i < pathElements.length ; i++) {
                    if(facetNavBean == null) {
                        stepInto = stepInto.getNode(pathElements[i]);
                        if(stepInto.isNodeType("hippofacnav:facetnavigation")) {
                            // we found the faceted navigation node! Now, append the free text search
                            // note we get the faceted navigation now with the object bean mngr backed by disposablePoolSession
                            facetNavBean = (HippoFacetNavigationBean)objectBeanMngr.getObject(stepInto.getPath() + "[{"+query+"}]");
                        }
                    } else {
                        // if the child path element still returns a faceted navigation element we continue, otherwise we break.
                        String nextPath = facetNavBean.getPath() + "/" + pathElements[i];
                        // note we get the faceted navigation now with the object bean mngr backed by disposablePoolSession
                        Object o = objectBeanMngr.getObject(nextPath);
                        if(o instanceof HippoFacetNavigationBean) {
                            facetNavBean = (HippoFacetNavigationBean)o;
                        }
                        if(o instanceof HippoResultSetBean) {
                            // we can stop, we are in the resultset
                            break;
                        }
                        if(o == null) {
                            // the path did not resolve to a bean. Thus, the path is incorrect. Return null.
                            facetNavBean = null;
                            break;
                        }
                    }
                }
                if(facetNavBean == null) {
                    log.info("We did not find a HippoFacetNavigationBean for path '{}' and query '{}'. Return null.",absPath, query);
                }
            } catch (PathNotFoundException e) {
                throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"' and query '"+query+"'", e);
            } catch (RepositoryException e) {
                throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"' and query '"+query+"'", e);
            } catch (ObjectBeanManagerException e) {
                throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"' and query '"+query+"'", e);
            }
            return facetNavBean;
        } finally {
            if (facnavTask != null) {
                facnavTask.stop();
            }
        }

    }
    
    /**
     * <p>
     * Returns the {@link HippoFacetNavigationBean} for this {@link HstRequest} from the {@link ResolvedSiteMapItem} where it is accounted for the free text <code>query</code>. When <code>query</code> is <code>null</code> or
     * empty, we return the HippoFacetNavigationBean without free text search. Else, we try to return the HippoFacetNavigationBean with free text search. If the 
     * HippoFacetNavigationBean does not exist in the faceted navigation tree in combination with the free text search, we return <code>null</code>. 
     * </p>
     * <p>
     * <b>Note</b> we can only return the HippoFacetNavigationBean if the current {@link ResolvedSiteMapItem} has a {@link ResolvedSiteMapItem#getRelativeContentPath()} that
     * points to points to a {@link HippoFacetNavigationBean} or to some lower descendant {@link HippoBean}, for example to a {@link HippoResultSetBean}. In this latter case,
     * we traverse up until we find a  {@link HippoFacetNavigationBean}, and return that one.
     * </p>
     * <p>
     * <b>If</b> you need a {@link HippoFacetNavigationBean} that is not on for the  {@link ResolvedSiteMapItem}, but at some fixed location,
     * you can use {@link #getFacetNavigationBean(HstRequest, String, String, ObjectConverter)}
     * </p>
     * 
     * If some exception happens, like we cannot get a disposable pooled session, we throw a {@link HstComponentException}
     * 
     * @param hstRequest the hstRequest
     * @param query the free text query that should be accounted for for this <code>facNavBean</code> 
     * @param objectConverter the objectConverter to be used
     * @return the <code>HippoFacetNavigationBean</code> accounted for this <code>query</code> and <code>null</code> if we could not find the HippoFacetNavigationBean when the <code>query</code> is applied
     * @throws HstComponentException
     */
    public static HippoFacetNavigationBean getFacetNavigationBean(HstRequest hstRequest, String query, ObjectConverter objectConverter) throws HstComponentException {
        ResolvedSiteMapItem resolvedSiteMapItem = hstRequest.getRequestContext().getResolvedSiteMapItem();
        String relPath = PathUtils.normalizePath(resolvedSiteMapItem.getRelativeContentPath());
        return getFacetNavigationBean(hstRequest, relPath, query, objectConverter);
    }

    /**
     * Same as  {@link #getFacetNavigationBean(HstRequest, String, ObjectConverter)} only now instead of a {@link String} query we 
     * pass in a {@link HstQuery}
     * @see {@link #getFacetNavigationBean(HstRequest, String, ObjectConverter)}
     * @param hstRequest the hstRequest
     * @param query a {@link HstQuery} object. If <code>null</code> the call returns as if there is no query
     * @param objectConverter the objectConverter to be used
     * @return the <code>HippoFacetNavigationBean</code> accounted for this <code>query</code> and <code>null</code> if we could not find the HippoFacetNavigationBean when the <code>query</code> is applied
     * @throws HstComponentException
     */
    public static HippoFacetNavigationBean getFacetNavigationBean(HstRequest hstRequest, HstQuery query, ObjectConverter objectConverter) throws HstComponentException {
        String queryAsString = null;
        if(query == null) {
            return getFacetNavigationBean(hstRequest, queryAsString, objectConverter);
        }
        try {
            queryAsString = "xpath("+query.getQueryAsString(true)+")";
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
        return getFacetNavigationBean(hstRequest, queryAsString, objectConverter);
    }
    
    /**
     * Same as  {@link #getFacetNavigationBean(HstRequest, HstQuery, ObjectConverter)} only now instead of having the faceted navigation
     * node from the {@link ResolvedSiteMapItem} we add a <code>relPath</code> where it should be found
     * @see {@link #getFacetNavigationBean(HstRequest, String, ObjectConverter)}
     * @param hstRequest the hstRequest
     * @param query a {@link HstQuery} object
     * @param relPath the relative path from site base content to the faceted navigation node, which must not start with a / and is relative to the site content base path  
     * @param objectConverter the objectConverter to be used
     * @return the <code>HippoFacetNavigationBean</code> accounted for this <code>query</code> and <code>relPath</code> and <code>null</code> if we could not find the HippoFacetNavigationBean when the <code>query</code> is applied
     * @throws HstComponentException
     */
    public static HippoFacetNavigationBean getFacetNavigationBean(HstRequest hstRequest, HstQuery query, String relPath, ObjectConverter objectConverter) throws HstComponentException {
        String queryAsString = null;
        try {
            queryAsString = "xpath("+query.getQueryAsString(true)+")";
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
        return getFacetNavigationBean(hstRequest, relPath, queryAsString, objectConverter);
    }
        
    
    /**
     * Tries to return a bean that is located in a faceted navigation tree below a result set. When it cannot be found,
     * or the bean is not of type <code>beanMappingClass</code>, <code>null</code> will be returned.
     * 
     * @param <T>
     * @param hstRequest the hstRequest
     * @param query the free text search as String that is used for this faceted navigation
     * @param objectConverter
     * @param beanMappingClass the class T must be of 
     * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
     */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(HstRequest hstRequest, String query,
            ObjectConverter objectConverter, Class<T> beanMappingClass)  {
        
        ResolvedSiteMapItem resolvedSiteMapItem = hstRequest.getRequestContext().getResolvedSiteMapItem();
        String relPath = PathUtils.normalizePath(resolvedSiteMapItem.getRelativeContentPath());
        
        return getFacetedNavigationResultDocument(hstRequest, query, relPath, objectConverter, beanMappingClass);
    }

    /**
     * Tries to return a bean that is located in a faceted navigation tree below a result set. When it cannot be found,
     * or the bean is not of type <code>beanMappingClass</code>, <code>null</code> will be returned.
     * 
     * @param <T>
     * @param hstRequest the hstRequest
     * @param query the free text search as String that is used for this faceted navigation. The query should already
     *              have been processed by {@link org.hippoecm.hst.util.SearchInputParsingUtils#parse(String, boolean)}
     *              if necessary.
     * @param relPath the relative path from site base content to the faceted navigation node, which must not start with a / and is relative to the site content base path
     * @param objectConverter
     * @param beanMappingClass the class T must be of 
     * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
     */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(final HstRequest hstRequest, final String query, final String relPath,
            final ObjectConverter objectConverter, final Class<T> beanMappingClass)  {
        
        String base = PathUtils.normalizePath(hstRequest.getRequestContext().getResolvedMount().getMount().getContentPath());
        
        if(relPath == null) {
            log.warn("Cannot return a content bean for relative path null for resolvedSitemapItem belonging to '{}'. Return null", hstRequest.getRequestContext().getResolvedSiteMapItem().getHstSiteMapItem().getId());
            return null;
        }
        
        String absPath = "/"+base;
        if(!"".equals(relPath)) {
            absPath += "/" + relPath;
        }
        
        if(StringUtils.isEmpty(query)) {
           try {
                ObjectBeanManager objectBeanMngr = new ObjectBeanManagerImpl(hstRequest.getRequestContext().getSession(), objectConverter);
                HippoBean bean  = (HippoBean)objectBeanMngr.getObject(absPath);
                if(bean == null) {
                    log.info("Cannot return Document below faceted navigation for path '{}'. Return null", absPath);
                    return null;
                }
                if(!beanMappingClass.isAssignableFrom(bean.getClass())) {
                    log.debug("Expected bean of type '{}' but found of type '{}'. Return null.", beanMappingClass.getName(), bean.getClass().getName());
                    return null;
                }
                return (T)bean;   
            } catch (ObjectBeanManagerException e) {
                throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"'", e);
            } catch (RepositoryException e) {
                throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"'", e);
            }
        }
        
        // we have free text search. Now, we have to fetch from the root every descendant one-by-one until we hit a FacetedNavigationNode. 
        
        // first, let's get a disposable session:
        Session disposablePoolSession = getDisposablePoolSession(hstRequest, query);
        ObjectBeanManager objectBeanMngr = new ObjectBeanManagerImpl(disposablePoolSession, objectConverter);
        
        HippoFacetNavigationBean facetNavBean = null;
        
        // first, with the original session which is not tied to THIS free text faceted navigation, we need to get the 
        // faceted navigation node. We CANNOT do this with the disposablePoolSession because then we TIE the faceted navigation
        // without free text search already to the disposablePoolSession
        try {
            Node siteBaseNode = (Node)hstRequest.getRequestContext().getSession().getItem("/"+base);
            Node stepInto = siteBaseNode;
            String[] pathElements = relPath.split("/");
            
            
            // find the faceted navigation node with free text search first:
            
            String remainderPath = null;
            
            for(int i = 0; i < pathElements.length ; i++) {
                if(facetNavBean == null) {
                    stepInto = stepInto.getNode(pathElements[i]);
                    if(stepInto.isNodeType("hippofacnav:facetnavigation")) {
                        // we found the faceted navigation node! Now, append the free text search
                        // note we get the faceted navigation now with the object bean mngr backed by disposablePoolSession
                        facetNavBean = (HippoFacetNavigationBean)objectBeanMngr.getObject(stepInto.getPath() + "[{"+query+"}]");
                    }
                } else {
                    if(remainderPath == null) {
                        remainderPath = pathElements[i];
                    } else {
                        remainderPath += "/"+pathElements[i];
                    }
                }
            }
            if(facetNavBean == null) {
                log.info("We did not find a Document in the faceted navigation for path '{}' and query '{}'. Return null.",absPath, query);
                return null;
            } else {
                // now we have the faceted navigation bean with search. Let's try to fetch the remainder path
                T bean = facetNavBean.getBean(remainderPath, beanMappingClass);
                if(bean == null) {
                    log.info("We did not find a Document in the faceted navigation for path '{}' and query '{}'. Return null.",absPath, query);
                }
                return bean;
            }
        } catch (PathNotFoundException e) {
            throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"' and query '"+query+"'", e);
        } catch (RepositoryException e) {
            throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"' and query '"+query+"'", e);
        } catch (ObjectBeanManagerException e) {
            throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"' and query '"+query+"'", e);
        }
    }
        
    
    /**
     * Tries to return a bean that is located in a faceted navigation tree below a result set. When it cannot be found,
     * or the bean is not of type <code>beanMappingClass</code>, <code>null</code> will be returned.
     * 
     * @param <T>
     * @param hstRequest the hstRequest
     * @param query the free text search as {@link HstQuery} that is used for this faceted navigation
     * @param objectConverter
     * @param beanMappingClass the class T must be of 
     * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
     */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(HstRequest hstRequest, HstQuery query,
            ObjectConverter objectConverter, Class<T> beanMappingClass)  {
        
        if(query == null) {
            return getFacetedNavigationResultDocument(hstRequest, (String)null, objectConverter, beanMappingClass);
        }
        
        String queryAsString = null;
        try {
            queryAsString = "xpath("+query.getQueryAsString(true)+")";
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
        return getFacetedNavigationResultDocument(hstRequest, queryAsString, objectConverter, beanMappingClass);
    }
    
    /**
     * Tries to return a bean that is located in a faceted navigation tree below a result set. When it cannot be found,
     * or the bean is not of type <code>beanMappingClass</code>, <code>null</code> will be returned.
     * 
     * @param <T>
     * @param hstRequest the hstRequest
     * @param query the free text search as {@link HstQuery} that is used for this faceted navigation
     * @param relPath the relative path from site base content to the faceted navigation node, which must not start with a / and is relative to the site content base path
     * @param objectConverter
     * @param beanMappingClass the class T must be of 
     * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
     */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(HstRequest hstRequest, HstQuery query, String relPath, 
            ObjectConverter objectConverter, Class<T> beanMappingClass)  {
        
        if(query == null) {
            return getFacetedNavigationResultDocument(hstRequest, (String)null, relPath, objectConverter, beanMappingClass);
        }
        
        String queryAsString = null;
        try {
            queryAsString = "xpath("+query.getQueryAsString(true)+")";
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
        return getFacetedNavigationResultDocument(hstRequest, queryAsString, relPath, objectConverter, beanMappingClass);
    }


    /**
     * This method tries to get a {@link Session} from a disposable pool which is identified by <code>disposablePoolIdentifier</code>
     *
     * If <code>disposablePoolIdentifier</code> is empty or <code>null</code> an HstComponentException will be thrown. If it is not possible to return a 
     * {@link Session} for the <code>disposablePoolIdentifier</code>, for example because there is configured a MultipleRepositoryImpl instead of 
     * LazyMultipleRepositoryImpl, also a {@link HstComponentException} will be thrown.
     *
     *
     * @param requestContext the hstRequest for this HstComponent
     * @param disposablePoolIdentifier the identifier for this disposable pool. It is not allowed to be empty or <code>null</code> 
     * @return a jcr {@link Session} from a disposable pool 
     * @throws HstComponentException
     */
    public static Session getDisposablePoolSession(HstRequestContext requestContext, String disposablePoolIdentifier) throws HstComponentException {

        try {
            String userID = null;
            Credentials cred = null;
            // if there exists subject based session, do use the credentials of that session to create session from disposable pool
            Session existingSession = requestContext.getSession(false);
            if (existingSession instanceof LazySession) {
                existingSession.getUserID();
                Subject subject = HstSubject.getSubject(null);
                if (subject != null) {
                    Set<Credentials> repoCredsSet = subject.getPrivateCredentials(Credentials.class);
                    if (!repoCredsSet.isEmpty()) {
                        cred = repoCredsSet.iterator().next();
                        // this userID does not contain the mandatory credential domain separator needed for the lazy pools
                        // hence we append it with [separator]lazy, for example @lazy
                        userID = ((SimpleCredentials) cred).getUserID() +
                                requestContext.getContextCredentialsProvider().getCredentialsDomainSeparator() + "lazy";
                    }
                }
            }
            if (cred == null) {
                cred = requestContext.getContextCredentialsProvider().getDefaultCredentials(requestContext);
                userID = ((SimpleCredentials) cred).getUserID();
            }

            char[] passwd = ((SimpleCredentials) cred).getPassword();

            String disposablePoolSessionUserId = userID + ";" + disposablePoolIdentifier + ";disposable";

            String disposableKey = DISPOSABLE_SESSION_KEY_PREFIX + ";" + disposablePoolSessionUserId;
            Session session = (Session) requestContext.getAttribute(disposableKey);
            if (session != null) {
                log.debug("There is already a disposable session for '{}' on the request context. Return that session", disposablePoolSessionUserId);
                return session;
            }
            SimpleCredentials disposablePoolSessionCredentials = new SimpleCredentials(disposablePoolSessionUserId, passwd);
            Repository repo = HstServices.getComponentManager().getComponent(Repository.class.getName());

            session = repo.login(disposablePoolSessionCredentials);
            requestContext.setAttribute(disposableKey, session);
            return session;
        } catch (RepositoryException e) {
            throw new HstComponentException(e);
        }
    }

    /**
     * 
     * @param hstRequest the current {@link HstRequest}
     * @param disposablePoolIdentifier the identifier for this disposable pool. It is not allowed to be empty or <code>null</code>  
     * @return a jcr {@link Session} from a disposable pool
     * @throws HstComponentException
     * @see {@link #getDisposablePoolSession(org.hippoecm.hst.core.request.HstRequestContext, String)}
     */
    public static Session getDisposablePoolSession(HstRequest hstRequest, String disposablePoolIdentifier) throws HstComponentException {
        return getDisposablePoolSession(hstRequest.getRequestContext(), disposablePoolIdentifier);
     }

}
