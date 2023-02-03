/*
 * Copyright 2019-2023 Bloomreach
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
import org.hippoecm.hst.core.request.HstRequestContext;

import javax.jcr.Node;

public class PageUpdateContextImpl extends AbstractPageContext implements PageUpdateContext {

    private transient final Node updatedSiteMapItemNode;
    private transient final Node updatedPageNode;

    public PageUpdateContextImpl(final HstRequestContext requestContext,
                                 final Mount editingMount,
                                 final Node updatedSiteMapItemNode,
                                 final Node updatedPageNode) {
        super(requestContext, editingMount);
        this.updatedSiteMapItemNode = updatedSiteMapItemNode;
        this.updatedPageNode = updatedPageNode;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Node getUpdatedSiteMapItemNode() {
        return updatedSiteMapItemNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node getUpdatedPageNode() {
        return updatedPageNode;
    }

    @Override
    public String toString() {
        return "PageUpdateContext{" +
                "requestContext=" + getRequestContext() +
                ", editingMount=" + getEditingMount() +
                ", updatedSiteMapItemNode=" + updatedSiteMapItemNode +
                ", updatedPageNode=" + updatedPageNode +
                '}';
    }
}
