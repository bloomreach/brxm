/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.service.documenturl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Returns the URL to a document node on a site of a certain type (preview, live, etc.).
 * The following configuration properties are available:
 * <ul>
 * <li>string 'protocol' for the URL protocol (default: 'http')</li>
 * <lu>string 'domain' for the URL host and domain (default 'localhost')</li>
 * <li>long 'port' for the URL port number (default: 80)</li>
 * <li>string 'path' for the URL path (default: '/site/previewfromcms')</li>
 * <li>string 'type' for the type of the site (default: 'preview').
 * </ul>
 * The created URL gets two additional parameters: 'uuid' with the UUID of the document, and 'type' with the
 * configured type.
 *
 * @deprecated From CMS 7.7, this service has been replaced by the document URL service in the channel manager.
 */
@Deprecated
public class SiteDocumentUrlService extends Plugin implements IDocumentUrlService {

    private static final Logger log = LoggerFactory.getLogger(SiteDocumentUrlService.class);

    private String protocol = "http";
    private String domain = "localhost";
    private int port = 80;
    private String path = "/site/previewfromcms";
    private String type = "preview";

    public SiteDocumentUrlService(IPluginContext context, IPluginConfig config) {
        super(context, config);

        protocol = config.getString("protocol", protocol);
        domain = config.getString("domain", domain);
        port = config.getAsInteger("port", port);
        path = config.getString("path", path);
        type = config.getString("type", type);

        final String serviceId = config.getString("service.id", IDocumentUrlService.DEFAULT_SERVICE_ID);
        context.registerService(this, serviceId);
    }

    @Override
    public String getUrl(final Node documentNode) {
        final String uuid = getUuidOfHandle(documentNode);

        if (uuid == null) {
            return null;
        }

        StringBuffer url = new StringBuffer();
        url.append(protocol).append("://");
        url.append(domain).append(':').append(port);
        url.append(path);
        url.append("?uuid=").append(uuid);
        url.append("&type=").append(type);

        return url.toString();
    }

    private String getUuidOfHandle(Node documentNode) {
        try {
            return documentNode.getParent().getIdentifier();
        } catch (RepositoryException e) {
            log.error("Error retrieving UUID from document handle", e);
        }
        return null;
    }

}
