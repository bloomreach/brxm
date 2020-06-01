/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.util;

import org.hippoecm.frontend.session.PluginUserSession;
import org.onehippo.cms7.services.cmscontext.CmsInternalCmsContextService;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

/**
 * Utility methods for dealing with a CmsSessionContext.
 */
public class CmsSessionUtil {

    public static void populateCmsSessionContext(final CmsInternalCmsContextService service,
                                                 final CmsSessionContext context,
                                                 final PluginUserSession session) {
        service.setData(context, CmsSessionContext.REPOSITORY_CREDENTIALS, session.getCredentials());
        service.setData(context, CmsSessionContext.LOCALE, session.getLocale());
    }
}
