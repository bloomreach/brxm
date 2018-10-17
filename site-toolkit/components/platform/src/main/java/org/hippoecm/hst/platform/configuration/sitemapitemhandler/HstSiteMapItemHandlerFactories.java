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
package org.hippoecm.hst.platform.configuration.sitemapitemhandler;

public interface HstSiteMapItemHandlerFactories {

    /**
     * registers a HstSiteMapItemHandlerFactory for {@code contextPath}. Note this MIGHT happen concurrently for
     * multiple webapps, hence keep synchronization in mind
     * @param contextPath
     * @param hstSiteMapItemHandlerFactory
     */
    void register(String contextPath, HstSiteMapItemHandlerFactory hstSiteMapItemHandlerFactory);

    /**
     * removes a HstSiteMapItemHandlerFactory for {@code contextPath}. Note this MIGHT happen concurrently for
     * multiple webapps, hence keep synchronization in mind
     * @param contextPath
     */
    void unregister(String contextPath);

    /**
     * @param contextPath
     * @return the {@link HstSiteMapItemHandlerFactory} for {@code contextPath} and {@code null} in case non present
     */
    HstSiteMapItemHandlerFactory getHstSiteMapItemHandlerFactory(String contextPath);
}
