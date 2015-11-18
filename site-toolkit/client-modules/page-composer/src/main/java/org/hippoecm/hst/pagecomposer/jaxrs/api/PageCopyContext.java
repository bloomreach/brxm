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

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageCopyContext {

    private static final Logger log = LoggerFactory.getLogger(PageCopyContext.class);

    private transient HstRequestContext requestContext;
    private transient Mount editingMount;
    private transient HstSite editingPreviewSite;
    private transient Mount targetMount;
    private transient HstSite targetMountPreviewSite;
    private transient HstSiteMapItem fromHstSiteMapItem;
    private transient Node fromSiteMapNode;
    private transient Node newSiteMapNode;
    private transient Node fromPage;
    private transient Node newPage;

    public PageCopyContext(final HstRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public HstRequestContext getRequestContext() {
        return requestContext;
    }

    public void setRequestContext(final HstRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public Node getNewSiteMapNode() {
        return newSiteMapNode;
    }

    public void setNewSiteMapNode(final Node newSiteMapNode) {
        this.newSiteMapNode = newSiteMapNode;
    }
}
