/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.site.components.service.ctx;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.onehippo.cms7.essentials.site.beans.BaseDocument;
import org.onehippo.cms7.essentials.site.components.BaseComponent;

/**
 * @version "$Id: SearchContext.java 157405 2013-03-08 09:15:49Z mmilicevic $"
 */
public interface SearchContext {


    Class<? extends BaseDocument> getBeanMappingClass();

    void setBeanMappingClass(Class<? extends BaseDocument> clazz);

    Class<? extends HippoBean>[] getFilterBeans();

    boolean isIncludeSubtypeBeans();

    <T extends HippoBean> T getScope();

    <T extends HippoBean> void setScope(T baseScope);


    <T extends BaseComponent> T getComponent();

    HstRequest getRequest();

    boolean isAscending();

    <T extends HippoBean> T getFacetedScope();
}
