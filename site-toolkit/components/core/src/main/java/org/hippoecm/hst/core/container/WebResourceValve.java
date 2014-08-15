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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webresources.Binary;
import org.onehippo.cms7.services.webresources.Content;
import org.onehippo.cms7.services.webresources.WebResource;
import org.onehippo.cms7.services.webresources.WebResourceException;
import org.onehippo.cms7.services.webresources.WebResourcesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebResourceValve extends AbstractBaseOrderableValve {

    private static final Logger log = LoggerFactory.getLogger(WebResourceValve.class);
    public static final int ONE_YEAR_MILLESECONDS = 1000 * 60 * 60 * 24 * 365;

    @Override
    public void invoke(final ValveContext context) throws ContainerException {
        final HstRequestContext requestContext = context.getRequestContext();
        final HttpServletRequest request = context.getServletRequest();
        final HttpServletResponse response = context.getServletResponse();

        WebResourcesService service = HippoServiceRegistry.getService(WebResourcesService.class);
        if (service == null) {
            log.error("Missing service for '{}'. Cannot serve webresource.", WebResourcesService.class.getName());
            throw new ContainerException("Missing service for '"+WebResourcesService.class.getName()+"'. Cannot serve webresource.",
                    new Throwable());
        }

        try {
            final Session session = requestContext.getSession();
            final WebResource webResource = service.getJcrWebResources(session).get("/" + requestContext.getResolvedSiteMapItem().getRelativeContentPath());

            final String version = request.getParameter("version");
            final Content content;
            if (StringUtils.isNotBlank(version)) {
                content = webResource.getContent(version);
                log.debug("Serving binary content for '{}' and version '{}'", webResource.getPath(), version);
            } else {
                content = webResource.getContent();
                // hash must be correct otherwise we do not serve trunk!
                if (content.getHash() != null) {
                    log.debug("Checking hash equality ");
                    final String hash = request.getParameter("hash");
                    if (StringUtils.isBlank(hash) || !hash.equals(content.getHash())) {
                        log.info("Web resource's content '{}' hash does not match request hash.", webResource.getPath());
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    log.debug("Serving binary content for '{}' and hash '{}'", webResource.getPath(), hash);
                }
            }

            setHeaders(response, content);
            final Binary binary = content.getBinary();
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


    public static void setHeaders(HttpServletResponse response, Content content) throws RepositoryException {
        // no need for ETag since expires 1 year
        response.setHeader("Content-Length", Long.toString(content.getBinary().getSize()));
        response.setContentType(content.getMimeType());
        // one year ahead max, see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.21
        final long expires = System.currentTimeMillis() + ONE_YEAR_MILLESECONDS;
        if (expires > 0) {
            response.setDateHeader("Expires", expires + System.currentTimeMillis());
            response.setHeader("Cache-Control", "max-age=" + (expires / 1000L));
        }
    }
}
