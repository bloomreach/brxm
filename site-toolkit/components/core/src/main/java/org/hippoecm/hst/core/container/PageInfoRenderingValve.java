/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
import java.io.Serializable;
import java.net.SocketException;
import java.util.Collection;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.cache.ForwardPlaceHolderHstPageInfo;
import org.hippoecm.hst.cache.HstPageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.constructs.web.Header;

/**
 * PageInfoRenderingValve
 * <P>
 * Simply invoke next valve.
 * If pageInfo object is found from the request attribute, then render the pageInfo.
 * </P>
 */
public class PageInfoRenderingValve extends AbstractBaseOrderableValve {

    static final String PAGE_INFO = PageInfoRenderingValve.class.getName() + ".pageInfo";
    private static final Logger log = LoggerFactory.getLogger(PageInfoRenderingValve.class);

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest request = context.getServletRequest();

        context.invokeNext();

        HstPageInfo pageInfo = (HstPageInfo) request.getAttribute(PAGE_INFO);

        if (pageInfo == null) {
            return;
        }

        HttpServletResponse response = context.getServletResponse();

        try {
            if (pageInfo instanceof ForwardPlaceHolderHstPageInfo) {
                log.debug("Page '{}' is being forwarded internally.", request.getRequestURI());
                // we need to set the forwarded info again on the request as it might now be not yet set because 
                // ForwardPlaceHolderPageInfo might come from the cache
                String forwardPathInfo = ((ForwardPlaceHolderHstPageInfo) pageInfo).getForwardPathInfo();
                request.setAttribute(ContainerConstants.HST_FORWARD_PATH_INFO, forwardPathInfo);
                return;
            }

            if (!response.isCommitted()) {
                // in case response.sendRedirect or something similar has been invoked, the response is already
                // committed and we do not need to write the response then
                writeResponse(response, pageInfo);
            }
        } catch (SocketException e) {
            if (log.isDebugEnabled()) {
                log.info("Unable to write response due to SocketException", e);
            } else {
                log.info("Unable to write response due to SocketException : {}", e.toString());
            }
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.warn("Unable to write response", e);
            } else {
                log.warn("Unable to write response : {}", e.toString());
            }
        }
    }

    /**
     * Writes the response from a PageInfo object.
     * <p/>
     * Headers are set last so that there is an opportunity to override
     *
     * @throws net.sf.ehcache.constructs.web.ResponseHeadersNotModifiableException
     *
     */
    protected void writeResponse(final HttpServletResponse response, final HstPageInfo pageInfo) throws IOException {
        setStatus(response, pageInfo);
        setContentType(response, pageInfo);
        setHeaders(pageInfo, response);
        pageInfo.writeContent(response);
    }

    /**
     * Status code
     *
     * @param response
     * @param pageInfo
     */
    protected void setStatus(final HttpServletResponse response, final HstPageInfo pageInfo) {
        response.setStatus(pageInfo.getStatusCode());
    }

    /**
     * Set the content type.
     *
     * @param response
     * @param pageInfo
     */
    protected void setContentType(final HttpServletResponse response, final HstPageInfo pageInfo) {
        String contentType = pageInfo.getContentType();

        if (contentType != null && contentType.length() > 0) {
            response.setContentType(contentType);
        }
    }

    /**
     * Set the headers in the response object
     *
     * @param pageInfo
     * @param response
     */
    protected void setHeaders(final HstPageInfo pageInfo,
                              final HttpServletResponse response) {

        final Collection<Header<? extends Serializable>> headers = pageInfo.getHeaders();

        // Track which headers have been set so all headers of the same name
        // after the first are added
        final TreeSet<String> setHeaders = new TreeSet<String>(
                String.CASE_INSENSITIVE_ORDER);

        for (final Header<? extends Serializable> header : headers) {
            final String name = header.getName();

            switch (header.getType()) {
                case STRING:
                    if (setHeaders.contains(name)) {
                        response.addHeader(name, (String) header.getValue());
                    } else {
                        setHeaders.add(name);
                        response.setHeader(name, (String) header.getValue());
                    }
                    break;
                case DATE:
                    if (setHeaders.contains(name)) {
                        response.addDateHeader(name, (Long) header.getValue());
                    } else {
                        setHeaders.add(name);
                        response.setDateHeader(name, (Long) header.getValue());
                    }
                    break;
                case INT:
                    if (setHeaders.contains(name)) {
                        response.addIntHeader(name, (Integer) header.getValue());
                    } else {
                        setHeaders.add(name);
                        response.setIntHeader(name, (Integer) header.getValue());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("No mapping for Header: "
                            + header);
            }
        }
    }

}
