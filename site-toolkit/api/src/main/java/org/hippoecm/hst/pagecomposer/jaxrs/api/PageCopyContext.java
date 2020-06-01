/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.api;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * Page copying context data used in {@link PageCopyEvent}.
 */
public interface PageCopyContext {

    /**
     * @return the {@link HstRequestContext} that originated this {@link PageCopyContext}. It will never be {@code null}
     */
    public HstRequestContext getRequestContext();

    /**
     * @return the {@link Mount} that belongs to the channel from which the copy action originated.
     * This method never returns {@code null}.
     */
    public Mount getEditingMount();

    /**
     * @return the {@link HstSiteMapItem} belonging to the page that is to be copied. This {@link HstSiteMapItem} instance always
     * belongs to the {@link #getEditingMount()}. This method never returns {@code null}.
     */
    public HstSiteMapItem getSourceSiteMapItem();

    /**
     * @return the JCR {@link javax.jcr.Node} belonging {@link #getSourceSiteMapItem()}. This method never returns {@code null}.
     */
    public Node getSourceSiteMapNode();

    /**
     * @return the {@link HstComponentConfiguration} belonging {@link #getSourceSiteMapItem()}. This method never returns {@code null}.
     */
    public HstComponentConfiguration getSourcePage();

    /**
     * @return the JCR {@link javax.jcr.Node} belonging {@link #getSourcePage()}. This method never returns {@code null}.
     */
    public Node getSourcePageNode();

    /**
     * @return the {@link Mount} that to which the page copy action is targeted. If the page copy is within the same
     * channel, this method returns the exact same instance as {@link #getEditingMount()}
     * This method never returns {@code null}.
     */
    public Mount getTargetMount();

    /**
     * <p>
     *     returns the target {@link HstSiteMapItem} to which the page copy action is targeted, or {@code null} in case
     *     the target is a {@code root siteMapItem}
     * </p>
     * <p>
     *     If it is {@code null}, it means the page copy will create a {@code root siteMapItem}.
     *     A {@code root siteMapItem} is a {@link HstSiteMapItem} that returns {@code null} for {@link HstSiteMapItem#getParentItem()}.
     * </p>
     * <p>
     *     If it returns a non {@code null} value, the returned {@link HstSiteMapItem} belongs to {@link #getTargetMount()}
     * </p>
     * @return the target {@link HstSiteMapItem} to which the page copy action is targeted, or {@code null}
     *
     */
    public HstSiteMapItem getTargetSiteMapItem();

    /**
     * @return the in {@link HstRequestContext#getSession() session} created (but not yet persisted) site map item JCR {@link javax.jcr.Node}
     * as a result of this copy page action
     */
    public Node getNewSiteMapItemNode();

    /**
     * @return the in {@link HstRequestContext#getSession() session} created (but not yet persisted) page JCR {@link javax.jcr.Node}
     * as a result of this copy page action
     */
    public Node getNewPageNode();
}
