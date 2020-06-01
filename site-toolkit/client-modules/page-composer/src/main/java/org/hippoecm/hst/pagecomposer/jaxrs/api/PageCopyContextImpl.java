/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.api;

import javax.jcr.Node;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.HstRequestContext;

public class PageCopyContextImpl implements PageCopyContext {

    private transient HstRequestContext requestContext;
    private transient Mount editingMount;
    private transient HstSiteMapItem sourceSiteMapItem;
    private transient Node sourceSiteMapNode;
    private transient HstComponentConfiguration sourcePage;
    private transient Node sourcePageNode;

    private transient Mount targetMount;
    /**
     * The parent {@link HstSiteMapItem} of the to be created siteMapItem node. If {@code null}, it means the
     * newly created siteMapItem is a siteMapItem that is a root  {@link HstSiteMapItem}, aka, a {@link HstSiteMapItem} that
     * does not have a parent  {@link HstSiteMapItem}.
     */
    private transient HstSiteMapItem targetSiteMapItem;

    private transient Node newSiteMapItemNode;
    private transient Node newPageNode;

    public PageCopyContextImpl(final HstRequestContext requestContext,
                           final Mount editingMount,
                           final HstSiteMapItem sourceSiteMapItem,
                           final Node sourceSiteMapNode,
                           final HstComponentConfiguration sourcePage,
                           final Node sourcePageNode,
                           final Mount targetMount,
                           final HstSiteMapItem targetSiteMapItem,
                           final Node newSiteMapItemNode,
                           final Node newPageNode) {
        this.requestContext = requestContext;
        this.editingMount = editingMount;
        this.sourceSiteMapItem = sourceSiteMapItem;
        this.sourceSiteMapNode = sourceSiteMapNode;
        this.sourcePage = sourcePage;
        this.sourcePageNode = sourcePageNode;
        this.targetMount = targetMount;
        this.targetSiteMapItem = targetSiteMapItem;
        this.newSiteMapItemNode = newSiteMapItemNode;
        this.newPageNode = newPageNode;
    }

    /**
     * @return the {@link HstRequestContext} that originated this {@link PageCopyContextImpl}. It will never be {@code null}
     */
    public HstRequestContext getRequestContext() {
        return requestContext;
    }

    /**
     * @return the {@link Mount} that belongs to the channel from which the copy action originated.
     * This method never returns {@code null}.
     */
    public Mount getEditingMount() {
        return editingMount;
    }

    /**
     * @return the {@link HstSiteMapItem} belonging to the page that is to be copied. This {@link HstSiteMapItem} instance always
     * belongs to the {@link #getEditingMount()}. This method never returns {@code null}.
     */
    public HstSiteMapItem getSourceSiteMapItem() {
        return sourceSiteMapItem;
    }

    /**
     * @return the JCR {@link javax.jcr.Node} belonging {@link #getSourceSiteMapItem()}. This method never returns {@code null}.
     */
    public Node getSourceSiteMapNode() {
        return sourceSiteMapNode;
    }

    /**
     * @return the {@link HstComponentConfiguration} belonging {@link #getSourceSiteMapItem()}. This method never returns {@code null}.
     */
    public HstComponentConfiguration getSourcePage() {
        return sourcePage;
    }

    /**
     * @return the JCR {@link javax.jcr.Node} belonging {@link #getSourcePage()}. This method never returns {@code null}.
     */
    public Node getSourcePageNode() {
        return sourcePageNode;
    }

    /**
     * @return the {@link Mount} that to which the page copy action is targeted. If the page copy is within the same
     * channel, this method returns the exact same instance as {@link #getEditingMount()}
     * This method never returns {@code null}.
     */
    public Mount getTargetMount() {
        return targetMount;
    }

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
    public HstSiteMapItem getTargetSiteMapItem() {
        return targetSiteMapItem;
    }

    /**
     * @return the in {@link HstRequestContext#getSession() session} created (but not yet persisted) site map item JCR {@link javax.jcr.Node}
     * as a result of this copy page action
     */
    public Node getNewSiteMapItemNode() {
        return newSiteMapItemNode;
    }

    /**
     * @return the in {@link HstRequestContext#getSession() session} created (but not yet persisted) page JCR {@link javax.jcr.Node}
     * as a result of this copy page action
     */
    public Node getNewPageNode() {
        return newPageNode;
    }

    @Override
    public String toString() {
        return "PageCopyContext{" +
                "requestContext=" + requestContext +
                ", editingMount=" + editingMount +
                ", sourceSiteMapItem=" + sourceSiteMapItem +
                ", sourceSiteMapNode=" + sourceSiteMapNode +
                ", sourcePage=" + sourcePage +
                ", sourcePageNode=" + sourcePageNode +
                ", targetMount=" + targetMount +
                ", targetSiteMapItem=" + targetSiteMapItem +
                ", newSiteMapItemNode=" + newSiteMapItemNode +
                ", newPageNode=" + newPageNode +
                '}';
    }
}
