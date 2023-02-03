/*
 *  Copyright 2019-2023 Bloomreach
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

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

/**
 * Page delete context data used in {@link PageDeleteEvent}.
 */
public interface PageDeleteContext extends PageActionContext {

    /**
     * @return the {@link HstSiteMapItem} belonging to the page that is to be deleted. This {@link HstSiteMapItem} instance always
     * belongs to the {@link #getEditingMount()}. This method never returns {@code null}.
     */
    public HstSiteMapItem getSourceSiteMapItem();

    /**
     * @return the path of the site map item that belongs to the page that is to be deleted.
     */
    public String getSourceSiteMapPath();

}
