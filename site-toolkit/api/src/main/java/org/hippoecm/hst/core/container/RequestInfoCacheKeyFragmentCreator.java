/**
 * Copyright 2013-2016 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.container;

import java.io.Serializable;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * <p>
 *     Responsible for creating the cachekey fragment that will be part of the {@link PageCacheKey}. Implementations should
 *     invoke {@link PageCacheKey#setAttribute(String, java.io.Serializable)} with value {@link #create(org.hippoecm.hst.core.request.HstRequestContext)}.
 * </p>
 * <p>
 *    If you have an application where you know that the requests for example always include some unimportant unique attribute
 *    as a request queryString parameter, you can choose to hook in your own {@link RequestInfoCacheKeyFragmentCreator} implementation
 *    that skips this queryString attribute. For example in general search engine crawlers have unique queryString parameters which
 *    are not used by the application, but merely by the crawlers to make sure they fall through caching proxies like mod_cache, squid, varnish
 *    or some CND etc in front of the application.
 * </p>
 */
public interface RequestInfoCacheKeyFragmentCreator {

    /**
     * Creates a serializable request info object that represents the parts of the request that makes the request unique
     * (and thus ensures unique requests have unique cachekeys)
     */
    Serializable create(HstRequestContext requestContext);

    void reset();

}
