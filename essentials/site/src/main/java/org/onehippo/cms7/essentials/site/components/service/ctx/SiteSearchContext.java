/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.site.components.service.ctx;

import java.util.List;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.onehippo.cms7.essentials.site.beans.BaseDocument;
import org.onehippo.cms7.essentials.site.components.BaseComponent;

import com.google.common.collect.ImmutableList;

/**
 * @version "$Id: SiteSearchContext.java 157405 2013-03-08 09:15:49Z mmilicevic $"
 */
public class SiteSearchContext implements SearchContext {


    private static List<Class<? extends HippoBean>> scope = ImmutableList.<Class<? extends HippoBean>>of(BaseDocument.class);
    private HstRequest request;
    private BaseComponent component;
    private Class<? extends BaseDocument> beanMappingClass;
    private HippoBean scopeBean;


    public SiteSearchContext(HstRequest request, BaseComponent component) {
        this.request = request;
        this.component = component;
    }

    public SiteSearchContext(HstRequest request, BaseComponent component, Class<? extends BaseDocument> getBeanMappingClass) {
        this.request = request;
        this.component = component;
        this.beanMappingClass = getBeanMappingClass;
    }

    @Override
    public Class<? extends BaseDocument> getBeanMappingClass() {
        return beanMappingClass;
    }

    @Override
    public void setBeanMappingClass(final Class<? extends BaseDocument> clazz) {
        this.beanMappingClass = clazz;
    }

    @SuppressWarnings({"SuspiciousToArrayCall", "unchecked"})
    @Override
    public Class<? extends HippoBean>[] getFilterBeans() {
        return (Class<? extends HippoBean>[]) scope.toArray(new Class<?>[scope.size()]);
    }

    @Override
    public boolean isIncludeSubtypeBeans() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends HippoBean> T getScope() {
        if (scopeBean != null) {
            return (T) scopeBean;
        }
        return (T) component.getSiteContentBaseBean(request);
    }

    @Override
    public <T extends HippoBean> void setScope(T baseScope) {
        scopeBean = baseScope;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BaseComponent> T getComponent() {
        return (T) component;
    }

    @Override
    public HstRequest getRequest() {
        return request;
    }

    @Override
    public boolean isAscending() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends HippoBean> T getFacetedScope() {
        return (T) component.getContentBean(request);
    }
}

