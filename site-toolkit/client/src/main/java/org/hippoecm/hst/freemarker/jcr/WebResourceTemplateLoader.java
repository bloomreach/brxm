/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.freemarker.jcr;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.hst.util.WebResourceUtils;
import org.onehippo.cms7.services.webresources.WebResourcesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebResourceTemplateLoader extends AbstractTemplateLoader {

    private static final Logger log = LoggerFactory.getLogger(WebResourceTemplateLoader.class);

    public Object findTemplateSource(String templateSource) throws IOException {
        if (templateSource == null || !templateSource.startsWith(ContainerConstants.FREEMARKER_WEBRESOURCE_TEMPLATE_PROTOCOL)) {
            return null;
        }

        String webResourcePath = "/" + PathUtils.normalizePath(templateSource.substring(
                ContainerConstants.FREEMARKER_WEBRESOURCE_TEMPLATE_PROTOCOL.length()));
        final HstRequestContext ctx = RequestContextProvider.get();
        if (ctx == null) {
            String msg = String.format("Cannot serve freemarker template from webresource '%s' when there is no " +
                    "HstRequestContext.", templateSource);
            throw new IllegalStateException(msg);
        }

        final String bundleName = WebResourceUtils.getBundleName(ctx);
        String absPath = WebResourcesService.JCR_ROOT_PATH + "/" + bundleName + webResourcePath;
        log.info("Trying to load freemarker template for webresource from '{}'", absPath);
        return getLoadingCache().get(absPath);
    }
}
