/*
 * Copyright 2008-2022 Bloomreach
 */
package org.hippoecm.hst.content.beans.query;

import javax.jcr.Node;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.util.DateTools;

public interface HstQueryManager {

    /**
     * @return the {@link Session} which was used to create this {@code HstQueryManager} with
     */
    Session getSession();


    /**
     * @return the default {@code {@link DateTools.Resolution}} for this {@code HstQueryManager} instance
     */
    DateTools.Resolution getDefaultResolution();

    /**
     * @return the {@link ObjectConverter} that was used to create this  {@code HstQueryManager} instance
     */
    ObjectConverter getObjectConverter();

    /**
     * Creates a empty query, with scope
     * @param scope 
     * @return a new <code>{@link HstQuery}</code> with scope {@code scope}
     */
    HstQuery createQuery(Node scope) throws QueryException;
    
    /**
     * Creates a query, with scope  and Filter for types of filterBean. If includeSubTypes is <code>true</code>,
     * the result may also contain HippoBean's whose primarytype is a subtype of the filterBean type. 
     * 
     * @param scope
     * @param filterBean
     * @param includeSubTypes
     * @return a new <code>{@link HstQuery}</code> with scope {@literal &} filter
     * @throws QueryException
     */
    HstQuery createQuery(Node scope, Class<? extends HippoBean> filterBean, boolean includeSubTypes) throws QueryException;
    
    /**
     * Creates a query, with scope and Filter for node types. If includeSubTypes is <code>true</code>,
     * the result may also contain node types whose primary type is a subtype of the filter node type. 
     * 
     * @param scope
     * @param nodeType
     * @param includeSubTypes
     * @return a new <code>{@link HstQuery}</code> with scope {@literal &} filter
     * @throws QueryException
     */
    HstQuery createQuery(Node scope, String nodeType, boolean includeSubTypes) throws QueryException;
    
    /**
     * Creates a empty query, with scope
     * @param scope 
     * @return a new <code>{@link HstQuery}</code> with scope {@code scope}
     */ 
    HstQuery createQuery(HippoBean scope) throws QueryException;
    
    /**
     * Creates a query, with scope HippoBean and Filter for types of filterBean. If includeSubTypes is <code>true</code>,
     * the result may also contain HippoBean's whose primarytype is a subtype of the filterBean type. 
     * 
     * @param scope
     * @param filterBean
     * @param includeSubTypes
     * @return a new <code>{@link HstQuery}</code> with scope {@literal &} filter
     * @throws QueryException
     */
    HstQuery createQuery(HippoBean scope, Class<? extends HippoBean> filterBean, boolean includeSubTypes) throws QueryException;
    
    /**
     * Creates a query, with the scope HippoBean and with a Filter that filters to only return HippoBeans of the types that are 
     * added as variable arguments.
     * 
     * @param scope
     * @param filterBeans
     * @return a new <code>{@link HstQuery}</code> with scope {@literal &} filter on jcr primary nodetype of the filterBean
     * @throws QueryException
     */
    HstQuery createQuery(Node scope, Class<? extends HippoBean>... filterBeans) throws QueryException;

    /**
     * @see #createQuery(Node, Class[]) createQuery(Node scope, Class<? extends HippoBean>... filterBeans) only now also subtypes of
     * <code>filterBeans</code> are included <b>if</b> <code>includeSubTypes = true</code>
     */
    HstQuery createQuery(Node scope, boolean includeSubTypes, Class<? extends HippoBean>... filterBeans) throws QueryException;

    /**
     * Creates a query, with the scope HippoBean and with a Filter that filters to only return HippoBeans of the types that are 
     * added as variable arguments.
     * 
     * @param scope
     * @param primaryNodeTypes
     * @return a new <code>{@link HstQuery}</code> with scope {@literal &} filter on jcr primary nodetype of the filterBean
     * @throws QueryException
     */
    HstQuery createQuery(Node scope, String ... primaryNodeTypes) throws QueryException;

    /**
     * @see #createQuery(Node, String[]) createQuery(Node scope, String ... primaryNodeTypes) only now also subtypes of
     * <code>filterBeans</code> are included <b>if</b> <code>includeSubTypes = true</code>
     */
    HstQuery createQuery(Node scope, boolean includeSubTypes, String ... primaryNodeTypes) throws QueryException;
    
    /**
     * Creates a query, with a scope and with a Filter that filters to only return HippoBeans of the types that are 
     * added as variable arguments.
     * 
     * @param scope
     * @param filterBean
     * @return a new <code>{@link HstQuery}</code> with scope and filter on jcr primary nodetype of the filterBean
     * @throws QueryException
     */
    HstQuery createQuery(HippoBean scope, Class<? extends HippoBean>... filterBean) throws QueryException;

    /**
     * @see #createQuery(HippoBean, Class[]) createQuery(HippoBean scope, Class<? extends HippoBean>... filterBean) only now also subtypes of
     * <code>filterBeans</code> are included <b>if</b> <code>includeSubTypes = true</code>
     */
    HstQuery createQuery(HippoBean scope, boolean includeSubTypes, Class<? extends HippoBean>... filterBean) throws QueryException;
    
    /**
     * Creates a query, with a scope and with a Filter that filters to only return HippoBeans of the types that are 
     * added as variable arguments.
     * 
     * @param scope
     * @param primaryNodeTypes
     * @return a new <code>{@link HstQuery}</code> with scope and filter on jcr primary nodetype of the filterBean
     * @throws QueryException
     */
    HstQuery createQuery(HippoBean scope, String ... primaryNodeTypes) throws QueryException;

    /**
     * @see #createQuery(HippoBean, String[]) createQuery(HippoBean scope, String ... primaryNodeTypes) only now also subtypes of
     * <code>filterBeans</code> are included <b>if</b> <code>includeSubTypes = true</code>
     */
    HstQuery createQuery(HippoBean scope, boolean includeSubTypes , String ... primaryNodeTypes) throws QueryException;
    
}
