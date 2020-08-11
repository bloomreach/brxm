/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.HstRequestContext;

public class PageDeleteContextImpl extends AbstractPageContext implements PageDeleteContext {

    private transient HstSiteMapItem sourceSiteMapItem;
    private transient String sourceSiteMapPath;

    public PageDeleteContextImpl(final HstRequestContext requestContext,
                                 final Mount editingMount,
                                 final HstSiteMapItem sourceSiteMapItem,
                                 final String sourceSiteMapPath) {
        super(requestContext, editingMount);
        this.sourceSiteMapItem = sourceSiteMapItem;
        this.sourceSiteMapPath = sourceSiteMapPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HstSiteMapItem getSourceSiteMapItem() {
        return sourceSiteMapItem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSourceSiteMapPath() {
        return sourceSiteMapPath;
    }

    @Override
    public String toString() {
        return "PageDeleteContextImpl{" +
                "requestContext=" + getRequestContext() +
                ", editingMount=" + getEditingMount() +
                ", sourceSiteMapItem=" + sourceSiteMapItem +
                ", sourceSiteMapPath=" + sourceSiteMapPath +
                '}';
    }
}
