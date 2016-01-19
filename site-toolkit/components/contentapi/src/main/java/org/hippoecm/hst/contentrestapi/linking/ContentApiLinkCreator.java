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

package org.hippoecm.hst.contentrestapi.linking;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.contentrestapi.ResourceContext;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;

import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

public class ContentApiLinkCreator {

    public Link create(final ResourceContext context, final String uuid) throws RepositoryException {
        final HstRequestContext requestContext = context.getRequestContext();
        final Mount apiMount = requestContext.getResolvedMount().getMount();
        final Node node = requestContext.getSession().getNodeByIdentifier(uuid);
        // TODO make it pluggable that different types can use a different created link, for example /folders for folders instead of
        // TODO /documents
        if (!node.isNodeType(NT_HANDLE)) {
            throw new IllegalArgumentException("Only links to documents are supported at this moment");
        }
        final String apiURL = getApiURL(requestContext, apiMount);
        return new Link(node.getIdentifier(), apiURL + "/documents/" + node.getIdentifier());
    }

    public Link convert(final ResourceContext context, final HstLink hstLink) throws RepositoryException, LinkConversionException {
        final HstRequestContext requestContext = context.getRequestContext();
        final Mount apiMount = requestContext.getResolvedMount().getMount();
        if (hstLink.getMount() != apiMount) {
            throw new IllegalArgumentException("Can only convert an HstLink to a content api Link if it belongs to the api mount");
        }
        final String mountContentPath = hstLink.getMount().getContentPath();
        final Node node;
        try {
            node  = requestContext.getSession().getNode(mountContentPath + "/" + hstLink.getPath());
        } catch (PathNotFoundException e) {
            throw new LinkConversionException(String.format("Cannot find node at '%s'", mountContentPath + "/" + hstLink.getPath()));
        }
        // TODO make it pluggable that different types can use a different created link, for example /folders for folders instead of
        // TODO /documents
        if (!node.isNodeType(NT_HANDLE)) {
            throw new LinkConversionException("Only links to documents are supported at this moment");
        }

        final String apiURL = getApiURL(requestContext, apiMount);
        return new Link(node.getIdentifier(), apiURL + "/documents/" + node.getIdentifier());
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

    public class LinkConversionException extends RuntimeException {

        public LinkConversionException(final String msg) {
            super(msg);
        }
    }
}
