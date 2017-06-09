/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.site;

import org.onehippo.cms7.services.hst.Channel;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;

@FunctionalInterface
public interface HstSiteProvider {

    /**
     * This key is used to store the map on the http session of the webmaster in the channel mngr. The map will contain
     * which branch should be used for a mount. The mapping will be {@link Mount#getIdentifier()} to {@link Channel#getBranchId()}
     */
    String HST_SITE_PROVIDER_HTTP_SESSION_KEY = HstSiteProvider.class.getName() + ".key";

    HstSite getHstSite(CompositeHstSite compositeHstSite, HstRequestContext requestContext);

}
