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
package org.hippoecm.hst.core.container;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webresources.Binary;
import org.onehippo.cms7.services.webresources.WebResource;
import org.onehippo.cms7.services.webresources.WebResourceBundle;
import org.onehippo.cms7.services.webresources.WebResourceException;
import org.onehippo.cms7.services.webresources.WebResourcesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebResourceValve extends AbstractBaseOrderableValve {

    private static final Logger log = LoggerFactory.getLogger(WebResourceValve.class);
    private static final long ONE_YEAR_SECONDS = TimeUnit.SECONDS.convert(365L, TimeUnit.DAYS);
    private static final long ONE_YEAR_MILLISECONDS = TimeUnit.MILLISECONDS.convert(ONE_YEAR_SECONDS, TimeUnit.SECONDS);


    @Override
    public void invoke(final ValveContext context) throws ContainerException {
        final HstRequestContext requestContext = context.getRequestContext();
        final HttpServletRequest request = context.getServletRequest();
        final HttpServletResponse response = context.getServletResponse();

        WebResourcesService service = HippoServiceRegistry.getService(WebResourcesService.class);
        if (service == null) {
            log.error("Missing service for '{}'. Cannot serve webresource.", WebResourcesService.class.getName());
            throw new ContainerException("Missing service for '" + WebResourcesService.class.getName() + "'. Cannot serve webresource.");
        }

        try {
            final Session session = requestContext.getSession();
            final ResolvedMount resolvedMount = requestContext.getResolvedMount();
            final ChannelInfo channelInfo = resolvedMount.getMount().getChannelInfo();

            final Channel channel = resolvedMount.getMount().getChannel();
            if (channel == null) {
                String msg = String.format("Cannot serve web resource for mount '%s' because it does not have a " +
                        "channel configuration.", resolvedMount.getMount());
                throw new WebResourceException(msg);
            }

            final String bundleName;
            if (StringUtils.isNotBlank(channelInfo.getWebResourceBundleName())) {
                bundleName = channelInfo.getWebResourceBundleName();
            } else {
                bundleName = channel.getId();
            }


            final WebResourceBundle webResourceBundle = service.getJcrWebResourceBundle(session, bundleName);

            String contentPath = "/" + requestContext.getResolvedSiteMapItem().getRelativeContentPath();
            String version = requestContext.getResolvedSiteMapItem().getParameter("version");
            if (version == null) {
                String msg = String.format("Cannot serve web resource '%s' for mount '%s' because sitemap item" +
                        "'%s' does not contain version param.",  contentPath,
                        resolvedMount.getMount(), requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getQualifiedId());
                throw new WebResourceException(msg);
            }

            final WebResource webResource;
            if (version.equals(webResourceBundle.getAntiCacheValue())) {
                webResource = webResourceBundle.get(contentPath);
            } else  {
                webResource = webResourceBundle.get(contentPath, version);
            }

            setHeaders(response, webResource);
            final Binary binary = webResource.getBinary();
            final ServletOutputStream outputStream = response.getOutputStream();
            IOUtils.copy(binary.getStream(), outputStream);
            outputStream.flush();

        } catch (RepositoryException e) {
            throw new ContainerException(e);
        } catch (IOException e) {
            throw new ContainerException(e);
        } catch (WebResourceException e) {
            if (log.isDebugEnabled()) {
                log.info("Cannot serve binary '{}'", request.getPathInfo(), e);
            } else {
                log.info("Cannot serve binary '{}' : cause '{}'", request.getPathInfo(), e.toString());
            }
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        context.invokeNext();
    }


    public static void setHeaders(final HttpServletResponse response, final WebResource webResource) throws RepositoryException {
        // no need for ETag since expires 1 year
        response.setHeader("Content-Length", Long.toString(webResource.getBinary().getSize()));
        response.setContentType(webResource.getMimeType());
        // one year ahead max, see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.21
        response.setDateHeader("Expires", ONE_YEAR_MILLISECONDS + System.currentTimeMillis());
        response.setHeader("Cache-Control", "max-age=" + ONE_YEAR_SECONDS);
    }
}
