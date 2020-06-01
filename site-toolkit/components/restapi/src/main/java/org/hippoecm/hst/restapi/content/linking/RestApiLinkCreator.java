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

package org.hippoecm.hst.restapi.content.linking;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.restapi.ResourceContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

public class RestApiLinkCreator {

    private static final Logger log = LoggerFactory.getLogger(RestApiLinkCreator.class);

    /**
     *
     * @param context the ResourceContext
     * @param uuid the UUID of the node for which the {@code hstLink} was created. When the {@code hstLink} is a
     *             container resource ({@link org.hippoecm.hst.core.linking.HstLink#isContainerResource()}, the {@code uuid}
     *             is allowed to be {@code null}
     * @param hstLink the <code>hstLink</code> for {@code uuid}. If {@code null}, a {@link  Link#invalid} is returned
     * @return
     */
    public Link convert(final ResourceContext context, final String uuid, final HstLink hstLink)  {
        final HstRequestContext requestContext = context.getRequestContext();
        final Mount apiMount = requestContext.getResolvedMount().getMount();

        if (hstLink == null || hstLink.isNotFound() || hstLink.getPath() == null) {
            return Link.invalid;
        }

        if (hstLink.isContainerResource()) {
            return Link.binary(hstLink.toUrlForm(requestContext, true));
        }

        final Node node;
        try {

            node  = requestContext.getSession().getNodeByIdentifier(uuid);
            if (hstLink.getMount() != apiMount) {
                // can only be a web link since non mapped mount are never tested for cross channel links
                return Link.site(hstLink.toUrlForm(requestContext, true));
            }
            // TODO make it pluggable that different types can use a different created link, for example /folders for folders instead of
            // TODO /documents
            if (!node.isNodeType(NT_HANDLE)) {
                log.info("Invalid nodetype '{}' for an api link.", node.getPrimaryNodeType().getName());
                return Link.invalid;
            }

            final String apiURL = getApiURL(requestContext, apiMount);
            return Link.local(node.getIdentifier(), apiURL + "/documents/" + node.getIdentifier());
        } catch (ItemNotFoundException e) {
            log.info("Cannot find node for id '{}'", uuid);
            return Link.invalid;
        } catch (RepositoryException e) {
            log.warn("Repository exception during link creation. Return link of type invaled : {}", e.toString());
            return Link.invalid;
        }

    }

    private String getApiURL(final HstRequestContext requestContext, final Mount apiMount) {
        final String scheme = apiMount.getScheme();
        final String hostName = apiMount.getVirtualHost().getHostName();
        final int port = HstRequestUtils.getRequestServerPort(requestContext.getServletRequest());
        final String portNumberPath;
        if (port == 80 || port == 443) {
            portNumberPath = "";
        } else {
            portNumberPath = ":" + port;
        }

        final String contextPath;
        if (apiMount.isContextPathInUrl()) {
            contextPath = apiMount.getContextPath();
        } else {
            contextPath = "";
        }
        final String mountPath = apiMount.getMountPath();
        return scheme + "://" + hostName + portNumberPath + contextPath + mountPath;
    }

}
