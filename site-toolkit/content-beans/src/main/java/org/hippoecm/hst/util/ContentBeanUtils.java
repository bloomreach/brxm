/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.util;

import java.util.ArrayList;
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
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoResultSetBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.jcr.SessionSecurityDelegation;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.hst.security.HstSubject;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentBeanUtils {

    private final static Logger log = LoggerFactory.getLogger(ContentBeanUtils.class);

    private static final String DISPOSABLE_SESSION_KEY_PREFIX = ContentBeanUtils.class.getName() + ";disposableSession";


    private ContentBeanUtils() {
    }

    /**
     * Determines if the class or interface represented by this content bean object is either the same as, or is a 
     * subclass of, the class or interface represented by the specified fully qualified class name parameter.
     * It returns true if so; otherwise it returns false. 
     * @param bean content bean object
     * @param typeName fully qualified class name or simple class name
     * @return
     */
    public static boolean isBeanType(Object bean, String typeName) {
        if (bean == null || StringUtils.isEmpty(typeName)) {
            return false;
        }

        Class<?> beanType = bean.getClass();
        String beanFqcn = beanType.getName();

        if (StringUtils.equals(beanFqcn, typeName)) {
            return true;
        }

        try {
            Class<?> type = Thread.currentThread().getContextClassLoader().loadClass(typeName);

            if (type.isAssignableFrom(bean.getClass())) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            log.debug("Type not found.", e);
            return false;
        }

        return false;
    }

    /**
     * @deprecated since 7.9.0 : objectConverter not used any more.
     * use {@link #createIncomingBeansQuery(HippoDocumentBean, HippoBean, String, Class, boolean)} instead
     */
    @Deprecated
    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope,
                                                    String linkPath, ObjectConverter converter,
                                                    Class<? extends HippoBean> beanMappingClass, boolean includeSubTypes) throws QueryException{
        return createIncomingBeansQuery(bean, scope, linkPath, beanMappingClass, includeSubTypes);

    }

    /**
     * Returns a HstQuery for incoming beans (incoming beans within scope {@code scope}). You can add filters and ordering to the query before executing it
     *  You need to add a <code>linkPath</code>: this is that path, that the incoming beans use to link to the HippoDocumentBean {@code bean}. For example, with 'myproject:link/@hippo:docbase' or even 'wildcard/@hippo:docbase' or
     * 'wildcard/wildcard/@hippo:docbase' where wildcard = *
     *
     * @param bean The HippoDocumentBean that you have, and for which you want to find the other beans that have a link to it (incoming beans)
     * @param scope the scope (hierarchical location) to search below for 'incoming beans'
     * @param linkPath the path where the 'incoming beans' have there link (mirror) stored, for example at myns:links/@hippo:docbase
     * @param beanMappingClass the type the 'incoming beans' should be of
     * @param includeSubTypes <code>true</code> when subtypes of beanMappingClass should be included in the result
     * @return a HstQuery that contains the constraints for 'incoming beans' to your <code>bean</code>
     * @throws QueryException
     */
    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope,
                                                    String linkPath,
                                                    Class<? extends HippoBean> beanMappingClass, boolean includeSubTypes) throws QueryException{

        List<String> linkPaths = new ArrayList<String>();
        linkPaths.add(linkPath);
        return createIncomingBeansQuery(bean, scope, linkPaths, beanMappingClass, includeSubTypes);

    }

    /**
     * @deprecated since 7.9.0 : objectConverter not used any more.
     * use {@link #createIncomingBeansQuery(HippoDocumentBean, HippoBean, int, Class, boolean)} instead
     */
    @Deprecated
    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope, int depth,
                                                    ObjectConverter converter, Class<? extends HippoBean> beanMappingClass,
                                                    boolean includeSubTypes) throws QueryException{

        return createIncomingBeansQuery(bean, scope, depth, beanMappingClass, includeSubTypes);
    }

    /**
     * Returns a HstQuery for incoming beans (incoming beans within scope {@code scope}). You can add filters and ordering to the query before executing it
     *  You need to add a <code>depth</code>: this is the maximum depth, that the incoming beans use to link to the HippoDocumentBean {@code bean}. For example, with 'myproject:link/@hippo:docbase' is depth 1,
     *  'myproject:somecompound/myproject:link/@hippo:docbase' is depth 2
     * @param bean The HippoDocumentBean that you have, and for which you want to find the other beans that have a link to it (incoming beans)
     * @param scope the scope (hierarchical location) to search below for 'incoming beans'
     * @param depth the <code>depth</code> until which the links below the HippoDocuments you want to find can be.  Maximum depth is 4, when larger, a QueryException is thrown
     * @param beanMappingClass the type the 'incoming beans' should be of
     * @param includeSubTypes <code>true</code> when subtypes of beanMappingClass should be included in the result
     * @return a HstQuery that contains the constraints for 'incoming beans' to your <code>bean</code>
     * @throws QueryException when <code>depth</code> is larger than 4
     */
    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope, int depth, Class<? extends HippoBean> beanMappingClass,
        boolean includeSubTypes) throws QueryException{
        if (depth < 0 || depth > 4) {
            throw new FilterException("Depth must be (including) between 0 and 4");
        }
        String path = "@hippo:docbase";
        List<String> linkPaths = new ArrayList<String>();
        linkPaths.add(path);
        for (int i = 1; i <= depth; i++) {
            path = "*/" + path;
            linkPaths.add(path);
        }
        return createIncomingBeansQuery(bean, scope, linkPaths, beanMappingClass, includeSubTypes);
     }

    /**
     * @deprecated since 7.9.0 : objectConverter not used any more.
     * use {@link #createIncomingBeansQuery(HippoDocumentBean, HippoBean, List, Class, boolean)} instead
     */
    @Deprecated
    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope,
                                                    List<String> linkPaths, ObjectConverter converter,
                                                    Class<? extends HippoBean> beanMappingClass, boolean includeSubTypes) throws QueryException{
        return createIncomingBeansQuery(bean, scope, linkPaths, beanMappingClass, includeSubTypes);
    }

    /**
     * Returns a HstQuery for incoming beans (incoming beans within scope {@code scope}). You can add filters and ordering to the query before executing it 
     * 
     * You need to add  <code>linkPaths</code>: these are the paths, that the incoming beans use to link to the HippoDocumentBean {@code bean}. For example, with 'myproject:link/@hippo:docbase' or even 'wildcard/@hippo:docbase' or 
     * 'wildcard/wildcard/@hippo:docbase' where wildcard = *
     * 
     * @param bean The HippoDocumentBean that you have, and for which you want to find the other beans that have a link to it (incoming beans)
     * @param scope the scope (hierarchical location) to search below for 'incoming beans'
     * @param linkPaths the paths where the 'incoming beans' have there link (mirror) stored, for example at {myns:links/@hippo:docbase, myns:alsolinks/@hippo:docbase }
     * @param beanMappingClass the type the 'incoming beans' should be of
     * @param includeSubTypes <code>true</code> when subtypes of beanMappingClass should be included in the result 
     * @return a HstQuery that contains the constraints for 'incoming beans' to your <code>bean</code>
     * @throws QueryException
     */
    public static HstQuery createIncomingBeansQuery(HippoDocumentBean bean, HippoBean scope,
            List<String> linkPaths,
            Class<? extends HippoBean> beanMappingClass, boolean includeSubTypes) throws QueryException{

        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext == null) {
            throw new QueryException("Cannot search without HstRequestContext");
        }
        String canonicalHandleUUID = bean.getCanonicalHandleUUID();
        HstQuery query = requestContext.getQueryManager().createQuery(scope, beanMappingClass, includeSubTypes);
        Filter filter = query.createFilter();
        for (String linkPath : linkPaths) {
            Filter orFilter = query.createFilter();
            orFilter.addEqualTo(linkPath, canonicalHandleUUID);
            filter.addOrFilter(orFilter);
        }
        query.setFilter(filter);
        return query;
    }

    /**
     * Returns a list of beans of type T (the same type as {@code beanMappingClass}) that have a (facet)link to the HippoDocumentBean {@code bean}. If no incoming beans are found, 
     * an <code>empty</code> list will be returned. 
     * 
     */
    public static <T extends HippoBean> List<T> getIncomingBeans(HstQuery query,
            Class<? extends HippoBean> beanMappingClass) throws QueryException {

        List<T> incomingBeans = new ArrayList<T>();

        HstQueryResult result = query.execute();
        HippoBeanIterator beans = result.getHippoBeans();
        while (beans.hasNext()) {
            T incomingBean = (T) beans.nextHippoBean();
            if(incomingBean == null) {
                continue;
            }
            if (!beanMappingClass.isAssignableFrom(incomingBean.getClass())) {
                // should not be possible
                log.warn("Found a bean not being of type or subtype of '{}'. Skip bean", beanMappingClass.getName());
                continue;
            }
            incomingBeans.add(incomingBean);
        }

        return incomingBeans;
    }

    /**
     * <p>
     * Returns the {@link org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean} for <code>absBasePath</code>, <code>relPath</code> and
     * accounted for the free text <code>query</code>. When <code>query</code> is <code>null</code> or
     * empty, the HippoFacetNavigationBean without free text search is returned. Else, a HippoFacetNavigationBean with free text search is returned. If the
     * HippoFacetNavigationBean does not exist in the faceted navigation tree in combination with the free text search, <code>null</code> is returned.
     * </p>
     * <p>
     * The <code>relPath</code> is relative to <code>absBasePath</code> and <b>must not</b> start with a /
     * </p>
     *
     * If some exception happens, a {@link org.hippoecm.hst.core.component.HstComponentException} is thrown
     *
     * @param absBasePath the absolute path (starting with /) from where to get the faceted navigation bean for <code>relPath</code> and
     *                     <code>query</code>. The <code>absBasePath</code> is NOT allowed to point to or to a descendant of a faceted navigation
     *                    node
     * @param relPath the relative path to the faceted navigation node (thus not start with a '/') and is relative to the site content base path
     * @param query the free text query that should be accounted for for this <code>facNavBean</code>, can be <code>null</code> in case of no query
     * @return
     * @throws HstComponentException
     */
    public static HippoFacetNavigationBean getFacetNavigationBean(final String absBasePath,final String relPath,final String query) throws HstComponentException {
        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext == null) {
            throw new HstComponentException("Cannot call #getFacetNavigationBean without HstRequestContext");
        }

        if(relPath == null) {
            log.warn("Cannot return a content bean for relative path null for resolvedSitemapItem belonging to '{}'. Return null", requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getId());
            return null;
        }

        String absPath = absBasePath;
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
                    HippoBean bean  = (HippoBean)requestContext.getObjectBeanManager().getObject(absPath);
                    if(bean == null) {
                        log.info("Cannot return HippoFacetNavigationBean for path '{}'. Return null", absPath);
                        return null;
                    }

                    while(bean != null && !(bean instanceof HippoFacetNavigationBean)) {
                        log.debug("Bean for '{}' is not instance of 'HippoFacetNavigationBean'. Let's check it's parent. ", bean.getPath());
                        if(bean.getPath().equals(absPath)) {
                            // we are at the sitebase and did not find a HippoFacetNavigationBean. return null
                            log.info("We did not find a 'HippoFacetNavigationBean' somewhere in the path below '{}'. Return null", absPath);
                            return null;
                        }
                        bean = bean.getParentBean();
                    }
                    return (HippoFacetNavigationBean)bean;

                } catch (ObjectBeanManagerException e) {
                    throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absPath+"'", e);
                }
            }

            if (facnavTask != null) {
                facnavTask.setAttribute("free query", query);
            }

            // we have free text search. Now, we have to fetch from the root every descendant one-by-one until we hit a FacetedNavigationNode.

            // first, let's get a disposable/query session:
            Session querySession = getDisposableSession(requestContext, query);
            ObjectBeanManager objectBeanMngr = requestContext.getObjectBeanManager(querySession);

            HippoFacetNavigationBean facetNavBean = null;

            // first, with the original session which is not tied to THIS free text faceted navigation, we need to get the
            // faceted navigation node. We CANNOT do this with the disposablePoolSession because then we TIE the faceted navigation
            // without free text search already to the disposablePoolSession
            try {
                Node baseNode = (Node)requestContext.getSession().getItem(absBasePath);
                Node stepInto = baseNode;
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
                    log.info("We did not find a HippoFacetNavigationBean for path '{}' and query '{}'. Return null.",absBasePath, query);
                }
            } catch (PathNotFoundException e) {
                throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absBasePath+"', relPath '"+relPath+"' and query '"+query+"'", e);
            } catch (RepositoryException e) {
                throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absBasePath+"', relPath '"+relPath+"'  and query '"+query+"'", e);
            } catch (ObjectBeanManagerException e) {
                throw new HstComponentException("Could not get the HippoFacetNavigationBean for '"+absBasePath+"', relPath '"+relPath+"'  and query '"+query+"'", e);
            }
            return facetNavBean;
        } finally {
            if (facnavTask != null) {
                facnavTask.stop();
            }
        }
    }

    /**
     * @see {@link #getFacetNavigationBean(String, String, String)} with <code>absBasePath</code> as "/"+ requestContext.getSiteContentBasePath();
     */
    public static HippoFacetNavigationBean getFacetNavigationBean(String relPath, String query) throws HstComponentException {
        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext == null) {
            throw new HstComponentException("Cannot call #getFacetNavigationBean without HstRequestContext");
        }

        if(relPath == null) {
            log.warn("Cannot return a content bean for relative path null for resolvedSitemapItem belonging to '{}'. Return null", requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getId());
            return null;
        }

        String absBasePath = "/"+ requestContext.getSiteContentBasePath();
        return getFacetNavigationBean(absBasePath, relPath, query);
    }

    /**
     * @see {@link #getFacetNavigationBean(String, String, String)} with <code>absBasePath</code> as "/"+ requestContext.getSiteContentBasePath() and
     * <code>relPath </code> as requestContext.getResolvedSiteMapItem().getRelativeContentPath()
     */
    public static HippoFacetNavigationBean getFacetNavigationBean(String query) throws HstComponentException {
        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext == null) {
            throw new HstComponentException("Cannot call #getFacetNavigationBean without HstRequestContext");
        }
        ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
        String relPath = PathUtils.normalizePath(resolvedSiteMapItem.getRelativeContentPath());
        return getFacetNavigationBean(relPath, query);
    }

    /**
     * Same as  {@link #getFacetNavigationBean(String)} only now instead of a {@link String} query we
     * pass in a {@link HstQuery}
     * @see {@link #getFacetNavigationBean(String)}
     * */
    public static HippoFacetNavigationBean getFacetNavigationBean(HstQuery query) throws HstComponentException {
        if(query == null) {
            return getFacetNavigationBean((String)null);
        }
        try {
            String queryAsString = "xpath("+query.getQueryAsString(true)+")";
            return getFacetNavigationBean(queryAsString);
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
    }

    /**
     * Same as  {@link #getFacetNavigationBean(HstQuery)} only now instead of having the faceted navigation
     * node from the {@link ResolvedSiteMapItem} we add a <code>relPath</code> where it should be found
     * @see {@link #getFacetNavigationBean(String)}
     * @param query a {@link HstQuery} object
     * @param relPath the relative path from site base content to the faceted navigation node, which must not start with a / and is relative to the site content base path
     * @return the <code>HippoFacetNavigationBean</code> accounted for this <code>query</code> and <code>relPath</code> and <code>null</code> if we could not find the HippoFacetNavigationBean when the <code>query</code> is applied
     * @throws HstComponentException
     */
    public static HippoFacetNavigationBean getFacetNavigationBean(HstQuery query, String relPath) throws HstComponentException {
        if(query == null) {
            return getFacetNavigationBean((String)null);
        }
        try {
            String queryAsString = "xpath("+query.getQueryAsString(true)+")";
            return getFacetNavigationBean(relPath, queryAsString);
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
    }

    /**
     * Same as  {@link #getFacetNavigationBean(HstQuery)} only now instead of having the faceted navigation
     * node from the {@link ResolvedSiteMapItem} we add <code>absBasePath</code> and <code>relPath</code> where it should be found
     * @see {@link #getFacetNavigationBean(String)}
     * @param query a {@link HstQuery} object
     * @param absBasePath the absolute path (starting with /) from where to get the faceted navigation bean for <code>relPath</code> and
     *                     <code>query</code>. The <code>absBasePath</code> is NOT allowed to point to or to a descendant of a faceted navigation
     *                    node
     * @param relPath the relative path from absBasePath to the faceted navigation node, which must not start with a / and is relative to the site content base path
     * @return the <code>HippoFacetNavigationBean</code> accounted for this <code>query</code> and <code>relPath</code> and <code>null</code> if we could not find the HippoFacetNavigationBean when the <code>query</code> is applied
     * @throws HstComponentException
     */
    public static HippoFacetNavigationBean getFacetNavigationBean(HstQuery query, String absBasePath, String relPath) throws HstComponentException {
        if(query == null) {
            return getFacetNavigationBean((String)null);
        }
        try {
            String queryAsString = "xpath("+query.getQueryAsString(true)+")";
            return getFacetNavigationBean(absBasePath, relPath, queryAsString);
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
    }

    /**
     * Tries to return a bean that is located in a faceted navigation tree below a result set. When it cannot be found,
     * or the bean is not of type <code>beanMappingClass</code>, <code>null</code> will be returned.
     *
     * @param <T>
     * @param query the free text search as String that is used for this faceted navigation. The query should already
     *              have been processed by {@link org.hippoecm.hst.util.SearchInputParsingUtils#parse(String, boolean)}
     *              if necessary.
     * @param absBasePath the absolute path (starting with /) from where to get the faceted navigation bean for <code>relPath</code> and
     *                     <code>query</code>. The <code>absBasePath</code> is NOT allowed to point to or to a descendant of a faceted navigation
     *                    node
     * @param relPath the relative path from <code>absBasePath</code> to the faceted navigation node, which must not start with a / and is relative to the site content base path
     * @param beanMappingClass the class T must be of
     * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
     */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(final String query, final String absBasePath, final String relPath,
                                                                              final Class<T> beanMappingClass)  {
        if(StringUtils.isBlank(relPath)|| StringUtils.isBlank(absBasePath)) {
            log.info("Cannot return a content bean for absolute base path or relative path empty. Return null");
            return null;
        }
        if (relPath.startsWith("/")) {
            log.info("relative path is not allowed to start with /. Return null");
            return null;
        }
        String absPath = absBasePath + "/" + relPath;
        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext == null) {
            throw new HstComponentException("Cannot call #getFacetNavigationBean without HstRequestContext");
        }
        if(StringUtils.isEmpty(query)) {
            try {
                HippoBean bean  = (HippoBean)requestContext.getObjectBeanManager().getObject(absPath);
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
            }
        }

        // we have free text search. Now, we have to fetch from the root every descendant one-by-one until we hit a FacetedNavigationNode.

        // first, let's get a disposable/query session:
        Session querySession = getDisposableSession(requestContext, query);
        ObjectBeanManager objectBeanMngr = requestContext.getObjectBeanManager(querySession);

        HippoFacetNavigationBean facetNavBean = null;

        // first, with the original session which is not tied to THIS free text faceted navigation, we need to get the
        // faceted navigation node. We CANNOT do this with the disposablePoolSession because then we TIE the faceted navigation
        // without free text search already to the disposablePoolSession
        try {
            Node baseNode = (Node)requestContext.getSession().getItem(absBasePath);
            Node stepInto = baseNode;
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
         * @param query the free text search as String that is used for this faceted navigation. The query should already
         *              have been processed by {@link org.hippoecm.hst.util.SearchInputParsingUtils#parse(String, boolean)}
         *              if necessary.
         * @param relPath the relative path from site base content to the faceted navigation node, which must not start with a / and is relative to the site content base path
         * @param beanMappingClass the class T must be of
         * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
         */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(final String query, final String relPath,
                                                                              final Class<T> beanMappingClass)  {
        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext == null) {
            throw new HstComponentException("Cannot call #getFacetNavigationBean without HstRequestContext");
        }
        return getFacetedNavigationResultDocument(query, "/"+requestContext.getSiteContentBasePath(), relPath, beanMappingClass);
    }

    /**
     * Tries to return a bean that is located in a faceted navigation tree below a result set. When it cannot be found,
     * or the bean is not of type <code>beanMappingClass</code>, <code>null</code> will be returned.
     *
     * @param <T>
     * @param query the free text search as String that is used for this faceted navigation
     * @param beanMappingClass the class T must be of
     * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
     */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(String query, Class<T> beanMappingClass)  {
        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext == null) {
            throw new HstComponentException("Cannot call #getFacetNavigationBean without HstRequestContext");
        }
        ResolvedSiteMapItem resolvedSiteMapItem = requestContext.getResolvedSiteMapItem();
        String relPath = PathUtils.normalizePath(resolvedSiteMapItem.getRelativeContentPath());

        return getFacetedNavigationResultDocument(query, relPath, beanMappingClass);
    }


    /**
     * Tries to return a bean that is located in a faceted navigation tree below a result set. When it cannot be found,
     * or the bean is not of type <code>beanMappingClass</code>, <code>null</code> will be returned.
     *
     * @param <T>
     * @param query the free text search as {@link HstQuery} that is used for this faceted navigation
     * @param absBasePath the absolute path (starting with /) from where to get the faceted navigation bean for <code>relPath</code> and
     *                     <code>query</code>. The <code>absBasePath</code> is NOT allowed to point to or to a descendant of a faceted navigation
     *                    node
     * @param relPath the relative path from <code>absBasePath</code> to the faceted navigation node, which must not start with a / and is relative to the site content base path
     * @param beanMappingClass the class T must be of
     * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
     */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(HstQuery query, String absBasePath, String relPath, Class<T> beanMappingClass) {
        if(query == null) {
            return getFacetedNavigationResultDocument((String)null, absBasePath,  relPath, beanMappingClass);
        }
        try {
            String queryAsString = "xpath("+query.getQueryAsString(true)+")";
            return getFacetedNavigationResultDocument(queryAsString, absBasePath, relPath, beanMappingClass);
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
    }

    /**
     * Tries to return a bean that is located in a faceted navigation tree below a result set. When it cannot be found,
     * or the bean is not of type <code>beanMappingClass</code>, <code>null</code> will be returned.
     *
     * @param <T>
     * @param query the free text search as {@link HstQuery} that is used for this faceted navigation
     * @param relPath the relative path from site base content to the faceted navigation node, which must not start with a / and is relative to the site content base path
     * @param beanMappingClass the class T must be of
     * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
     */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(HstQuery query, String relPath, Class<T> beanMappingClass) {
        if(query == null) {
            return getFacetedNavigationResultDocument((String)null, relPath, beanMappingClass);
        }
        try {
            String queryAsString = "xpath("+query.getQueryAsString(true)+")";
            return getFacetedNavigationResultDocument(queryAsString, relPath, beanMappingClass);
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
    }

    /**
     * Tries to return a bean that is located in a faceted navigation tree below a result set. When it cannot be found,
     * or the bean is not of type <code>beanMappingClass</code>, <code>null</code> will be returned.
     *
     * @param <T>
     * @param query the free text search as {@link HstQuery} that is used for this faceted navigation
     * @param beanMappingClass the class T must be of
     * @return The faceted navigation result document of type T and <code>null</code> if it cannot be found or is not of type <code>T</code>
     */
    public static <T extends  HippoBean> T getFacetedNavigationResultDocument(HstQuery query, Class<T> beanMappingClass)  {
        if(query == null) {
            return getFacetedNavigationResultDocument((String)null, beanMappingClass);
        }
        try {
            String queryAsString = "xpath("+query.getQueryAsString(true)+")";
            return getFacetedNavigationResultDocument(queryAsString, beanMappingClass);
        } catch (QueryException e) {
            throw new HstComponentException("Unable to create a string representation of query", e);
        }
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
     */
    public static Session getDisposableSession(HstRequestContext requestContext, String sessionIdentifier) throws HstComponentException {
        if (sessionIdentifier == null) {
            throw new HstComponentException("sessionIdentifier not allowed to be null");
        }
        try {
            String userID = null;
            Credentials cred = null;
            // if there exists subject based session, do use the credentials of that session to create session from disposable pool
            Session existingSession = requestContext.getSession(false);

            if (requestContext.isCmsRequest() && existingSession instanceof HippoSession) {
                // this is an non-proxied jcr session : for this, we do not instantiate disposable session pools
                // we need to get a fresh session to avoid reusing already built up virtual states
                return getPreviewCmsQuerySession(requestContext, sessionIdentifier);
            }

            if (existingSession instanceof LazySession) {
                Subject subject = HstSubject.getSubject(null);
                if (subject != null) {
                    Set<Credentials> repoCredsSet = subject.getPrivateCredentials(Credentials.class);
                    if (!repoCredsSet.isEmpty()) {
                        cred = repoCredsSet.iterator().next();
                        // this userID does not contain the mandatory credential domain separator needed for the lazy pools
                        // hence we append it with [separator]lazy, for example @lazy
                        userID = ((SimpleCredentials) cred).getUserID() + getCredentialsDomainSeparator() + "lazy";
                    }
                }
            }
            if (cred == null) {
                cred = requestContext.getContextCredentialsProvider().getDefaultCredentials(requestContext);
                userID = ((SimpleCredentials) cred).getUserID();
            }

            char[] passwd = ((SimpleCredentials) cred).getPassword();

            String disposablePoolSessionUserId = userID + ";" + sessionIdentifier + ";disposable";

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

    public static Session getPreviewCmsQuerySession(HstRequestContext requestContext, String sessionIdentifier) throws HstComponentException {
        try {
            SessionSecurityDelegation sessionSecurityDelegation = HstServices.getComponentManager().getComponent(SessionSecurityDelegation.class.getName());
            if (!sessionSecurityDelegation.sessionSecurityDelegationEnabled()) {
                log.debug("Security Delegation was expected to be enabled for cms request with non proxied session but it was not enabled. " +
                        "Return session from request context instead of new security delegated one");
                return requestContext.getSession(true);

            }
            HttpSession httpSession = requestContext.getServletRequest().getSession(false);
            if (httpSession == null) {
                throw new IllegalStateException("HttpSession should not be null for cms requests");
            }
            Credentials cmsUserCred = (Credentials) httpSession.getAttribute(ContainerConstants.CMS_SSO_REPO_CREDS_ATTR_NAME);
            if (cmsUserCred == null) {
                throw new IllegalStateException("HttpSession should contain cms user credentials attribute for cms requests");
            }
            // create a security delegated session that is automatically cleaned up at the end of the request
            return sessionSecurityDelegation.getOrCreatePreviewSecurityDelegate(cmsUserCred, sessionIdentifier);
        } catch (RepositoryException e) {
            throw new HstComponentException(e);
        }
    }

    private static String getCredentialsDomainSeparator() {
        return HstServices.getComponentManager().getContainerConfiguration().getString("repository.pool.user.name.separator");
    }


}
